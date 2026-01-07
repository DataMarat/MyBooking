$ErrorActionPreference = "Stop"

function Start-Service($name, $path, $port) {
  Write-Host "Starting $name ..."
  Start-Process -WindowStyle Normal -FilePath ".\mvnw.cmd" -ArgumentList "-q -pl $path spring-boot:run" -WorkingDirectory (Get-Location)
}

Start-Service "Eureka"        "eureka-server" 8761
Start-Service "Hotel Service" "hotel-service" 8082
Start-Service "Booking"       "booking-service" 8083
Start-Service "Gateway"       "api-gateway"   8081

# Stop all: Get-Process java | Stop-Process