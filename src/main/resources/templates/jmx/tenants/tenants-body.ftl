<#assign headerStyleFirst = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign headerStyleOther = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;\" align=\"left\" nowrap">

<#assign customCellStyle = "border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;">
<#assign groupHeaderStyle = "color:#315973; padding:20px 0 5px; border-bottom: 1px solid #E5EFF8;">
<#assign count = 0>
<#if data.params??>
    <#if data.tenant??>
        Healthcheck request for ${data.tenant} in ${data.region} was performed
    </#if>

    <div class="tabwrap">

        <ul class="tabsContent">
            <#list data.params?keys?sort as cloud>
                <#assign count=0>
                <li class="tabs-control-item" ><a href="#${cloud}">${cloud}</a></li>
            </#list>
        </ul>
        <div class="holder-tabs">
            <#list data.params?keys?sort as cloud>
                <@table cloud data.params[cloud]/>
            </#list>
        </div>
    </div>
</#if>

<#macro table cloud listTenants>
    <#assign count=0>
    <div class="${cloud}">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
            <tr>
               <td ${headerStyleFirst}>Tenant alias</td>
               <td ${headerStyleOther}>Region alias</td>
               <#if cloud == "VMWARE">
                    <td ${headerStyleOther}>Organization name</td>
               </#if>
               <td ${headerStyleOther}>Is managment available</td>
               <#if cloud == "VMWARE">
                    <td ${headerStyleOther}>Shapes</td>
               </#if>
               <#if cloud == "OPEN_STACK">
                    <td ${headerStyleOther}>Native name</td>
               </#if>
               <#if cloud == "OPEN_STACK">
                    <td ${headerStyleOther}>Security group name</td>
               </#if>
               <#if cloud == "OPEN_STACK">
                    <td ${headerStyleOther}>Userdata template id</td>
               </#if>
               <td ${headerStyleOther}>State</td>
               <td ${headerStyleOther}>Last healthcheck</td>
                <#if cloud == "VMWARE">
               <td ${headerStyleOther}>Check</td>
               </#if>
             </tr>

            <#list listTenants as tenant>
               <@tableContent cloud tenant/>
            </#list>
        </table>
    </div>
</#macro>

<#macro tableContent cloud tenant>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>

    <tr>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if tenant.tenantAlias??>${tenant.tenantAlias}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if tenant.regionAlias??>${tenant.regionAlias}<#else>n/a</#if>
        </td>
        <#if cloud == "VMWARE">
            <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
                <#if tenant.name??>${tenant.name}<#else>n/a</#if>
            </td>
        </#if>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            ${tenant.managementAvailable?string('yes', 'no')}
        </td>
        <#if cloud == "VMWARE">
            <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
                <#if tenant.shapes??>${tenant.shapes}<#else>n/a</#if>
            </td>
        </#if>
        <#if cloud == "OPEN_STACK">
            <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
                <#if tenant.nativeName??>${tenant.nativeName}<#else>n/a</#if>
            </td>
        </#if>
        <#if cloud == "OPEN_STACK">
            <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
                <#if tenant.securityGroupName??>${tenant.securityGroupName}<#else>n/a</#if>
            </td>
        </#if>
        <#if cloud == "OPEN_STACK">
            <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
                <#if tenant.userdataTemplateId??>${tenant.userdataTemplateId}<#else>n/a</#if>
            </td>
        </#if>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            <#if tenant.state??>${tenant.state}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            <#if tenant.lastStatusUpdate??>${tenant.lastStatusUpdate}<#else>n/a</#if>
        </td>
         <#if cloud == "VMWARE">
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}">
            <a href="/diagnostic/tenants?tenant=${tenant.tenantAlias}&region=${tenant.regionAlias}&cloud=${cloud}" class="linkRestart" style="color: green;">Perform healthcheck</a>
        </td>
        </#if>
    </tr>

    <#assign count=count+1>
</#macro>
