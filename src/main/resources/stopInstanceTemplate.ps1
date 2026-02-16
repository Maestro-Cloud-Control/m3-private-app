Get-VM -Id "$Uuid" | Stop-VM -Force -Passthru | ConvertTo-Json -Compress -Depth 7
