<#macro showNumber number>
    <#if number??>
        ${format.reformatInteger(number)}
    <#else>
        --
    </#if>
</#macro>

<#if data.memoryInfoList??>
<#assign count = 0>
<table cellspacing="0" cellpadding="0" border="0" width="100%">
    <tr>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;" width="15%">Time</td>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="15%">Used Memory, KB</td>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="20%">Free Memory, KB</td>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="20%">Max Memory, KB</td>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="15%">Total Memory, KB</td>
        <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="15%">Memory Usage, %</td>
    </tr>
    <#list data.memoryInfoList as memoryInfo>
        <#if (memoryInfo.time)??>

            <@row memoryInfo/>

        </#if>
    </#list>
</table>

<#else>

    <#if data.errorMessage??>
        <tr><td height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
        <tr><td style="${bodyCell}" align="center"><div class="radar_nothing">Following error occurred: ${data.errorMessage}</div></td></tr>
    </#if>

</#if>

<#macro row memoryInfo>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
    
	<#assign usage = format.getInteger(memoryInfo.memoryUsage)>
            <#if usage??>
                <#if usage gte 70>
                    <#assign rowStyle = "color:#aa0000">
                <#elseif usage gte 50>
                    <#assign rowStyle = "color:#aa8800">
                <#else>
                    <#assign rowStyle = "color: #315973;">
                </#if>
            <#else>
                <#assign rowStyle = "color: #315973;">
            </#if>
	
	        <tr style="${rowStyle}">
                <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;"> ${(memoryInfo.time)!"--"} </td>
                <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" > <@showNumber memoryInfo.usedMemoryKb/> </td>
                <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;"> <@showNumber memoryInfo.freeMemoryKb/> </td>
                <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;"> <@showNumber memoryInfo.maxMemoryKb/> </td>
                <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; "> <@showNumber memoryInfo.totalMemoryKb/> </td>
                <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8;"> <@showNumber memoryInfo.memoryUsage/> </td>
            </tr>
	
    
    <#assign count=count+1>
    
</#macro>