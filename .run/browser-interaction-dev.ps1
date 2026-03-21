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
    throw "Timeout waiting for expression: $Expression"
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

try {
    $adminCtx = (Send-CdpCommand -Ws $ws -Method 'Target.createBrowserContext').result.browserContextId

    Write-Host '[1/4] Admin browser login'
    $adminPage = New-PageSession -Ws $ws -BrowserContextId $adminCtx -Url 'http://127.0.0.1:5175/login'
    Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '健身平台管理端' -TimeoutSec 20 | Out-Null
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '填充 root_admin' | Out-Null
    Start-Sleep -Milliseconds 250
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '登录' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/auth/login' -TimeoutSec 20 | Out-Null
    Wait-ForValue -Ws $ws -SessionId $adminPage.sessionId -Expression 'location.pathname' -Predicate { param($v) $v -eq '/permission-center' } -TimeoutSec 25 | Out-Null
    Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '权限中心' -TimeoutSec 25 | Out-Null
    Write-Host '  login + redirect: ok'

    Navigate-And-Wait -Ws $ws -SessionId $adminPage.sessionId -Url 'http://127.0.0.1:5175/videos' -ExpectedPath '/videos' -ExpectedText '视频'
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '刷新' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/video/list' -TimeoutSec 20 | Out-Null
    Write-Host '  videos refresh: ok'

    Navigate-And-Wait -Ws $ws -SessionId $adminPage.sessionId -Url 'http://127.0.0.1:5175/orders' -ExpectedPath '/orders' -ExpectedText '订单'
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/ops/order/list' -TimeoutSec 20 | Out-Null
    $orderRows = Eval-Cdp -Ws $ws -SessionId $adminPage.sessionId -Expression "document.querySelectorAll('.el-table__body-wrapper tbody tr').length"
    if ($orderRows -gt 0) {
        Click-FirstTableRow -Ws $ws -SessionId $adminPage.sessionId | Out-Null
        Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/ops/order/detail' -TimeoutSec 20 | Out-Null
        Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '订单详情' -TimeoutSec 20 | Out-Null
        Write-Host '  order detail: ok'
    } else {
        Write-Host '  order detail skipped: no rows'
    }

    Navigate-And-Wait -Ws $ws -SessionId $adminPage.sessionId -Url 'http://127.0.0.1:5175/operation-logs' -ExpectedPath '/operation-logs' -ExpectedText '操作日志'
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/operation-log/list' -TimeoutSec 20 | Out-Null
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '示例筛选' | Out-Null
    Click-ButtonByText -Ws $ws -SessionId $adminPage.sessionId -Text '查询' | Out-Null
    Wait-ForResource -Ws $ws -SessionId $adminPage.sessionId -Needle '/admin/operation-log/list?module=role_permission' -TimeoutSec 20 | Out-Null
    Wait-ForText -Ws $ws -SessionId $adminPage.sessionId -Needle '成功率' -TimeoutSec 20 | Out-Null
    Write-Host '  operation logs filter: ok'

    Write-Host 'BROWSER_INTERACTION_OK'
}
finally {
    try { if ($adminCtx) { Send-CdpCommand -Ws $ws -Method 'Target.disposeBrowserContext' -Params @{ browserContextId = $adminCtx } | Out-Null } } catch { }
    if ($ws -and $ws.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        try { $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, 'done', [Threading.CancellationToken]::None).Wait() } catch { }
    }
    if ($ws) { $ws.Dispose() }
}


