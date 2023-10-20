<tr><td  height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
<#assign h = format>
<#assign firstHeaderStyle = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign otherHeadersStyle = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;border-left: 1px solid #E5EFF8;\" align=\"left\" nowrap">

<#macro cellContent value="">
    <#if value?has_content>${value}<#else>--</#if>
</#macro>
<#if data.restartMessage??>
    <tr><td style="font:13px tahoma,sans-serif; width:100%; padding:10px 0;">${data.restartMessage}</td></tr>
</#if>

<#if data.operations?? || data.schedules??>
<#assign count = 0>
    <#if data.operations??>
    <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <#list data.operations?keys?sort as scope>
            <tr>
                <td style="${bodyCell}${bodyCell_big} color:#315973; padding:20px 0 5px;  border-bottom: 1px solid #E5EFF8;" align="left" colspan="2">${scope} operations:</td>
            </tr>
            <#list data.operations[scope] as operation>
               <@field operation.title data.restartLinks[operation]/>
            </#list>
        </#list>
    </table>
    </#if>

    <#if data.schedules??>
    <table cellspacing="0" cellpadding="0" border="0" width="100%">
		<#if data.scheduleStats??>
			<tr>
                <td style="${bodyCell}${bodyCell_big} color:#315973; padding:20px 0 5px;  border-bottom: 1px solid #E5EFF8;" align="left" colspan="3">
					Number of schedules in progress and queued: ${data.scheduleStats.count} - <a class="linkRestart" href="${data.scheduleStats.resetLink}">Reset</a>
				</td>

            </tr>
            <#if data.scheduleStats.actions??>
                <#list data.scheduleStats.actions as name>
                    <@action name/>
                </#list>
            </#if>
		</#if>
        <#list data.schedules?keys?sort as zoneName>
            <tr>
                <td style="${bodyCell}${bodyCell_big} color:#315973; padding:20px 0 5px;  border-bottom: 1px solid #E5EFF8;" align="left" colspan="3">${zoneName} schedules:</td>
            </tr>
            <tr>
                <td ${firstHeaderStyle}>Schedule name</td>
                <td ${otherHeadersStyle}>Next run time</td>
                <td ${otherHeadersStyle}>Restart</td>
                <td ${otherHeadersStyle}>Suspend/Resume</td>
            </tr>
            <#list data.schedules[zoneName] as schedule>
                <#assign linkIdentifier = "${schedule.scheduleInfo.scheduleName}-${schedule.scheduleInfo.zoneName}" >
                <#assign restartLink = data.restartLinks[linkIdentifier] >
                <#assign suspendLink = data.suspendLinks[linkIdentifier] >
                <#assign name = h.prettyEnumName(schedule.scheduleName) >
                <@scheduleField schedule restartLink suspendLink />
            </#list>
        </#list>
    </table>
    </#if>

<#else>
<table cellspacing="0" cellpadding="0" border="0" width="100%">
    <tr><td height="2" style="background:#315973;font:13px tahoma,sans-serif;color:#ffffff; width:100%;"></td></tr>
    <tr><td style="${bodyCell}" align="center"><div class="radar_nothing">There are no schedule operations available.</div></td></tr>
</table>
</#if>

<#macro field title data>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
	
	<tr>
		<td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" width="60%">${title}</td>
		<td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;" width="40%"><a class="linkRestart" href="${data}">Restart</a></td>
	</tr>
    
    <#assign count=count+1>
    
</#macro>

<#macro scheduleField schedule restartLink suspendLink>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>
    <tr>
        <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" width="35%"><@cellContent h.prettyEnumName(schedule.scheduleInfo.scheduleName) /></td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;" width="25%"><@cellContent h.prettyShortDateTime(h.toDate(schedule.executionInfo.nextRun)) /></td>
        <td style="${bodyCell}${isOdd?string(colClear_rowOdd1, colClear_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;" width="20%"><a class="linkRestart" href="${restartLink}">Restart</a></td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; border-right: 1px solid #E5EFF8; color:#315973;" width="20%"><a class="linkRestart" href="${suspendLink}"><#if schedule.suspended >Resume<#else>Suspend</#if></a></td>
    </tr>

    <#assign count=count+1>

</#macro>

<#macro action actionName>
    <#local isOdd=(count%2 != 0)>
    <#assign even>background:#F9FCFE;</#assign>
    <#assign odd>background:#F7FBFF;</#assign>
    <tr>
        <td style="${bodyCell}${isOdd?string(odd, even)} border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;">
            <@cellContent actionName/>
        </td>
    </tr>
    <#assign count=count+1>
</#macro>
