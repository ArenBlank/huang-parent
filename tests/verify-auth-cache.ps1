param(
    [string]$AppBaseUrl1 = "http://localhost:8093",
    [string]$AppBaseUrl2 = "http://localhost:8094",
    [string]$AdminBaseUrl = "http://localhost:8092",
    [string]$MemberAccount = "root_member",
    [string]$MemberPassword = "root",
    [string]$AdminAccount = "root_admin",
    [string]$AdminPassword = "root",
    [long]$MemberUserId = 101,
    [string]$TempPassword = "root-temp-123",
    [switch]$SkipCleanup
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body,
        [hashtable]$Headers
    )

    $invokeParams = @{
        Method      = $Method
        Uri         = $Url
        ContentType = "application/json"
        Headers     = @{}
    }
    if ($Headers) {
        $invokeParams.Headers = $Headers
    }
    if ($null -ne $Body) {
        $invokeParams.Body = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 }
    }
    return Invoke-RestMethod @invokeParams
}

function Assert-BizCode {
    param(
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][int]$ExpectedCode,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if ($null -eq $Response) {
        throw "$Label returned null response"
    }
    if ($Response.code -ne $ExpectedCode) {
        $message = if ($Response.message) { $Response.message } else { "<empty>" }
        throw "$Label expected biz code $ExpectedCode but got $($Response.code), message=$message"
    }
    Write-Host ("[PASS] {0} -> code={1}, message={2}" -f $Label, $Response.code, $Response.message) -ForegroundColor Green
}

function Login-App {
    param(
        [string]$BaseUrl,
        [string]$Account,
        [string]$Password
    )

    $response = Invoke-JsonApi -Method "POST" -Url "$BaseUrl/app/auth/login" -Body @{
        account   = $Account
        password  = $Password
        loginType = "password"
    }
    Assert-BizCode -Response $response -ExpectedCode 200 -Label "app login ($BaseUrl)"
    return $response.data.accessToken
}

function Login-Admin {
    param(
        [string]$BaseUrl,
        [string]$Account,
        [string]$Password
    )

    $response = Invoke-JsonApi -Method "POST" -Url "$BaseUrl/admin/auth/login" -Body @{
        account  = $Account
        password = $Password
    }
    Assert-BizCode -Response $response -ExpectedCode 200 -Label "admin login"
    return $response.data.accessToken
}

function Get-AppProfile {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$Label
    )

    $response = Invoke-JsonApi -Method "GET" -Url "$BaseUrl/app/profile/info" -Headers @{
        Authorization = "Bearer $Token"
    }
    if ($Label) {
        Write-Host ("[INFO] {0} -> code={1}, message={2}" -f $Label, $response.code, $response.message)
    }
    return $response
}

function Update-AppPassword {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$OldPassword,
        [string]$NewPassword,
        [string]$Label
    )

    $response = Invoke-JsonApi -Method "PUT" -Url "$BaseUrl/app/profile/password" -Headers @{
        Authorization = "Bearer $Token"
    } -Body @{
        oldPassword     = $OldPassword
        newPassword     = $NewPassword
        confirmPassword = $NewPassword
    }
    Assert-BizCode -Response $response -ExpectedCode 200 -Label $Label
}

function Update-UserStatus {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [long]$UserId,
        [int]$Status,
        [string]$Remark,
        [string]$Label
    )

    $response = Invoke-JsonApi -Method "PUT" -Url "$BaseUrl/admin/user/status" -Headers @{
        Authorization = "Bearer $Token"
    } -Body @{
        userId = $UserId
        status = $Status
        remark = $Remark
    }
    Assert-BizCode -Response $response -ExpectedCode 200 -Label $Label
}

$adminToken = $null
$currentPassword = $MemberPassword
$memberDisabled = $false
$result = [ordered]@{
    memberAccount              = $MemberAccount
    memberUserId               = $MemberUserId
    appBaseUrl1                = $AppBaseUrl1
    appBaseUrl2                = $AppBaseUrl2
    adminBaseUrl               = $AdminBaseUrl
    oldTokenFailsAfterPassword = $false
    oldTokenFailsAfterDisable  = $false
    restoredPassword           = $false
    restoredStatus             = $false
}

try {
    Write-Step "Login app member and verify protected endpoint on both app instances"
    $oldToken = Login-App -BaseUrl $AppBaseUrl1 -Account $MemberAccount -Password $currentPassword
    $profile1 = Get-AppProfile -BaseUrl $AppBaseUrl1 -Token $oldToken -Label "profile with old token on app1"
    Assert-BizCode -Response $profile1 -ExpectedCode 200 -Label "profile with old token on app1"
    $profile2 = Get-AppProfile -BaseUrl $AppBaseUrl2 -Token $oldToken -Label "profile with old token on app2"
    Assert-BizCode -Response $profile2 -ExpectedCode 200 -Label "profile with old token on app2"

    Write-Step "Update password to trigger tokenVersion invalidation and app auth cache eviction"
    Update-AppPassword -BaseUrl $AppBaseUrl1 -Token $oldToken -OldPassword $currentPassword -NewPassword $TempPassword -Label "update app password"
    $currentPassword = $TempPassword

    Write-Step "Reuse old token on both app instances, expect token invalid (602)"
    $staleAfterPassword1 = Get-AppProfile -BaseUrl $AppBaseUrl1 -Token $oldToken -Label "stale token after password change on app1"
    Assert-BizCode -Response $staleAfterPassword1 -ExpectedCode 602 -Label "stale token after password change on app1"
    $staleAfterPassword2 = Get-AppProfile -BaseUrl $AppBaseUrl2 -Token $oldToken -Label "stale token after password change on app2"
    Assert-BizCode -Response $staleAfterPassword2 -ExpectedCode 602 -Label "stale token after password change on app2"
    $result.oldTokenFailsAfterPassword = $true

    Write-Step "Login with new password, then verify protected endpoint on both instances again"
    $freshToken = Login-App -BaseUrl $AppBaseUrl2 -Account $MemberAccount -Password $currentPassword
    $freshProfile1 = Get-AppProfile -BaseUrl $AppBaseUrl1 -Token $freshToken -Label "fresh token on app1"
    Assert-BizCode -Response $freshProfile1 -ExpectedCode 200 -Label "fresh token on app1"
    $freshProfile2 = Get-AppProfile -BaseUrl $AppBaseUrl2 -Token $freshToken -Label "fresh token on app2"
    Assert-BizCode -Response $freshProfile2 -ExpectedCode 200 -Label "fresh token on app2"

    Write-Step "Login admin and disable the member to trigger AFTER_COMMIT driven admin-side eviction"
    $adminToken = Login-Admin -BaseUrl $AdminBaseUrl -Account $AdminAccount -Password $AdminPassword
    Update-UserStatus -BaseUrl $AdminBaseUrl -Token $adminToken -UserId $MemberUserId -Status 0 -Remark "verify-auth-cache disable" -Label "disable app member"
    $memberDisabled = $true

    Write-Step "Reuse current member token on both app instances, expect app login auth failure (501)"
    $disabled1 = Get-AppProfile -BaseUrl $AppBaseUrl1 -Token $freshToken -Label "disabled member token on app1"
    Assert-BizCode -Response $disabled1 -ExpectedCode 501 -Label "disabled member token on app1"
    $disabled2 = Get-AppProfile -BaseUrl $AppBaseUrl2 -Token $freshToken -Label "disabled member token on app2"
    Assert-BizCode -Response $disabled2 -ExpectedCode 501 -Label "disabled member token on app2"
    $result.oldTokenFailsAfterDisable = $true

    Write-Step "Phase verification completed, entering cleanup"
}
finally {
    if (-not $SkipCleanup) {
        Write-Step "Cleanup: restore user status and original password"
        try {
            if ($adminToken -and $memberDisabled) {
                Update-UserStatus -BaseUrl $AdminBaseUrl -Token $adminToken -UserId $MemberUserId -Status 1 -Remark "verify-auth-cache restore" -Label "restore member status"
                $memberDisabled = $false
                $result.restoredStatus = $true
            }
        } catch {
            Write-Warning "Failed to restore member status: $($_.Exception.Message)"
        }

        try {
            if ($currentPassword -ne $MemberPassword) {
                $restoreToken = Login-App -BaseUrl $AppBaseUrl1 -Account $MemberAccount -Password $currentPassword
                Update-AppPassword -BaseUrl $AppBaseUrl1 -Token $restoreToken -OldPassword $currentPassword -NewPassword $MemberPassword -Label "restore member password"
                $currentPassword = $MemberPassword
                $result.restoredPassword = $true
            }
        } catch {
            Write-Warning "Failed to restore member password: $($_.Exception.Message)"
        }
    }

    Write-Host ""
    Write-Host "Verification summary:" -ForegroundColor Yellow
    $result.GetEnumerator() | ForEach-Object {
        Write-Host ("- {0}: {1}" -f $_.Key, $_.Value)
    }
}
