New-VHD -ParentPath $VHDParentPath -Path $VHDPath -Differencing | Out-null
New-VM -Name "$Name" -MemoryStartupBytes $MemoryMBytesMB -Path $VMPath -VHDPath $VHDPath -SwitchName $SwitchName | Set-VM -ProcessorCount $ProcessorCount -Passthru | Start-VM -Passthru | ConvertTo-Json -Compress -Depth 7
