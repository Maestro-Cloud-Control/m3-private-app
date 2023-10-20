/*
 * Copyright 2023 Maestro Cloud Control LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.maestro3.diagnostic.controller;

import io.maestro3.diagnostic.manager.IDiagnosticPageManager;
import io.maestro3.diagnostic.model.EnumJMXPage;
import io.maestro3.diagnostic.model.EnumResponseFormat;
import io.maestro3.diagnostic.model.container.JmxInfoDataContainer;
import io.maestro3.diagnostic.model.container.MemoryInfoDataContainer;
import io.maestro3.diagnostic.model.healthcheck.HealthCheckReport;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.impl.DefaultUrlBuilder;
import io.maestro3.diagnostic.util.HttpUtil;
import io.maestro3.diagnostic.view.JmxViewHelper;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.SdkCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



@RestController
@RequestMapping("/diagnostic")
public class DiagnosticController {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticController.class);

    private static final String CONTENT_NAME = "data";
    private static final String PARAM_LAST_TIME = "last";
    private static final String RESTART = "restart";

    private IDiagnosticPageManager manager;
    private DefaultUrlBuilder urlBuilder;
    private JmxViewHelper helper = new JmxViewHelper();
    private boolean isMultiJsFiles;
    private IHealthCheckService healthCheckService;
    private int extendedApiLogging;


    @Autowired
    public DiagnosticController(IDiagnosticPageManager manager,
                                DefaultUrlBuilder urlBuilder,
                                @Value("${ui.js.files.multi}") boolean isMultiJsFiles,
                                @Value("${extended.request.logging.minutes}") int extendedApiLogging,
                                IHealthCheckService healthCheckService) {
        this.manager = manager;
        this.urlBuilder = urlBuilder;
        this.extendedApiLogging = extendedApiLogging;
        this.isMultiJsFiles = isMultiJsFiles;
        this.healthCheckService = healthCheckService;
    }

    @GetMapping(EnumJMXPage.Paths.JMX_INFO)
    public ModelAndView getJmxInfo(HttpServletRequest request) throws IOException {
        ModelAndView model;
        JmxInfoDataContainer data = manager.getJmxInfo();
        EnumResponseFormat format = getPageResponseFormat(EnumJMXPage.JMX_INFO, request);
        switch (format) {
            case HTML:
                return buildHtmlModel(EnumJMXPage.JMX_INFO, data);
            case XML:
                return new ModelAndView("jmx-xml", CONTENT_NAME, data);
            case JSON:
                Map<String, Object> allData = new HashMap<>();
                allData.put("content", getContent(EnumJMXPage.JMX_INFO, data));
                allData.put("page", EnumJMXPage.JMX_INFO.getViewName());
                allData.put("links", createPageLinks());
                return new ModelAndView("jmx-json", CONTENT_NAME, allData);
            default:
                return null;
        }
    }

    @GetMapping(EnumJMXPage.Paths.MEMORY)
    public ModelAndView getJmxMemoryInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ModelAndView model;
        int lastTimeMin = 5; // !!! Default: Show LAST 5 minutes
        String lastTimeVal = request.getParameter(PARAM_LAST_TIME);
        if (StringUtils.isNotBlank(lastTimeVal)) {
            try {
                lastTimeMin = Integer.parseInt(lastTimeVal);
            } catch (NumberFormatException nfe) {
                logger.error(nfe.getMessage());
                lastTimeMin = 5;
            }
        }
        MemoryInfoDataContainer data = manager.getMemoryDetails(lastTimeMin);
        model = buildJmxPageModelAndView(EnumJMXPage.MEMORY, request, data);
        return model;
    }

    @GetMapping(EnumJMXPage.Paths.JOBS)
    public ModelAndView getJobs(HttpServletRequest request,
                                HttpServletResponse response) {
        ModelAndView mav;
        Map<String, Object> data = new HashMap<>();
        data.put("jobs", manager.getJobsModel());
        mav = buildJmxPageModelAndView(EnumJMXPage.JOBS, request, data);
        return mav;
    }

    @GetMapping(EnumJMXPage.Paths.INSTANCE_RUN_REPORT)
    public ModelAndView getIrr(HttpServletRequest request,
                                   HttpServletResponse response) {
        Map data = manager.getInstanceRunDetails();

        return buildJmxPageModelAndView(EnumJMXPage.INSTANCE_RUN_REPORT, request, data);
    }

    @GetMapping(EnumJMXPage.Paths.TENANTS)
    public ModelAndView getTenants(HttpServletRequest request,
                                   HttpServletResponse response) {
        Map data = manager.getTenantInfoModel();

        return buildJmxPageModelAndView(EnumJMXPage.TENANTS, request, data);
    }

    @GetMapping(value = EnumJMXPage.Paths.TENANTS,
        params = {EnumJMXPage.RequestParams.REGION,
            EnumJMXPage.RequestParams.TENANT,
            EnumJMXPage.RequestParams.CLOUD,})
    public ModelAndView initCheck(HttpServletRequest request,
                                  @RequestParam(EnumJMXPage.RequestParams.REGION) String regionAlias,
                                  @RequestParam(EnumJMXPage.RequestParams.CLOUD) String cloud,
                                  @RequestParam(EnumJMXPage.RequestParams.TENANT) String tenantAlias,
                                  HttpServletResponse response) {
        SdkCloud sdkCloud = SdkCloud.fromValue(cloud);
        Map data = manager.getTenantInfoModel();
        data.put("tenant", tenantAlias);
        data.put("region", regionAlias);
        manager.performHealthcheck(sdkCloud, tenantAlias, regionAlias);
        return buildJmxPageModelAndView(EnumJMXPage.TENANTS, request, data);
    }

    @GetMapping(EnumJMXPage.Paths.OPERATIONS)
    public ModelAndView getOperations(HttpServletRequest request) {
        ModelAndView model;
        Map<String, Object> content = manager.getOperationsModel();
        model = buildJmxPageModelAndView(EnumJMXPage.OPERATIONS, request, content);
        return model;
    }

    @GetMapping(value = EnumJMXPage.Paths.OPERATIONS, params = RESTART)
    public ModelAndView restartOperation(@RequestParam(RESTART) String scheduleOperation,
                                         HttpServletResponse response,
                                         HttpServletRequest request) {
        ModelAndView model;
        String result = manager.invokeOperation(scheduleOperation);
        Map<String, Object> content = manager.getOperationsModel();
        content.put("restartMessage", result);
        model = buildJmxPageModelAndView(EnumJMXPage.OPERATIONS, request, content);
        return model;
    }

    @GetMapping(EnumJMXPage.Paths.HEALTH_CHECK)
    public ModelAndView getHealthCheck(HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        ModelAndView model;
        Map<String, Object> data = new HashMap<>();
        data.put("report", healthCheckService.getReport());
        data.put("date", new Date());
        data.put("mobile", false);
        model = buildJmxPageModelAndView(EnumJMXPage.HEALTH_CHECK, request, data);
        return model;
    }


    @GetMapping(EnumJMXPage.Paths.EXTENDED_HTTP)
    public ModelAndView getExtendedApiInfo(HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        ModelAndView model;
        Map<String, Object> data = new HashMap<>();
        HealthCheckReport report = healthCheckService.getReport();
        data.put("report", report);
        data.put("date", new Date());
        data.put("mobile", false);
        model = buildJmxPageModelAndView(EnumJMXPage.EXTENDED_HTTP_METRICS, request, data);
        return model;
    }

    protected ModelAndView buildJmxPageModelAndView(EnumJMXPage page,
                                                    HttpServletRequest request,
                                                    Object data) {
        EnumResponseFormat format = getPageResponseFormat(page, request);
        switch (format) {
            case HTML:
                return buildHtmlModel(page, data);
            case XML:
                return new ModelAndView("jmx-xml", CONTENT_NAME, data);
            case JSON:
                Map<String, Object> allData = new HashMap<>();
//                if (data instanceof ExecutionResult) {
                data = getContent(page, data);
//                }
                allData.put("content", data);
                allData.put("page", page.getViewName());
                return new ModelAndView("jmx-json", CONTENT_NAME, allData);
            default:
                return null;
        }
    }

    protected EnumResponseFormat getPageResponseFormat(EnumJMXPage page,
                                                       HttpServletRequest request) {
        EnumResponseFormat defaultFormat = page.getDefaultFormat();
        List<EnumResponseFormat> supportedFormats = page.getSupportedFormats();
        EnumResponseFormat format = HttpUtil.getResponseFormat(request, defaultFormat);
        if (!supportedFormats.contains(format)) {
            format = defaultFormat;
        }
        return format;
    }

    protected ModelAndView buildHtmlModel(EnumJMXPage page,
                                          Object data) {
        ModelAndView modelAndView = new ModelAndView();

        modelAndView.setViewName(getViewName(page));
        Map<String, String> links = createPageLinks();
        modelAndView.addObject("page", page.getViewName());
        modelAndView.addObject("links", links);
        modelAndView.addObject("format", helper);
        modelAndView.addObject("multiJsFiles", isMultiJsFiles);

        Object content = getContent(page, data);
        if (content != null) {
            modelAndView.addObject(CONTENT_NAME, content);
        }

        return modelAndView;
    }

    private String getViewName(EnumJMXPage page) {
        if (page == EnumJMXPage.HEALTH_CHECK) {
            return "HealthCheck";
        }
        return "JmxPage";
    }

    protected Map<String, String> createPageLinks() {
//        boolean allowed = orchestrationSettingsService.isAllowJMXActionCommands();
        Map<String, String> result = new TreeMap<String, String>();
        for (EnumJMXPage page : EnumJMXPage.values()) {
            if (EnumJMXPage.EXTENDED_HTTP_METRICS == page && extendedApiLogging <= 0) {
                continue;
            }
            String url = urlBuilder.buildJmxPageUrl(page);
            result.put(url, page.getLabel());
        }
        return result;
    }

    protected Object getContent(EnumJMXPage page, Object data) {
        Object content = null;
        // this is not really good: here we un-parse whatever models we got in data, then drop them into model and view
        switch (page) {
            case JMX_INFO:
                if (data instanceof JmxInfoDataContainer) {
                    content = manager.getJmxInfoModel((JmxInfoDataContainer) data);
                }

                break;
            case MEMORY:
                if (data instanceof MemoryInfoDataContainer) {
                    content = manager.getMemoryModel((MemoryInfoDataContainer) data);
                }
                break;
            default:
                content = data;
                break;
        }
        return content;
    }

}
