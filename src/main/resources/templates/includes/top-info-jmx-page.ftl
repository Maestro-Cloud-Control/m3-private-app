<tr valign="bottom" align="left">
    <td width="80%" style="padding:5px;">
        <a  href="/diagnostic/jmx-info" class="logo"></a><span class='logoText'><#include "../jmx/${page}/${page}-title.ftl"/></span>
        <div class="links_block">
            <#list links?keys?sort as link>
                <a href="${link}">${links[link]}</a>
            </#list>
        </div>
    </td>
</tr>
<tr><td height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff;"></td></tr>