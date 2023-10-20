<#assign headerStyleFirst = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign headerStyleOther = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;\" align=\"left\" nowrap">

<#assign customCellStyle = "border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;">
<#assign groupHeaderStyle = "color:#315973; padding:20px 0 5px; border-bottom: 1px solid #E5EFF8;">
<#assign count = 0>

<#if data??>
    <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <#if data.jobs?? && data.jobs?size!=0>
            <tr>
                <td style="${bodyCell}${bodyCell_big} ${groupHeaderStyle}" align="left" colspan="2">Jobs in processing
                    state:
                </td>
            </tr>

            <tr>
                <td ${headerStyleFirst}>Title</td>
                <td ${headerStyleOther}>State</td>
                <td ${headerStyleOther}>Last execution</td>
                <td ${headerStyleOther}>Next execution</td>
            </tr>

            <#list data.jobs as job>
                <@tableContent job/>
            </#list>
        <#else>
            <tr>
                <td style="${bodyCell}${bodyCell_big} ${groupHeaderStyle}" align="left" colspan="2">There are no jobs in processing state found.</td>
            </tr>
        </#if>
    </table>
</#if>

<#macro tableContent job>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>

    <tr>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if job.jobName??>${job.jobName}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if job.state??>${job.state}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            <#if job.lastExecutionDate??>${job.lastExecutionDate}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            <#if job.nextExecutionDate??>${job.nextExecutionDate}<#else>n/a</#if>
        </td>
    </tr>

    <#assign count=count+1>
</#macro>
