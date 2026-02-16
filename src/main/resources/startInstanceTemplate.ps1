Get-VM -Id "$Uuid" | Start-VM -Passthru | ConvertTo-Json -Compress -Depth 7
