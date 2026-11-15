# ============================================================================
#  AI-TransWatch - kiem thu chap nhan end-to-end (MySQL that + FastAPI AI)
#
#  Cach chay:
#    1. Nap .env vao phien PowerShell (xem docs/HUONG_DAN_CHAY.md muc 4).
#    2. AI service dang chay o http://127.0.0.1:8000, backend o 8080.
#    3. .\scripts\verify-e2e.ps1
#
#  Script chi DOC du lieu seed demo va TAO du lieu kiem thu moi voi tien to E2E-
#  nen chay lai nhieu lan van an toan (co hau to thoi gian).
#  Ghi chu: script co tinh chi dung ky tu ASCII de tranh loi encoding tren
#  Windows PowerShell 5.1. Thong bao tieng Viet co dau nam trong API/UI.
# ============================================================================
[CmdletBinding()]
param(
    [string]$ApiBase = 'http://127.0.0.1:8080/api'
)

$ErrorActionPreference = 'Stop'
$script:Pass = 0
$script:Fail = 0

function Write-Utf8Json([string]$Path, $Body) {
    $json = $Body | ConvertTo-Json -Depth 6 -Compress
    return Invoke-RestMethod -Method Post -Uri "$ApiBase$Path" -Headers $script:AuthHeaders `
        -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}

# Backend dung hai kieu trang: Spring Page (content/totalElements) cho xe, tai xe,
# chuyen, canh bao; va CustomerPageResponse (items/totalItems) cho khach hang.
function Get-PageItems($page) {
    if ($null -ne $page.items) { return $page.items }
    if ($null -ne $page.content) { return $page.content }
    return @()
}

function Get-PageTotal($page) {
    if ($null -ne $page.totalItems) { return $page.totalItems }
    if ($null -ne $page.totalElements) { return $page.totalElements }
    return (Get-PageItems $page).Count
}

function Check([string]$Name, [scriptblock]$Probe) {
    try {
        $result = & $Probe
        if ($result) { $script:Pass++; Write-Host "  [PASS] $Name" -ForegroundColor Green }
        else { $script:Fail++; Write-Host "  [FAIL] $Name" -ForegroundColor Red }
    } catch {
        $script:Fail++; Write-Host "  [FAIL] $Name -> $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Expect-HttpStatus([string]$Name, [int]$Expected, [scriptblock]$Call) {
    $actual = 0
    try { & $Call | Out-Null; $actual = 200 } catch {
        $response = $_.Exception.Response
        $actual = if ($response) { [int]$response.StatusCode } else { -1 }
    }
    if ($actual -eq $Expected) { $script:Pass++; Write-Host "  [PASS] $Name ($actual)" -ForegroundColor Green }
    else { $script:Fail++; Write-Host "  [FAIL] $Name (expected $Expected, got $actual)" -ForegroundColor Red }
}

Write-Host ""
Write-Host "=== AI-TransWatch E2E === $ApiBase" -ForegroundColor Cyan

# ---------------------------------------------------------------- 1. Suc khoe
Write-Host ''
Write-Host '1. Service health'
Check 'Backend /api/health = UP' {
    (Invoke-RestMethod "$ApiBase/health").data.status -eq 'UP'
}
Check 'FastAPI /health = UP' {
    (Invoke-RestMethod 'http://127.0.0.1:8000/health').status -eq 'UP'
}

# ------------------------------------------------------- 2. Xac thuc & phan quyen
Write-Host ''
Write-Host '2. Authentication and authorization'
$login = Invoke-RestMethod -Method Post -Uri "$ApiBase/auth/login" -ContentType 'application/json' `
    -Body (@{ username = 'admin'; password = 'password' } | ConvertTo-Json)
Check 'Admin login returns JWT' { $login.success -and $login.data.accessToken.Length -gt 20 }
$script:AuthHeaders = @{ Authorization = "Bearer $($login.data.accessToken)" }

Check 'GET /auth/me returns ADMIN role' {
    (Invoke-RestMethod "$ApiBase/auth/me" -Headers $script:AuthHeaders).data.roles -contains 'ADMIN'
}

Expect-HttpStatus 'Wrong password returns 401' 401 {
    Invoke-RestMethod -Method Post -Uri "$ApiBase/auth/login" -ContentType 'application/json' `
        -Body (@{ username = 'admin'; password = 'wrong-password' } | ConvertTo-Json)
}
Expect-HttpStatus 'Missing token returns 401' 401 { Invoke-RestMethod "$ApiBase/vehicles" }
Expect-HttpStatus 'Dispatcher cannot call admin-only simulator (403)' 403 {
    $dispatcher = Invoke-RestMethod -Method Post -Uri "$ApiBase/auth/login" -ContentType 'application/json' `
        -Body (@{ username = 'dispatcher'; password = 'password' } | ConvertTo-Json)
    $headers = @{ Authorization = "Bearer $($dispatcher.data.accessToken)" }
    $seededTrip = (Invoke-RestMethod "$ApiBase/trips?page=0&size=1" -Headers $headers).data.content[0].id
    Invoke-RestMethod -Method Post -Uri "$ApiBase/demo/trips/$seededTrip/simulate-step" -Headers $headers `
        -ContentType 'application/json' -Body (@{ scenario = 'NORMAL' } | ConvertTo-Json)
}

# ------------------------------------------------------------ 3. Du lieu nen
Write-Host ''
Write-Host '3. Reference data and orders (V4 seed on MySQL)'
$seededVehicles = (Invoke-RestMethod "$ApiBase/vehicles?page=0&size=100" -Headers $script:AuthHeaders).data
$seededDrivers = (Invoke-RestMethod "$ApiBase/drivers?page=0&size=100" -Headers $script:AuthHeaders).data
$seededCustomers = (Invoke-RestMethod "$ApiBase/customers?page=0&size=100" -Headers $script:AuthHeaders).data
$seededTrips = (Invoke-RestMethod "$ApiBase/trips?page=0&size=100" -Headers $script:AuthHeaders).data
$customerItems = Get-PageItems $seededCustomers
$tripItems = Get-PageItems $seededTrips
Check 'Vehicles readable from MySQL' { (Get-PageTotal $seededVehicles) -ge 2 }
Check 'Drivers readable from MySQL' { (Get-PageTotal $seededDrivers) -ge 1 }
Check 'Seeded customers readable' { $customerItems.Count -ge 2 }
Check 'Seeded trip TRIP-DEMO-001 exists' { ($tripItems | Where-Object tripCode -eq 'TRIP-DEMO-001') -ne $null }

$report = (Invoke-RestMethod "$ApiBase/reports/overview" -Headers $script:AuthHeaders).data
Check 'Report overview returns aggregate totals' { $report.totalTrips -ge 1 -and $report.totalOrders -ge 1 }
Check 'Report overview returns 7-day series' { $report.tripsPerDay.Count -eq 7 -and $report.alertsPerDay.Count -eq 7 }
Check 'Report overview returns valid rates' {
    $report.onTimeRate -ge 0 -and $report.onTimeRate -le 1 -and $report.alertHandlingRate -ge 0 -and $report.alertHandlingRate -le 1
}

Expect-HttpStatus 'Negative weight rejected with 400' 400 {
    Write-Utf8Json '/orders' @{
        orderCode = "E2E-BAD-$(Get-Date -Format HHmmss)"; customerId = $customerItems[0].id
        cargoName = 'Invalid cargo'; weightKg = -5; pickupAddress = 'A'; deliveryAddress = 'B'
    }
}

# ------------------------------------------------- 4. Tao tai nguyen & dieu phoi
Write-Host ''
Write-Host '4. Business flow: create vehicle, driver, order, trip'
$stamp = Get-Date -Format 'HHmmss'
$vehicle = Write-Utf8Json '/vehicles' @{
    licensePlate = "E2E-$stamp"; vehicleType = 'TRUCK'; brand = 'Test'; model = 'E2E'
    manufactureYear = 2024; maxPayloadKg = 5000; currentOdometerKm = 1000; fuelLevelPercent = 80
    status = 'AVAILABLE'; registrationExpiryDate = '2030-01-01'; insuranceExpiryDate = '2030-01-01'
    maintenanceIntervalKm = 5000; nextMaintenanceOdometerKm = 6000
}
Check 'Create vehicle AVAILABLE' { $vehicle.data.status -eq 'AVAILABLE' }

$driver = Write-Utf8Json '/drivers' @{
    employeeCode = "E2E-DRV-$stamp"; licenseNumber = "E2E-LIC-$stamp"; licenseClass = 'C'
    licenseExpiryDate = '2030-12-31'; status = 'AVAILABLE'; safetyScore = 95; totalDistanceKm = 0
}
Check 'Create driver AVAILABLE' { $driver.data.status -eq 'AVAILABLE' }

$order = Write-Utf8Json '/orders' @{
    orderCode = "E2E-ORD-$stamp"; customerId = $customerItems[0].id; cargoName = 'Test cargo'
    weightKg = 1200; pickupAddress = 'Warehouse A'; pickupLatitude = 10.84; pickupLongitude = 106.80
    deliveryAddress = 'Warehouse B'; deliveryLatitude = 10.90; deliveryLongitude = 106.77
    deliveryDeadline = (Get-Date).AddDays(2).ToString('yyyy-MM-ddTHH:mm:ss')
}
Check 'Create order PENDING' { $order.data.status -eq 'PENDING' }

$trip = Write-Utf8Json '/trips' @{
    tripCode = "E2E-TRIP-$stamp"; vehicleId = $vehicle.data.id; driverId = $driver.data.id
    orderIds = @($order.data.id); scheduledStartAt = (Get-Date).AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ss')
    estimatedArrivalAt = (Get-Date).AddHours(3).ToString('yyyy-MM-ddTHH:mm:ss')
}
Check 'Create trip SCHEDULED' { $trip.data.status -eq 'SCHEDULED' }
Check 'Vehicle becomes ON_TRIP' {
    (Invoke-RestMethod "$ApiBase/vehicles/$($vehicle.data.id)" -Headers $script:AuthHeaders).data.status -eq 'ON_TRIP'
}
Check 'Order becomes ASSIGNED' {
    (Invoke-RestMethod "$ApiBase/orders/$($order.data.id)" -Headers $script:AuthHeaders).data.status -eq 'ASSIGNED'
}

Expect-HttpStatus 'Re-dispatching busy vehicle rejected (409)' 409 {
    Write-Utf8Json '/trips' @{
        tripCode = "E2E-CONFLICT-$stamp"; vehicleId = $vehicle.data.id; driverId = $driver.data.id
        orderIds = @($order.data.id); scheduledStartAt = (Get-Date).AddMinutes(10).ToString('yyyy-MM-ddTHH:mm:ss')
    }
}

# ------------------------------------------------------ 5. GPS va canh bao
Write-Host ''
Write-Host '5. GPS telemetry, rule engine and AI analysis'
Check 'Move to PICKUP_IN_PROGRESS' {
    (Invoke-RestMethod -Method Patch -Uri "$ApiBase/trips/$($trip.data.id)/status" -Headers $script:AuthHeaders `
        -ContentType 'application/json' -Body (@{ status = 'PICKUP_IN_PROGRESS' } | ConvertTo-Json)).data.status -eq 'PICKUP_IN_PROGRESS'
}
Check 'Move to IN_TRANSIT' {
    (Invoke-RestMethod -Method Patch -Uri "$ApiBase/trips/$($trip.data.id)/status" -Headers $script:AuthHeaders `
        -ContentType 'application/json' -Body (@{ status = 'IN_TRANSIT' } | ConvertTo-Json)).data.status -eq 'IN_TRANSIT'
}

$gps = Write-Utf8Json "/trips/$($trip.data.id)/gps" @{
    latitude = 10.85; longitude = 106.79; speedKmh = 95; headingDegree = 90; engineOn = $true; stoppedMinutes = 0
}
Check 'Ingest GPS at 95 km/h' { $gps.data.id -ne $null }
Check 'Latest GPS matches ingested speed' {
    (Invoke-RestMethod "$ApiBase/trips/$($trip.data.id)/gps/latest" -Headers $script:AuthHeaders).data.speedKmh -eq 95
}
$alerts = (Invoke-RestMethod "$ApiBase/alerts?tripId=$($trip.data.id)&page=0&size=20" -Headers $script:AuthHeaders).data
$alertItems = Get-PageItems $alerts
Check 'SPEEDING/HIGH alert created' {
    ($alertItems | Where-Object { $_.alertType -eq 'SPEEDING' -and $_.severity -eq 'HIGH' }) -ne $null
}
Check 'Alert evidence contains telemetry JSON' {
    ($alertItems | Where-Object alertType -eq 'SPEEDING').evidence -match 'speedKmh'
}

$alertId = ($alertItems | Where-Object alertType -eq 'SPEEDING').id
Check 'Alert status -> IN_PROGRESS' {
    (Invoke-RestMethod -Method Patch -Uri "$ApiBase/alerts/$alertId/status" -Headers $script:AuthHeaders `
        -ContentType 'application/json' -Body (@{ status = 'IN_PROGRESS'; handlingNote = 'Contacted the driver' } | ConvertTo-Json)).data.status -eq 'IN_PROGRESS'
}
Check 'Alert status -> RESOLVED' {
    (Invoke-RestMethod -Method Patch -Uri "$ApiBase/alerts/$alertId/status" -Headers $script:AuthHeaders `
        -ContentType 'application/json' -Body (@{ status = 'RESOLVED'; handlingNote = 'Handled' } | ConvertTo-Json)).data.status -eq 'RESOLVED'
}

$analysis = Write-Utf8Json "/trips/$($trip.data.id)/analysis" @{
    routeDeviationMeters = 450; stoppedMinutes = 20; remainingDistanceKm = 35
}
Check 'AI returns positive ETA' { $analysis.data.etaMinutes -gt 0 }
Check 'AI returns lateProbability within [0,1]' {
    $analysis.data.lateProbability -ge 0 -and $analysis.data.lateProbability -le 1
}
Check 'AI returns valid riskLevel' { $analysis.data.riskLevel -in @('LOW', 'MEDIUM', 'HIGH') }
Check 'AI returns Vietnamese explanations' { $analysis.data.explanations.Count -ge 1 }
Check 'AI uses trained model (not baseline)' { $analysis.data.usingBaseline -eq $false }
Write-Host ("     -> ETA {0} min | late {1:P0} | risk {2} | model {3}" -f `
    $analysis.data.etaMinutes, $analysis.data.lateProbability, $analysis.data.riskLevel, $analysis.data.modelVersion) -ForegroundColor DarkGray

# --------------------------------------------------------- 6. Ket thuc chuyen
Write-Host ''
Write-Host '6. Complete trip and release resources'
Check 'Move to COMPLETED' {
    (Invoke-RestMethod -Method Patch -Uri "$ApiBase/trips/$($trip.data.id)/status" -Headers $script:AuthHeaders `
        -ContentType 'application/json' -Body (@{ status = 'COMPLETED' } | ConvertTo-Json)).data.status -eq 'COMPLETED'
}
Check 'Order becomes DELIVERED' {
    (Invoke-RestMethod "$ApiBase/orders/$($order.data.id)" -Headers $script:AuthHeaders).data.status -eq 'DELIVERED'
}
Check 'Vehicle back to AVAILABLE' {
    (Invoke-RestMethod "$ApiBase/vehicles/$($vehicle.data.id)" -Headers $script:AuthHeaders).data.status -eq 'AVAILABLE'
}
Check 'Driver back to AVAILABLE' {
    (Invoke-RestMethod "$ApiBase/drivers/$($driver.data.id)" -Headers $script:AuthHeaders).data.status -eq 'AVAILABLE'
}

# ------------------------------------- 7. Rule engine mo rong + canh bao dinh ky
Write-Host ''
Write-Host '7. Extended alert rules and periodic checks'
$demoTrip = ($tripItems | Where-Object tripCode -eq 'TRIP-DEMO-001').id

Check 'Simulator ROUTE_DEVIATION creates ROUTE_DEVIATION alert' {
    Write-Utf8Json "/demo/trips/$demoTrip/simulate-step" @{ scenario = 'ROUTE_DEVIATION' } | Out-Null
    $tripAlerts = (Invoke-RestMethod "$ApiBase/alerts?tripId=$demoTrip&page=0&size=50" -Headers $script:AuthHeaders).data
    ((Get-PageItems $tripAlerts) | Where-Object alertType -eq 'ROUTE_DEVIATION') -ne $null
}
Check 'Simulator CONTINUOUS_DRIVING creates CONTINUOUS_DRIVING alert' {
    Write-Utf8Json "/demo/trips/$demoTrip/simulate-step" @{ scenario = 'CONTINUOUS_DRIVING' } | Out-Null
    $tripAlerts = (Invoke-RestMethod "$ApiBase/alerts?tripId=$demoTrip&page=0&size=50" -Headers $script:AuthHeaders).data
    ((Get-PageItems $tripAlerts) | Where-Object alertType -eq 'CONTINUOUS_DRIVING') -ne $null
}

# Xe sap het dang kiem (10 ngay) va da vuot moc bao duong -> 2 canh bao dinh ky
$expiringVehicle = Write-Utf8Json '/vehicles' @{
    licensePlate = "E2E-DOC-$stamp"; vehicleType = 'TRUCK'; maxPayloadKg = 5000
    currentOdometerKm = 20000; status = 'AVAILABLE'
    registrationExpiryDate = (Get-Date).AddDays(10).ToString('yyyy-MM-dd')
    insuranceExpiryDate = (Get-Date).AddDays(400).ToString('yyyy-MM-dd')
    nextMaintenanceOdometerKm = 19000
}
Check 'Periodic checks create DOCUMENT_EXPIRING and MAINTENANCE_DUE' {
    Write-Utf8Json '/demo/trips/alerts/run-checks' @{} | Out-Null
    $all = (Invoke-RestMethod "$ApiBase/alerts?page=0&size=100" -Headers $script:AuthHeaders).data
    $mine = (Get-PageItems $all) | Where-Object { $_.vehicleId -eq $expiringVehicle.data.id }
    ($mine | Where-Object alertType -eq 'DOCUMENT_EXPIRING') -ne $null -and ($mine | Where-Object alertType -eq 'MAINTENANCE_DUE') -ne $null
}
Check 'Duplicate suppression: second run creates no new alert' {
    $second = (Write-Utf8Json '/demo/trips/alerts/run-checks' @{}).data.createdAlerts
    $second -eq 0
}

# Chuyen khong nhan duoc telemetry trong 40 phut -> GPS_SIGNAL_LOST
$signalVehicle = Write-Utf8Json '/vehicles' @{
    licensePlate = "E2E-SIG-$stamp"; vehicleType = 'TRUCK'; maxPayloadKg = 5000; currentOdometerKm = 1000; status = 'AVAILABLE'
}
$signalDriver = Write-Utf8Json '/drivers' @{
    employeeCode = "E2E-SIGD-$stamp"; licenseNumber = "E2E-SIGL-$stamp"; licenseClass = 'C'
    licenseExpiryDate = '2030-12-31'; status = 'AVAILABLE'; safetyScore = 95; totalDistanceKm = 0
}
$signalOrder = Write-Utf8Json '/orders' @{
    orderCode = "E2E-SIGO-$stamp"; customerId = $customerItems[0].id; cargoName = 'Signal test'
    weightKg = 100; pickupAddress = 'A'; deliveryAddress = 'B'
}
$signalTrip = Write-Utf8Json '/trips' @{
    tripCode = "E2E-SIGT-$stamp"; vehicleId = $signalVehicle.data.id; driverId = $signalDriver.data.id
    orderIds = @($signalOrder.data.id); scheduledStartAt = (Get-Date).AddMinutes(2).ToString('yyyy-MM-ddTHH:mm:ss')
}
Invoke-RestMethod -Method Patch -Uri "$ApiBase/trips/$($signalTrip.data.id)/status" -Headers $script:AuthHeaders `
    -ContentType 'application/json' -Body (@{ status = 'PICKUP_IN_PROGRESS' } | ConvertTo-Json) | Out-Null
Write-Utf8Json "/trips/$($signalTrip.data.id)/gps" @{
    latitude = 10.85; longitude = 106.79; speedKmh = 40; engineOn = $true
    recordedAt = (Get-Date).AddMinutes(-40).ToString('yyyy-MM-ddTHH:mm:ss')
} | Out-Null
Check 'Silent trip produces GPS_SIGNAL_LOST alert' {
    Write-Utf8Json '/demo/trips/alerts/run-checks' @{} | Out-Null
    $tripAlerts = (Invoke-RestMethod "$ApiBase/alerts?tripId=$($signalTrip.data.id)&page=0&size=20" -Headers $script:AuthHeaders).data
    ((Get-PageItems $tripAlerts) | Where-Object alertType -eq 'GPS_SIGNAL_LOST') -ne $null
}
Invoke-RestMethod -Method Patch -Uri "$ApiBase/trips/$($signalTrip.data.id)/status" -Headers $script:AuthHeaders `
    -ContentType 'application/json' -Body (@{ status = 'CANCELLED' } | ConvertTo-Json) | Out-Null

# ------------------------------------------- 8. Kenh canh bao realtime (WebSocket/STOMP)
Write-Host ''
Write-Host '8. Realtime alert channel (WebSocket/STOMP)'
Check 'WebSocket subscriber receives alert pushed by the rule engine' {
    $realtimeScript = Join-Path $PSScriptRoot 'verify-realtime.mjs'
    $output = & node $realtimeScript 2>&1
    $output | Where-Object { $_ -match '\[(PASS|FAIL|INFO)\]' } | ForEach-Object { Write-Host "     $_" -ForegroundColor DarkGray }
    $LASTEXITCODE -eq 0
}

# ------------------------------------------------------------------ Tong ket
Write-Host ''
Write-Host "=== Result: $script:Pass PASS, $script:Fail FAIL ===" -ForegroundColor Cyan
if ($script:Fail -gt 0) { exit 1 }
exit 0
