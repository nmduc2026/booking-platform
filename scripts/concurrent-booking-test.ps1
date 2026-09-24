# Concurrent booking smoke test (PowerShell)
# Usage: .\scripts\concurrent-booking-test.ps1 -SlotId <uuid> -Token <jwt>

param(
    [Parameter(Mandatory = $true)][string]$SlotId,
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$BaseUrl = "http://localhost:8083",
    [int]$Concurrency = 20
)

$success = [System.Collections.Concurrent.ConcurrentBag[int]]::new()
$conflict = [System.Collections.Concurrent.ConcurrentBag[int]]::new()
$other = [System.Collections.Concurrent.ConcurrentBag[string]]::new()

$jobs = 1..$Concurrency | ForEach-Object {
    Start-Job -ScriptBlock {
        param($BaseUrl, $SlotId, $Token)
        try {
            $resp = Invoke-WebRequest -Method POST -Uri "$BaseUrl/bookings" `
                -Headers @{ Authorization = "Bearer $Token" } `
                -ContentType "application/json" `
                -Body (@{ slotId = $SlotId } | ConvertTo-Json) `
                -UseBasicParsing
            return "OK:$($resp.StatusCode)"
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 409) { return "CONFLICT" }
            return "ERR:$code"
        }
    } -ArgumentList $BaseUrl, $SlotId, $Token
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$ok = ($results | Where-Object { $_ -like "OK:*" }).Count
$cf = ($results | Where-Object { $_ -eq "CONFLICT" }).Count
$err = ($results | Where-Object { $_ -like "ERR:*" })

Write-Host "success=$ok conflict=$cf"
if ($err) { Write-Host "other=$($err -join ', ')" }

if ($ok -eq 1 -and $cf -eq ($Concurrency - 1)) {
    Write-Host "PASS: only one booking succeeded"
    exit 0
}

Write-Host "FAIL: expected 1 success and $($Concurrency - 1) conflicts"
exit 1
