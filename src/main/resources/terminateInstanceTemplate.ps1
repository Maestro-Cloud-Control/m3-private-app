Get-VM -Id "$Uuid" | Remove-VM -Force -Passthru | ConvertTo-Json -Compress -Depth 7
