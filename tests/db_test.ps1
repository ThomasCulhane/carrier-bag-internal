$Base = if($env:BASE -ne $null) { $env:BASE } else { "http://localhost:8080/api/rows" }

Write-Host "Environment Variable: $env:BASE"
Write-Host "Running API tests against: $Base"

try {
  Invoke-RestMethod -Method Get -Uri $Base -ErrorAction Stop | Out-Null
  Write-Host "GET list OK"
} catch {
  Write-Error "GET list failed: $_"
  exit 2
}

# Create
$body = @{
  author = "test-suite"
  title = "created-by-tests"
  genre = "FICTION"
  submitDate = "2025-10-22"
  status = "UNASSIGNED"
  emailed = $false
} | ConvertTo-Json

try {
  $create = Invoke-RestMethod -Method Post -Uri $Base -Body $body -ContentType "application/json" -ErrorAction Stop
} catch {
  Write-Error "Create failed: $_"
  exit 3
}

if (-not $create.id) {
  Write-Error "Create response missing id: $create"
  exit 3
}
$id = $create.id
Write-Host "Created id=$id"

# Get single
try {
  Invoke-RestMethod -Method Get -Uri ("{0}/{1}" -f $Base, $id) -ErrorAction Stop | Out-Null
  Write-Host "GET single OK"
} catch {
  Write-Error "GET single failed: $_"
  exit 4
}

# Update
$putBody = @{ emailed = $true } | ConvertTo-Json
try {
  $putRes = Invoke-RestMethod -Method Put -Uri ("{0}/{1}" -f $Base, $id) -Body $putBody -ContentType "application/json" -ErrorAction Stop
  if ($putRes.updated -ne 1) {
    Write-Error "PUT failed: $putRes"
    exit 5
  }
  Write-Host "PUT OK"
} catch {
  Write-Error "PUT failed: $_"
  exit 5
}

# Delete
try {
  $del = Invoke-WebRequest -Method Delete -Uri ("{0}/{1}" -f $Base, $id) -ErrorAction Stop
  if ($del.StatusCode.Value__ -ne 204) {
    Write-Error "DELETE failed: $($del.StatusCode)"
    exit 6
  }
  Write-Host "DELETE OK"
} catch {
  if ($_.Exception.Response -and $_.Exception.Response.StatusCode.Value__ -eq 204) {
    Write-Host "DELETE OK (caught)"
  } else {
    Write-Error "DELETE failed: $_"
    exit 6
  }
}

# Verify deletion
try {
  Invoke-RestMethod -Method Get -Uri ("{0}/{1}" -f $Base, $id) -ErrorAction Stop
  Write-Error "GET after delete expected 404"
  exit 7
} catch {
  if ($_.Exception.Response -and $_.Exception.Response.StatusCode.Value__ -eq 404) {
    Write-Host "Verified deletion. All tests passed."
    exit 0
  } else {
    Write-Error "Unexpected error: $_"
    exit 8
  }
}