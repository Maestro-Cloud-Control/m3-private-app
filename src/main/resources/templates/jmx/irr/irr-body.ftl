<#assign headerStyleFirst = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign headerStyleOther = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign customCellStyle = "border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;">
<#assign groupHeaderStyle = "color:#315973; padding:20px 0 5px; border-bottom: 1px solid #E5EFF8;">
<#assign count = 0>
<#function cellStyle rowOdd colOdd>
    <#if rowOdd>
        <#return colOdd?string(colClear_rowOdd, colShade_rowOdd)/>
    <#else>
        <#return colOdd?string(colClear_rowEven, colShade_rowEven)/>
    </#if>
</#function>

<#if data.params??>
    <div class="tabwrap">
        <ul class="tabsContent">
            <#list data.params?keys?sort as cloud>
                <#assign count=0>
                <li class="tabs-control-item" ><a href="#${cloud}">${cloud}</a></li>
            </#list>
        </ul>
        <div class="holder-tabs">
            <#list data.params?keys?sort as cloud>
                <@cloudContent cloud data.params[cloud]/>
            </#list>
        </div>
    </div>
</#if>

<#macro cloudContent cloud mapContents>
    <#assign count=0>
    <div class="${cloud}">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <tr><td colspan="8" height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
        <#if mapContents?? && mapContents?size!=0>
            <#list mapContents?keys?sort as regionInTenant>
                <#assign contentByRegion=mapContents[regionInTenant]>
                <tr>
                    <td style="${bodyCell}${bodyCell_big} color:#315973; padding:20px 0 5px;" align="left" colspan="8">${regionInTenant}</td>
                </tr>
                <tr>
                    <td style="${titleCell}${titleClear}" align="left">Image</td>
                    <td style="${titleCell}${titleShade}" align="left">Total</td>
                    <td style="${titleCell}${titleClear}" align="left">${r"< 1m"}</td>
                    <td style="${titleCell}${titleShade}" align="left">${r"1-3m"}</td>
                    <td style="${titleCell}${titleClear}" align="left">${r"3-10m"}</td>
                    <td style="${titleCell}${titleShade}" align="left">${r"10-30m"}</td>
                    <td style="${titleCell}${titleClear}" align="left">${r"> 30m"}</td>
                    <td style="${titleCell}${titleShade}" align="left">Error</td>
                </tr>
                <#assign rowOdd = false>
                <#list contentByRegion?keys?sort as imageName>
                    <#assign contentByImage = contentByRegion[imageName]>
                    <tr valign="top">
                        <#assign colOdd = true>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-left: 1px solid #e5eff8;" align="left" >${imageName}</td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["total"] != 0>${contentByImage["total"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["lessOneMin"] != 0>${contentByImage["lessOneMin"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["oneToThree"] != 0>${contentByImage["oneToThree"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["threeToTen"] != 0>${contentByImage["threeToTen"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["tenToThirty"] != 0>${contentByImage["tenToThirty"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["moreThenThirty"] != 0>${contentByImage["moreThenThirty"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                        <td nowrap style="${bodyCell}${cellStyle(rowOdd, colOdd)} color: #315973; border-bottom: 1px solid #e5eff8; border-right: 1px solid #e5eff8;"  align="center" >
                            <#if contentByImage["error"] != 0>${contentByImage["error"]}<#else>--</#if>
                        </td>
                        <#assign colOdd = !colOdd>
                    </tr>
                    <#assign rowOdd = !rowOdd>
                </#list>
            </#list>
        </table>
        </#if>
    </div>
</#macro>
