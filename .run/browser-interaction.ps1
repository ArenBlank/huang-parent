$ErrorActionPreference = 'Stop'

function New-CdpClient {
    param([string]$WebSocketUrl)
    $ws = New-Object System.Net.WebSockets.ClientWebSocket
    $ws.ConnectAsync([Uri]$WebSocketUrl, [Threading.CancellationToken]::None).Wait()
    return $ws
}

function Send-CdpCommand {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $Method,
        [hashtable] $Params = @{},
        [string] $SessionId = $null
    )

    $script:NextId++
    $payload = [ordered]@{ id = $script:NextId; method = $Method }
    if ($Params -and $Params.Count -gt 0) { $payload.params = $Params }
    if ($SessionId) { $payload.sessionId = $SessionId }

    $json = $payload | ConvertTo-Json -Compress -Depth 20
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $sendSegment = [ArraySegment[byte]]::new($bytes)
    $Ws.SendAsync($sendSegment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, [Threading.CancellationToken]::None).Wait()

    while ($true) {
        $buffer = New-Object byte[] 65536
        $ms = New-Object System.IO.MemoryStream
        do {
            $recvSegment = [ArraySegment[byte]]::new($buffer)
            $result = $Ws.ReceiveAsync($recvSegment, [Threading.CancellationToken]::None).Result
            if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
                throw 'CDP websocket closed'
            }
            $ms.Write($buffer, 0, $result.Count)
        } while (-not $result.EndOfMessage)

        $text = [Text.Encoding]::UTF8.GetString($ms.ToArray())
        $ms.Dispose()
        $msg = $text | ConvertFrom-Json
        if ($msg.id -eq $script:NextId) { return $msg }
    }
}

function Eval-Cdp {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Expression
    )

    $resp = Send-CdpCommand -Ws $Ws -Method 'Runtime.evaluate' -Params @{ expression = $Expression; returnByValue = $true; awaitPromise = $true } -SessionId $SessionId
    if ($resp.error) { throw $resp.error.message }
    return $resp.result.result.value
}

function Wait-ForValue {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Expression,
        [Parameter(Mandatory = $true)] [scriptblock] $Predicate,
        [int] $TimeoutSec = 25
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $value = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression $Expression
            if (& $Predicate $value) { return $value }
        } catch {
        }
        Start-Sleep -Milliseconds 250
    }
    $href = ''
    $preview = ''
    try { $href = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression 'location.href' } catch { $href = 'n/a' }
    try { $preview = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression "document.body ? (document.body.innerText || '').slice(0, 300) : '<no-body>'" } catch { $preview = 'n/a' }
    throw "Timeout waiting for expression: $Expression | href=$href | preview=$preview"
}

function New-PageSession {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [string] $BrowserContextId = $null,
        [string] $Url = 'about:blank'
    )

    $params = @{ url = $Url }
    if ($BrowserContextId) { $params.browserContextId = $BrowserContextId }
    $target = Send-CdpCommand -Ws $Ws -Method 'Target.createTarget' -Params $params
    $attach = Send-CdpCommand -Ws $Ws -Method 'Target.attachToTarget' -Params @{ targetId = $target.result.targetId; flatten = $true }
    $sessionId = $attach.result.sessionId
    Send-CdpCommand -Ws $Ws -Method 'Page.enable' -SessionId $sessionId | Out-Null
    Send-CdpCommand -Ws $Ws -Method 'Runtime.enable' -SessionId $sessionId | Out-Null
    return @{ sessionId = $sessionId; targetId = $target.result.targetId }
}

function Click-ButtonByText {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Text
    )

    $safeText = $Text.Replace("'", "\\'")
    $expr = "(() => { const el = Array.from(document.querySelectorAll('button')).find(b => ((b.innerText || b.textContent || '').trim() === '$safeText') || ((b.innerText || b.textContent || '').includes('$safeText'))); if (!el) return 'missing'; el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window })); return 'clicked'; })()"
    $result = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression $expr
    if ($result -eq 'missing') { throw "Button not found: $Text" }
    return $result
}

function Click-FirstTableRow {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId
    )

    $expr = "(() => { const row = document.querySelector('.el-table__body-wrapper tbody tr'); if (!row) return 'missing'; row.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window })); return row.innerText || 'clicked'; })()"
    $result = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression $expr
    if ($result -eq 'missing') { throw 'No table row found to click' }
    return $result
}

function Wait-ForResource {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Needle,
        [int] $TimeoutSec = 20
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $expr = "performance.getEntriesByType('resource').map(r => r.name).join('\n')"
            $resources = Eval-Cdp -Ws $Ws -SessionId $SessionId -Expression $expr
            if ($resources -like "*${Needle}*") { return $resources }
        } catch {
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Timeout waiting for resource: $Needle"
}

function Wait-ForText {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Needle,
        [int] $TimeoutSec = 25
    )

    Wait-ForValue -Ws $Ws -SessionId $SessionId -Expression "document.body ? document.body.innerText : ''" -Predicate { param($v) $v -like "*${Needle}*" } -TimeoutSec $TimeoutSec
}

function Navigate-And-Wait {
    param(
        [Parameter(Mandatory = $true)] [System.Net.WebSockets.ClientWebSocket] $Ws,
        [Parameter(Mandatory = $true)] [string] $SessionId,
        [Parameter(Mandatory = $true)] [string] $Url,
        [Parameter(Mandatory = $true)] [string] $ExpectedPath,
        [Parameter(Mandatory = $true)] [string] $ExpectedText
    )

    Send-CdpCommand -Ws $Ws -Method 'Page.navigate' -Params @{ url = $Url } -SessionId $SessionId | Out-Null
    Wait-ForValue -Ws $Ws -SessionId $SessionId -Expression 'location.pathname' -Predicate { param($v) $v -eq $ExpectedPath } -TimeoutSec 30 | Out-Null
    Wait-ForText -Ws $Ws -SessionId $SessionId -Needle $ExpectedText -TimeoutSec 30 | Out-Null
}

$browserInfo = Invoke-RestMethod http://127.0.0.1:9222/json/version
$ws = New-CdpClient -WebSocketUrl $browserInfo.webSocketDebuggerUrl
$script:NextId = 0

$adminCtx = $null
$appCtx = $null

try {
    $adminCtx = (Send-CdpCommand -Ws $ws -Method 'Target.createBrowserContext').result.browserContextId
    $appCtx = (Send-CdpCommand -Ws $ws -Method 'Target.createBrowserContext').result.browserContextId

    Write-Host '[1/6] Admin browser login'
    $adminPage = New-PageSession -Ws $ws -BrowserContextId $adminCtx -Url 'http://localhost:5173/login'
    Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '健身平台管理端' -TimeoutSec 20 | Out-Null
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '填充 root_admin' | Out-Null
    Start-Sleep -Milliseconds 250
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '登录' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/auth/login' -TimeoutSec 20 | Out-Null
    Wait-ForValue -Ws $ws -SessionId $adminPage.sessionId -Expression 'location.pathname' -Predicate { param($v) $v -eq '/permission-center' } -TimeoutSec 25 | Out-Null
    Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '权限中心' -TimeoutSec 25 | Out-Null
    Write-Host '  login + redirect: ok'

    Navigate-And-Wait -Ws $ws -SessionId $adminPage.sessionId -Url 'http://localhost:5173/videos' -ExpectedPath '/videos' -ExpectedText '视频素材'
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '刷新' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/video/list' -TimeoutSec 20 | Out-Null
    Write-Host '  videos refresh: ok'

    Write-Host '[2/6] App browser login'
    $appPage = New-PageSession -Ws $ws -BrowserContextId $appCtx -Url 'http://localhost:5174/login'
    Wait-ForText -Ws $ws -SessionId $appPage.sessionId -Needle '健身平台 App' -TimeoutSec 20 | Out-Null
    Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '填充 root' | Out-Null
    Start-Sleep -Milliseconds 250
    Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '登录' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $appPage.sessionId -Needle '/app/auth/login' -TimeoutSec 20 | Out-Null
    Wait-ForValue -Ws $ws -SessionId $appPage.sessionId -Expression 'location.pathname' -Predicate { param($v) $v -eq '/home' } -TimeoutSec 25 | Out-Null
    Wait-ForText -Ws $ws -SessionId $appPage.sessionId -Needle '系统配置' -TimeoutSec 25 | Out-Null
    Write-Host '  login + redirect: ok'

    Navigate-And-Wait -Ws $ws -SessionId $appPage.sessionId -Url 'http://localhost:5174/home' -ExpectedPath '/home' -ExpectedText '系统配置'
    $configSelector = 'input[placeholder="site_name,default_avatar"]'
    $exprSetConfig = "(() => { const el = document.querySelector('$configSelector'); if (!el) return 'missing'; const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(el, 'site_name,default_avatar'); el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); return el.value; })()"
    $configResult = Eval-Cdp -Ws $ws -SessionId $appPage.sessionId -Expression $exprSetConfig
    if ($configResult -eq 'missing') { throw 'Config input not found' }
    Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '读取配置' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $appPage.sessionId -Needle '/app/system-config/map' -TimeoutSec 20 | Out-Null
    Wait-ForText -Ws $ws -SessionId $appPage.sessionId -Needle 'site_name' -TimeoutSec 20 | Out-Null
    Write-Host '  home config read: ok'

    Navigate-And-Wait -Ws $ws -SessionId $appPage.sessionId -Url 'http://localhost:5174/booking' -ExpectedPath '/booking' -ExpectedText '刷新'
    Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '刷新' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $appPage.sessionId -Needle '/app/booking/schedule/list' -TimeoutSec 20 | Out-Null

    $hasScheduleRow = Eval-Cdp -Ws $ws -SessionId $appPage.sessionId -Expression @"
(() => {
  const cards = Array.from(document.querySelectorAll('.card'))
  const scheduleCard = cards.find(c => (c.innerText || c.textContent || '').includes('教练预约'))
  if (!scheduleCard) return false
  return !!scheduleCard.querySelector('.el-table__body-wrapper tbody tr')
})()
"@
    if ($hasScheduleRow) {
        Click-FirstTableRow -Ws $ws -SessionId $appPage.sessionId | Out-Null
        Wait-ForText -Ws $ws -SessionId $appPage.sessionId -Needle '档期ID' -TimeoutSec 15 | Out-Null
        Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '提交预约' | Out-Null
        Wait-ForResource -Ws $ws -SessionId $appPage.sessionId -Needle '/app/booking/create' -TimeoutSec 20 | Out-Null
        Wait-ForText -Ws $ws -SessionId $appPage.sessionId -Needle '最近预约' -TimeoutSec 20 | Out-Null
        Write-Host '  booking create: ok'

        $payButtonExists = Eval-Cdp -Ws $ws -SessionId $appPage.sessionId -Expression "Array.from(document.querySelectorAll('button')).some(b => (b.innerText || b.textContent || '').includes('模拟支付'))"
        if ($payButtonExists) {
            Click-ButtonByText -Ws $ws -SessionId $appPage.sessionId -Text '模拟支付' | Out-Null
            Wait-ForResource -Ws $ws -SessionId $appPage.sessionId -Needle '/app/pay/mock-notify' -TimeoutSec 20 | Out-Null
            Write-Host '  booking pay: ok'
        }
    } else {
        Write-Host '  booking create skipped: no schedule rows'
    }

    Write-Host 'BROWSER_INTERACTION_OK'
}
finally {
    try { if ($adminCtx) { Send-CdpCommand -Ws $ws -Method 'Target.disposeBrowserContext' -Params @{ browserContextId = $adminCtx } | Out-Null } } catch { }
    try { if ($appCtx) { Send-CdpCommand -Ws $ws -Method 'Target.disposeBrowserContext' -Params @{ browserContextId = $appCtx } | Out-Null } } catch { }
    if ($ws -and $ws.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        try { $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, 'done', [Threading.CancellationToken]::None).Wait() } catch { }
    }
    if ($ws) { $ws.Dispose() }
}
