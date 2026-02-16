Get-VM -Id "$Uuid" | Restart-VM -Force -Passthru | ConvertTo-Json -Compress -Depth 7
