<#include "includes/doctype-jmx-page.ftl" />

<tr>
    <td>
        <table width="100%" cellspacing="0" cellpadding="0" border="0">
            <#include "includes/top-info-jmx-page.ftl" />
        </table>
    </td>
</tr>
<tr>
    <td style="font:13px tahoma,sans-serif;color:#000;">
        <div class="holder-text">
            <#include "jmx/${page}/${page}-description.ftl" />
        </div>
    </td>
</tr>
<tr>
    <td>
        <#include "jmx/${page}/${page}-body.ftl" />
    </td>
</tr>

<form method="POST" action="/logout">
    <input class="logOutBtn" type="submit" value="LogOut"/>
</form>

<#include "includes/footer.ftl" />