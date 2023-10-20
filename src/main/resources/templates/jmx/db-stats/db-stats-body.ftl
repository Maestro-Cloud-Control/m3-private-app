<#if data??>
<tr><td><a class="linkRestart" href="?clear=true">Clear statistics</a></td></tr>
<#if message??>
<tr><td style="font:13px tahoma,sans-serif; width:100%; padding:10px 0;">${message}</td></tr>
</#if>
    <#assign count = 0>
    <tr>
        <td>
            <table cellspacing="0" cellpadding="0" border="0" width="100%">
                <tr>
                    <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;" align="center" width="10%">Collection</td>
                    <td style="${titleCell}${titleShade} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;border-left: 1px solid #E5EFF8;" align="center" nowrap width="10%">Write operations performed</td>
                    <td style="${titleCell}${titleShade} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;border-left: 1px solid #E5EFF8;" align="center" nowrap width="10%">Operations replicated</td>
                </tr>
                <#list data as rowData>
                    <@statsRow rowData.collectionName rowData.writeOperations rowData.replicatedOperations />
                </#list>
            </table>
        </td>
    </tr>
</#if>

<#macro statsRow collection firstValue secondValue>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
    <tr>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" align="center" width="10%">${collection}</td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973; border-right: 1px solid #E5EFF8;" align="center" nowrap width="10%">${format.prettyNumber(firstValue)}</td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973; border-right: 1px solid #E5EFF8;" align="center" nowrap width="10%">${format.prettyNumber(secondValue)}</td>
    </tr>
    <#assign count=count+1>
</#macro>