<!doctype html>

<#include "ui-scheme-metro.ftl" />

<html>
<head>
    <title><#include "../jmx/${page}/${page}-title.ftl"/></title>
    <link type="text/css" rel="stylesheet" href="/css/fonts.css">
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
    <link href="/css/tabs.css" rel="stylesheet" type="text/css"/>
    <link href="/css/mobile-jmx.css" rel="stylesheet" type="text/css"/>
    <script type="text/javascript" src="/js/maestro-web.js"></script>

                      			<#-- <#if jmx/${multiJsFiles}> -->
                      		    <#--    <script type="text/javascript" src="js/goog/base.js"></script> -->
                                  <#--    <script type="text/javascript" src="js/goog/ui/deps.js"></script> -->
                                  <#--    <script type="text/javascript" src="js/maestro/init.js"></script> -->
                                  <#--    <script>radar();</script> -->
                      			<#-- <#else>-->
                      					<script type="text/javascript" src="/js/radar.js" ></script>
                      					<script>radar()</script>
                      			<#-- </#if> -->

    <link href="/img/favicon.ico" rel="shortcut icon">
</head>
<body id="reporting" ${bodyAttr}>
<div class="pageHolder">
    <div id="mainMenu">
        <div class="mainMenuInner">
            <div class="login">
               <#-- <div class="logged">
                    <span class="learn-more"><a href="/site" title="Learn more" target="_blank">Learn more</a></span>
                    <p>Welcome, <strong><@sec.authentication property="principal.user.fullName"/></strong></p>
                    <div>
                        <a title="Logout" class="logout" href="/maestro2/j_spring_security_logout">Logout</a>
                    </div>-->
                </div>
            </div>
        </div>
    </div>
    <div class="pageContent">
        <div class="table-holder">
            <table cellspacing="0" cellpadding="0" align="center" width="100%">
