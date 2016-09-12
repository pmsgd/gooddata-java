/**
 * Copyright (C) 2004-2016, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.connector;

import com.gooddata.AbstractGoodDataIT;
import com.gooddata.GoodDataException;
import com.gooddata.gdc.UriResponse;
import com.gooddata.project.Project;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.gooddata.JsonMatchers.serializesToJson;
import static com.gooddata.connector.Status.Code.ERROR;
import static com.gooddata.connector.Status.Code.SYNCHRONIZED;
import static com.gooddata.util.ResourceUtils.*;
import static java.util.Collections.singletonMap;
import static net.jadler.Jadler.onRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConnectorServiceIT extends AbstractGoodDataIT {
    private Project project;
    private ConnectorService connectors;

    @BeforeMethod
    public void setUp() throws Exception {
        project = MAPPER.readValue(readFromResource("/project/project.json"), Project.class);
        connectors = gd.getConnectorService();
    }

    @Test
    public void shouldCreateIntegration() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/md/PROJECT_ID/templates")
            .respond()
                .withBody(readFromResource("/project/project-templates.json"));

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration")
            .respond()
                .withBody(readFromResource("/connector/integration.json"));

        onRequest()
                .havingMethodEqualTo("PUT")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/settings")
            .respond()
                .withStatus(200);

        final Zendesk4Settings settings = new Zendesk4Settings("http://zendesk");
        final Integration integration = connectors.createIntegration(project, settings);
        assertThat(integration, notNullValue());
    }

    @Test
    public void shouldGetIntegration() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration")
            .respond()
                .withBody(readFromResource("/connector/integration.json"));

        final Integration integration = connectors.getIntegration(project, ConnectorType.ZENDESK4);
        assertThat(integration, notNullValue());
    }

    @Test(expectedExceptions = IntegrationNotFoundException.class)
    public void shouldFailGetIntegrationNotFount() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration")
            .respond()
                .withStatus(404);

        connectors.getIntegration(project, ConnectorType.ZENDESK4);
    }

    @Test
    public void shouldUpdateIntegration() throws Exception {
        onRequest()
                .havingMethodEqualTo("PUT")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration")
            .respond()
                .withBody(readFromResource("/connector/integration.json"));

        final Integration integration = new Integration("/projectTemplates/template");
        connectors.updateIntegration(project, ConnectorType.ZENDESK4, integration);
    }

    @Test(expectedExceptions = IntegrationNotFoundException.class)
    public void shouldFailUpdateIntegrationNotFound() throws Exception {
        onRequest()
                .havingMethodEqualTo("PUT")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration")
            .respond()
                .withStatus(404);

        final Integration integration = new Integration("/projectTemplates/template");
        connectors.updateIntegration(project, ConnectorType.ZENDESK4, integration);
    }

    @Test
    public void shouldExecuteProcess() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes")
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")));
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")
            .respond()
                .withBody(readFromResource("/connector/process-status-scheduled.json"))
            .thenRespond()
                .withBody(readFromResource("/connector/process-status-finished.json"))
        ;

        final ProcessStatus process = connectors.executeProcess(project, new Zendesk4ProcessExecution()).get();
        assertThat(process.getStatus().getCode(), is(SYNCHRONIZED.name()));
    }

    @Test(expectedExceptions = ConnectorException.class, expectedExceptionsMessageRegExp = ".*zendesk4 process PROCESS failed.*")
    public void shouldFailExecuteProcessPolling() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes")
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")));
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")
            .respond()
                .withStatus(400)
        ;
        connectors.executeProcess(project, new Zendesk4ProcessExecution()).get();
    }

    @Test(expectedExceptions = GoodDataException.class)
    public void shouldFailExecuteProcess() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes")
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")));
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS")
            .respond()
                .withBody(readFromResource("/connector/process-status-error.json"));

        final ProcessStatus process = connectors.executeProcess(project, new Zendesk4ProcessExecution()).get();
        assertThat(process.getStatus().getCode(), is(ERROR.name()));
    }

    @Test
    public void shouldGetProcessStatus() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID")
            .respond()
                .withBody(readFromResource("/connector/process-status-scheduled.json"))
            .thenRespond()
                .withBody(readFromResource("/connector/process-status-finished.json"));

        final IntegrationProcessStatus runningProcess = new IntegrationProcessStatus(null, null, null,
                singletonMap("self", "/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID"));
        final ProcessStatus process = connectors.getProcessStatus(runningProcess).get();
        assertThat(process.getStatus().getCode(), is(SYNCHRONIZED.name()));
    }

    @Test(expectedExceptions = ConnectorException.class, expectedExceptionsMessageRegExp = ".*zendesk4 process PROCESS_ID failed.*")
    public void shouldFailGetProcessStatusPolling() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID")
            .respond()
                .withStatus(400);
        final IntegrationProcessStatus runningProcess = new IntegrationProcessStatus(null, null, null,
                singletonMap("self", "/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID"));
        connectors.getProcessStatus(runningProcess).get();
    }

    @Test(expectedExceptions = GoodDataException.class)
    public void shouldFailGetProcessStatus() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID")
            .respond()
                .withBody(readFromResource("/connector/process-status-error.json"));

        final IntegrationProcessStatus runningProcess = new IntegrationProcessStatus(null, null, null,
                singletonMap("self", "/gdc/projects/PROJECT_ID/connectors/zendesk4/integration/processes/PROCESS_ID"));
        final ProcessStatus process = connectors.getProcessStatus(runningProcess).get();
        assertThat(process.getStatus().getCode(), is(ERROR.name()));
    }

    @Test
    public void shouldGetSettings() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
            .respond()
                .withBody(readFromResource("/connector/settings-zendesk4.json"));

        final Zendesk4Settings zendesk4Settings = connectors.getZendesk4Settings(project);
        assertThat(zendesk4Settings, serializesToJson("/connector/settings-zendesk4.json"));
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void shouldGetSettingsNotFound() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
            .respond()
                .withStatus(404);

        connectors.getZendesk4Settings(project);
    }
}
