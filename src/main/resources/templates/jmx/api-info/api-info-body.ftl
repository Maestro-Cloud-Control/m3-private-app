<#if data.report??>
<div class="tabwrap">
    <ul class="tabsContent">
        <#list data.report.orchestratorStates as group>
            <#if group.httpRegionStatistic?? && group.httpRegionStatistic.extendedHttpStatistic??>
                <li class="tabs-control-item" ><a href="#${group.title}">${group.title}</a></li>
            </#if>
        </#list>
    </ul>
    <div class="holder-tabs">
        <#list data.report.orchestratorStates as group>
            <#if group.httpRegionStatistic?? && group.httpRegionStatistic.extendedHttpStatistic??>
                <@table group/>
            </#if>
        </#list>
    </div>
</div>

<#else>
    <#if data.errorMessage??>
        <tr><td height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
        <tr><td style="${bodyCell}" align="center"><div class="radar_nothing">Following error occurred: ${data.errorMessage}</div></td></tr>
    </#if>
</#if>

<#macro table group>
    <#assign count=0>
    <div class="${group.title}">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
            <tr>
                <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;" width="60%">Request URL</td>
                <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="20%">Count</td>
                <td style="${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;" width="20%">Error rate</td>
            </tr>
            <#if group.httpRegionStatistic?? && group.httpRegionStatistic.extendedHttpStatistic??>
                <#list group.httpRegionStatistic.extendedHttpStatistic.totalRequestsByUrl?keys?sort as urlKey>
                    <@row group.httpRegionStatistic.extendedHttpStatistic.totalRequestsByUrl[urlKey]/>
                </#list>
            </#if>
        </table>
    </div>
</#macro>

<#macro row requestParams>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
    
    <tr>
        <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" width="40%">${requestParams.url}</td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;  word-break: break-all;" width="588px">${requestParams.requestsCount}</td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;  word-break: break-all;" width="588px">${requestParams.errorRate} %</td>
    </tr>
    
    <#assign count=count+1>
    
</#macro>
