
<#include "includes/doctype-healthCheck.ftl" />

<tr>
	<td>
			<table width="100%" cellspacing="0" cellpadding="5" border="0">
					<#include "includes/top-info.ftl" />
			</table>
	</td>
</tr>

<#include "includes/header-spacer.ftl" />
<#assign headerStyleFirst = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8;\" align=\"left\" nowrap">
<#assign headerStyleOther = "style=\"${titleCell}${titleClear} background:#80B0CD; color:#fff; border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8;\" align=\"left\" nowrap">

<#assign customCellStyle = "border-bottom: 1px solid #E5EFF8; border-left: 1px solid #E5EFF8; color:#315973;">
<#assign groupHeaderStyle = "color:#315973; padding:20px 0 5px; border-bottom: 1px solid #E5EFF8;">
<#assign outdated = false>
<#if data.report??><assign outdated = data.report.outdated></#if>
<tr <#if outdated>class="error"</#if>>
	<td style="border-bottom:1px solid #315973;">
		<div class="holder-text">
			<p>Maestro Private Agent is a system, that can manage different distributed regions. Sometimes, the connection with one or several of the regions is unstable or broken due to technical issues.  In this case, the agent will not be able to manage private cloud properly. </p>
			<p>This page gives real-time detailed information on each of the activated amqp-regions performance.</p>
		</div>
		<div class="holder-text-phone">
			<p>Maestro Private Agent is a system, that can manage different distributed regions. Sometimes, the connection with one or several of the regions is unstable or broken due to technical issues.  In this case, the agent will not be able to manage private cloud properly. </p>
		</div>
<#if data.report??>
<#assign report = data.report>
        <#if report.orchestratorStates??>
            <div id="tabwrap" class="healthcheckTab">
                <ul class="tabsContent">
                    <#list report.orchestratorStates as oState>
                        <#assign count=0>
                        <li class="tabs-control-item" ><a href="#${oState.title}">${oState.title}</a></li>
                    </#list>
                </ul>
                <div class="holder-tabs">
                    <#assign count=0>
                    <#list report.orchestratorStates as oState>
                    <div id="${oState.title}">
                        <table cellspacing="0" cellpadding="5" border="0" width="100%">
                            <#if count == 0>
                            <tr>
                                <@field "Date" formatTime(oState.date) oState.outdated/>
                                <@field "Ip" oState.ip oState.outdated/>

                                <td colspan="2" height:2px; style="border-top:1px solid #315973;padding:0;"></td>
                                <#list oState.mongoDBStates as mongo>
                                    <@field "MongoDB, Host" mongo.host/>
                                    <#if mongo.username??>
                                        <@field "MongoDB, Username" mongo.username/>
                                    </#if>
                                    <@field "MongoDB, Port" mongo.port/>
                                    <@field "MongoDB, Database" mongo.database/>
                                    <@field "MongoDB, Latency" mongo.latency/>
                                    <#if mongo.additionalParams??>
                                        <#list mongo.additionalParams?keys as prop>
                                            <#assign propertyName= "MongoDB, " + prop>
                                            <@field propertyName mongo.additionalParams[prop]/>
                                        </#list>
                                    </#if>
                                    <td colspan="2" height:2px; style="border-top:1px solid #315973;padding:0;"></td>
                                </#list>

                            </tr>
                            <#else>
                                <#if oState.httpRegionStatistic??>
                                    <#assign http = oState.httpRegionStatistic/>
                                    <@field "Http requests, Total" http.totalRequests/>
                                    <@field "Http requests, Per last 5 min" http.requestsPer5Min/>
                                    <td colspan="2" height:2px; style="border-top:1px solid #315973;padding:0;"></td>
                                    <#if http.extendedHttpStatistic??>
                                        <#assign extended = http.extendedHttpStatistic/>
                                        <@field "Http requests, Statistic time (min)" extended.statisticTime/>
                                        <@field "Http requests, Total error rate" extended.errorRate + " %"/>
                                        <@field "Http requests, Max request time" extended.maxRequestTime + " sec"/>
                                        <@field "Http requests, Min request time" extended.minRequestTime + " sec"/>
                                        <td colspan="2" height:2px; style="border-top:1px solid #315973;padding:0;"></td>
                                    </#if>
                                 </#if>
                                <#if oState.amqpRegionStatistic??>
                                    <#assign rabbit = oState.amqpRegionStatistic/>
                                    <@field "RabbitMQ, Host" rabbit.host rabbit.outdated/>
                                    <@field "RabbitMQ, Vhost" rabbit.vhost rabbit.outdated/>
                                    <@field "RabbitMQ, Username" rabbit.username rabbit.outdated/>
                                    <@field "RabbitMQ, Port" rabbit.port rabbit.outdated/>
                                    <@field "RabbitMQ, Channels" rabbit.channels rabbit.outdated/>
                                    <@field "RabbitMQ, Connections" rabbit.connections rabbit.outdated/>
                                    <@field "RabbitMQ, Consumed messages (All)" rabbit.allConsumedMessages rabbit.outdated/>
                                    <@field "RabbitMQ, Consumed messages (per 5 min)" rabbit.consumedMessagesPerFiveMin rabbit.outdated/>
                                    <@field "RabbitMQ, Published messages (All)" rabbit.allPublishedMessages rabbit.outdated/>
                                    <@field "RabbitMQ, Published messages (per 5 min)" rabbit.publishedMessagesPerFiveMin rabbit.outdated/>
                                    <@field "RabbitMQ, Latency" rabbit.latency rabbit.outdated/>
                                    <td colspan="2" height:2px; style="border-top:1px solid #315973;padding:0;"></td>
                                 </#if>
                            </#if>
                        </table>
                        <#if count != 0 && oState.amqpRegionStatistic?? && rabbit.queueStats?size gt 0 >
                            <@table oState.title oState.amqpRegionStatistic.queueStats/>
                        </#if>
                    </div>
                    <#assign count=count+1>
                    </#list>
                    </div>
                </div>
            </div>
        </#if>
    </td>
</tr>

<form method="POST" action="/logout">
    <input class="logOutBtn" type="submit" value="LogOut"/>
</form>

<#else>
	<tr>
		<td>
			<table cellspacing="0" cellpadding="0" border="0" width="100%">
			<tr >
				<td style="${bodyCell} padding-top:20px; color:#315973" align="center">Report is not available yet, try again later.</td>
			</tr>
			</table>
		</td>
	</tr>
</#if>
<#include "includes/footer.ftl" />

<#function formatTime date>
    <#return date?string("EEEE, MMMM dd, yyyy, hh:mm:ss a '('zzz')'")/>
</#function>

<#macro field name value="" red=false>
        <#local isOdd=(count%2 != 0)>
        <tr <#if red>class="error"</#if>>
                <td style="${bodyCell}${isOdd?string(colClear_rowOdd, colClear_rowEven)}white-space: nowrap;" align="right"><b>${name}</b></td>
                <td style="${bodyCell}${isOdd?string(colShade_rowOdd, colShade_rowEven)}" align="left" width="100%">
                        <#if value?is_enumerable>
                                <#assign separator=""/>
                                <#list value as item>${separator}${item}<#assign separator=", "/></#list>
                        <#else>
                                ${value}
                        </#if>
                </td>
        </tr>
        <#assign count=count+1>
</#macro>

<#macro table region queuesStats>
    <#assign count=0>
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
            <tr>
               <td ${headerStyleFirst}>Queue name</td>
               <td ${headerStyleOther}>Last usage</td>
               <td ${headerStyleOther}>Message count</td>
               <td ${headerStyleOther}>Queue type</td>
             </tr>

            <#list queuesStats as queue>
               <@tableContent queue/>
            </#list>
        </table>
</#macro>

<#macro tableContent queue>
    <#local isOdd=(count%2 != 0)>

    <#assign colClear_rowEven1>background:#F9FCFE;</#assign>
    <#assign colShade_rowEven1>background:#FFFFFF;</#assign>
    <#assign colClear_rowOdd1>background:#F4F9FE;</#assign>
    <#assign colShade_rowOdd1>background:#F7FBFF;</#assign>

    <tr>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if queue.queueName??>${queue.queueName}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if queue.lastUsage??>${queue.lastUsage?number_to_date?string("EEEE, MMMM dd, yyyy, hh:mm:ss a '('zzz')'")}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if queue.count??>${queue.count}<#else>n/a</#if>
        </td>
        <td style="${bodyCell}${isOdd?string(colShade_rowOdd1, colShade_rowEven1)} ${customCellStyle}"/>
            <#if queue.type??>${queue.type}<#else>n/a</#if>
        </td>
    </tr>

    <#assign count=count+1>
</#macro>

<script type="text/javascript">
	var isMobile = false;
	<#if data.mobile>
	isMobile = true;
	</#if>
	
</script>
