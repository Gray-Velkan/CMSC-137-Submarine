# Quick test script for multiplayer
$javaPath = 'C:\Users\Lanot\.vscode\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64\bin\java.exe'
$gameDir = 'c:\Users\Lanot\Downloads\submarine survival\CMSC-137-Submarine'

Write-Host "Starting Host..." -ForegroundColor Green
Start-Process -FilePath $javaPath -ArgumentList "-cp", "bin", "edu.cmsc137.submarine.GameLauncher" -WorkingDirectory $gameDir -NoNewWindow

Start-Sleep -Seconds 3

Write-Host "Starting Client..." -ForegroundColor Yellow
Start-Process -FilePath $javaPath -ArgumentList "-cp", "bin", "edu.cmsc137.submarine.GameLauncher" -WorkingDirectory $gameDir -NoNewWindow

Write-Host "Both games started. Host first, then join with localhost." -ForegroundColor Cyan