<#if data.params??>
<div class="tabwrap">
    <ul class="tabsContent">
        <#list data.params?keys?sort as group>
            <#assign count=0>
            <li class="tabs-control-item" ><a href="#${group}">${group}</a></li>
        </#list>
    </ul>
    <div class="holder-tabs">
        <#list data.params?keys?sort as group>
            <@table group data.params[group]/>
        </#list>
    </div>
</div>

<#else>
    <#if data.errorMessage??>
        <tr><td height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
        <tr><td style="${bodyCell}" align="center"><div class="radar_nothing">Following error occurred: ${data.errorMessage}</div></td></tr>
    </#if>
</#if>

<#macro table group paramsInGroup>
    <#assign count=0>
    <div class="${group}">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
            <tr>
                <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;" width="40%">Name</td>
                <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="60%">Value</td>
            </tr>

            <#list paramsInGroup?keys?sort as paramName>
                <@row paramName paramsInGroup[paramName]/>
            </#list>
        </table>
    </div>
</#macro>

<#macro row paramName value>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
    
    <tr>
        <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" width="40%">${paramName}</td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;  word-break: break-all;" width="588px">${value}</td>
    </tr>
    
    <#assign count=count+1>
    
</#macro>