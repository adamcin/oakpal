powershell -NonInteractive - <<\EOF
refreshenv
# Round brackets in variable names cause problems with bash
Get-Location
Get-ChildItem env:*
Get-ChildItem env:* | %{
  if (!($_.Name.Contains('('))) {
    $value = $_.Value
    if ($_.Name -eq 'PATH') {
      $value = $value -replace ';',':'
    }
    Write-Output ("export " + $_.Name + "='" + $value + "'")
  }
} | Tee-Object | Out-File -Encoding ascii .\refreshenv.sh
Get-ChildItem .\refreshenv.sh
EOF
source "./refreshenv.sh"