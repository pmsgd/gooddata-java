/*
 * Copyright (C) 2004-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service;

import com.gooddata.sdk.service.account.AccountService;
import com.gooddata.sdk.service.auditevent.AuditEventService;
import com.gooddata.sdk.service.connector.ConnectorService;
import com.gooddata.sdk.service.dataload.OutputStageService;
import com.gooddata.sdk.service.dataload.processes.ProcessService;
import com.gooddata.sdk.service.dataset.DatasetService;
import com.gooddata.sdk.service.executeafm.ExecuteAfmService;
import com.gooddata.sdk.service.export.ExportService;
import com.gooddata.sdk.service.featureflag.FeatureFlagService;
import com.gooddata.sdk.service.gdc.DataStoreService;
import com.gooddata.sdk.service.gdc.GdcService;
import com.gooddata.sdk.service.httpcomponents.HttpClientRestOperationsProvider;
import com.gooddata.sdk.service.lcm.LcmService;
import com.gooddata.sdk.service.md.MetadataService;
import com.gooddata.sdk.service.md.maintenance.ExportImportService;
import com.gooddata.sdk.service.notification.NotificationService;
import com.gooddata.sdk.service.project.ProjectService;
import com.gooddata.sdk.service.project.model.ModelService;
import com.gooddata.sdk.service.projecttemplate.ProjectTemplateService;
import com.gooddata.sdk.service.warehouse.WarehouseService;
import org.apache.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.apache.http.util.VersionInfo.loadVersionInfo;

/**
 * Entry point for GoodData SDK usage.
 * <p>
 * Configure connection to GoodData using one of constructors. One can then get initialized service he needs from
 * the newly constructed instance. This instance can be also used later for logout from GoodData Platform.
 * <p>
 * Usage example:
 * <pre><code>
 *     GoodData gd = new GoodData("roman@gooddata.com", "Roman1");
 *     // do something useful like: gd.getSomeService().doSomething()
 *     gd.logout();
 * </code></pre>
 */
public class GoodData2 {

    private final RestTemplate restTemplate;
    private final AccountService accountService;
    private final ProjectService projectService;
    private final MetadataService metadataService;
    private final ModelService modelService;
    private final GdcService gdcService;
    private DataStoreService dataStoreService;
    private DatasetService datasetService;
    private final ConnectorService connectorService;
    private ProcessService processService;
    private final WarehouseService warehouseService;
    private final NotificationService notificationService;
    private final ExportImportService exportImportService;
    private final FeatureFlagService featureFlagService;
    private final OutputStageService outputStageService;
    private final ProjectTemplateService projectTemplateService;
    private final ExportService exportService;
    private final AuditEventService auditEventService;
    private final ExecuteAfmService executeAfmService;
    private final LcmService lcmService;

    GoodData2(RestOperationsProvider provider, GoodDataEndpoint endpoint, GoodDataSettings settings) {
        this.restTemplate = (RestTemplate) provider.createRestOperations(endpoint,settings, Collections.emptyMap()); //FIXME
        accountService = new AccountService(getRestTemplate(), settings);
        projectService = new ProjectService(getRestTemplate(), accountService, settings);
        metadataService = new MetadataService(getRestTemplate(), settings);
        modelService = new ModelService(getRestTemplate(), settings);
        gdcService = new GdcService(getRestTemplate(), settings);

        exportService = new ExportService(getRestTemplate(), endpoint, settings);

        warehouseService = new WarehouseService(getRestTemplate(), settings);
        connectorService = new ConnectorService(getRestTemplate(), projectService, settings);
        notificationService = new NotificationService(getRestTemplate(), settings);
        exportImportService = new ExportImportService(getRestTemplate(), settings);
        featureFlagService = new FeatureFlagService(getRestTemplate(), settings);
        outputStageService = new OutputStageService(getRestTemplate(), settings);
        projectTemplateService = new ProjectTemplateService(getRestTemplate(), settings);
        auditEventService = new AuditEventService(getRestTemplate(), accountService, settings);
        executeAfmService = new ExecuteAfmService(getRestTemplate(), settings);
        lcmService = new LcmService(getRestTemplate(), settings);

        if (provider instanceof HttpClientRestOperationsProvider) {
            final HttpClient httpClient = ((HttpClientRestOperationsProvider) provider).getHttpClient();
            dataStoreService = new DataStoreService(httpClient, getRestTemplate(), gdcService, endpoint.toUri());
            datasetService = new DatasetService(getRestTemplate(), dataStoreService, settings);
            processService = new ProcessService(getRestTemplate(), accountService, dataStoreService, settings);
        } else {
            // TODO log some warning
        }
    }
    /**
     * Get the configured {@link RestTemplate} used by the library.
     * This is the extension point for inheriting classes providing additional services.
     * @return REST template
     */
    protected final RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * Logout from GoodData Platform
     */
    public void logout() {
        getAccountService().logout();
    }

    /**
     * Get initialized service for project management (to list projects, create a project, ...)
     *
     * @return initialized service for project management
     */
    @Bean("goodDataProjectService")
    public ProjectService getProjectService() {
        return projectService;
    }

    /**
     * Get initialized service for account management (to get current account information, logout, ...)
     *
     * @return initialized service for account management
     */
    @Bean("goodDataAccountService")
    public AccountService getAccountService() {
        return accountService;
    }

    /**
     * Get initialized service for metadata management (to query, create and update project metadata like attributes,
     * facts, metrics, reports, ...)
     *
     * @return initialized service for metadata management
     */
    @Bean("goodDataMetadataService")
    public MetadataService getMetadataService() {
        return metadataService;
    }

    /**
     * Get initialized service for model management (to get model diff, update model, ...)
     *
     * @return initialized service for model management
     */
    @Bean("goodDataModelService")
    public ModelService getModelService() {
        return modelService;
    }

    /**
     * Get initialized service for API root management (to get API root links, ...)
     *
     * @return initialized service for API root management
     */
    @Bean("goodDataGdcService")
    public GdcService getGdcService() {
        return gdcService;
    }

    /**
     * Get initialized service for data store (user staging/WebDAV) management (to upload, download, delete, ...)
     *
     * @return initialized service for data store management
     */
    @Bean("goodDataDataStoreService")
    public DataStoreService getDataStoreService() {
        return dataStoreService;
    }

    /**
     * Get initialized service for dataset management (to list manifest, get datasets, load dataset, ...)
     *
     * @return initialized service for dataset management
     */
    @Bean("goodDataDatasetService")
    public DatasetService getDatasetService() {
        return datasetService;
    }

    /**
     * Get initialized service for exports management (export report,...)
     *
     * @return initialized service for exports
     */
    @Bean("goodDataExportService")
    public ExportService getExportService() {
        return exportService;
    }

    /**
     * Get initialized service for dataload processes management and process executions.
     *
     * @return initialized service for dataload processes management and process executions
     */
    @Bean("goodDataProcessService")
    public ProcessService getProcessService() {
        return processService;
    }

    /**
     * Get initialized service for ADS management (create, access and delete ads instances).
     *
     * @return initialized service for ADS management
     */
    @Bean("goodDataWarehouseService")
    public WarehouseService getWarehouseService() {
        return warehouseService;
    }

    /**
     * Get initialized service for connector integration management (create, update, start process, ...).
     *
     * @return initialized service for connector integration management
     */
    @Bean("goodDataConnectorService")
    public ConnectorService getConnectorService() {
        return connectorService;
    }

    /**
     * Get initialized service for project notifications management.
     *
     * @return initialized service for project notifications management
     */
    @Bean("goodDataNotificationService")
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Get initialized service for metadata export/import.
     *
     * @return initialized service for metadata export/import
     */
    @Bean("goodDataExportImportService")
    public ExportImportService getExportImportService() {
        return exportImportService;
    }

    /**
     * Get initialized service for feature flag management.
     *
     * @return initialized service for feature flag management
     */
    @Bean("goodDataFeatureFlagService")
    public FeatureFlagService getFeatureFlagService() {
        return featureFlagService;
    }

    /**
     * Get initialized service for output stage management.
     *
     * @return initialized service for output stage management
     */
    @Bean("goodDataOutputStageService")
    public OutputStageService getOutputStageService() {
        return outputStageService;
    }

    /**
     * Get initialized service for project templates
     *
     * @return initialized service for project templates
     */
    @Bean("goodDataProjectTemplateService")
    public ProjectTemplateService getProjectTemplateService() {
        return projectTemplateService;
    }

    /**
     * Get initialized service for audit events
     * @return initialized service for audit events
     */
    @Bean("goodDataAuditEventService")
    public AuditEventService getAuditEventService() {
        return auditEventService;
    }

    /**
     * Get initialized service for afm execution
     * @return initialized service for afm execution
     */
    @Bean("goodDataExecuteAfmService")
    public ExecuteAfmService getExecuteAfmService() {
        return executeAfmService;
    }

    /**
     * Get initialized service for Life Cycle Management
     * @return initialized service for Life Cycle Management
     */
    @Bean("goodDataLcmService")
    public LcmService getLcmService() {
        return lcmService;
    }
}
