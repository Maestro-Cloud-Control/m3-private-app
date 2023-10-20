<!doctype html>

<#include "ui-scheme-metro.ftl" />

<html>
<head>
	<title>Maestro Orchestrator: HealthCheck</title>
	<link type="text/css" rel="stylesheet" href="/css/fonts.css">
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
	<meta http-equiv="content-type" content="text/html; charset=utf-8">
	<meta http-equiv="refresh" content="60" />
	<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no" />


	<link href="/css/tabs.css" rel="stylesheet" type="text/css"/>

	<#if data.mobile>
		<link href="/cssmobile-metro.css" rel="stylesheet" type="text/css"/>
		<!--<script type="text/javascript" src="js/common.js"></script>-->
		<#else>
			<link href="/css/tabs.css" rel="stylesheet" type="text/css"/>
			<link href="/css/mobile-jmx.css" rel="stylesheet" type="text/css"/>
	</#if>
            <script type="text/javascript" src="/js/maestro-web.js"></script>
					<#--	<#if data.multiJsFiles> -->
                			<#--		<script type="text/javascript" src="js/goog/base.js"></script> -->
                             <#--       <script type="text/javascript" src="js/goog/ui/deps.js"></script> -->
                             <#--       <script type="text/javascript" src="js/maestro/init.js"></script> -->
                			<#--	<#else> -->


                			<#--	</#if> -->

	<link href="/img/favicon.ico" rel="shortcut icon">
</head>
<body id="reporting" ${bodyAttr} class="<#if data.mobile>isMobile <#else> ''</#if>" >
	<div class="pageHolder">
		<#if data.mobile>
			<div id="backBar" class="topBar toolbarShow">
				<span class="backButton" title="Back"><a class="back" href="/diagnostic/jmx-info">Back</a></span>
				<div class="inner"><h3>HealthCheck</h3></div>
			</div>
			<#else>

			
		</#if>
		<div class="pageContent">
			<div class="table-holder">
				<table cellspacing="0" cellpadding="0" align="center">
	<script type="text/javascript" src="/js/tabReporting.js"></script>
	<script>tabReporting()</script>