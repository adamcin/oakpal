powershell -NonInteractive - <<\EOF
Get-Location
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned
Import-Module "$env:ChocolateyInstall\helpers\chocolateyProfile.psm1"
Update-SessionEnvironment
# Round brackets in variable names cause problems with bash
Get-ChildItem env:* | %{
  if (!($_.Name.Contains('('))) {
    $value = $_.Value
    if ($_.Name -eq 'PATH') {
      $value = $value -replace ';',':'
    }
    Write-Output ("export " + $_.Name + "='" + $value + "'")
  }
} | Out-File -Encoding ascii .\refreshenv.sh
EOF
pwd
source "./refreshenv.sh"