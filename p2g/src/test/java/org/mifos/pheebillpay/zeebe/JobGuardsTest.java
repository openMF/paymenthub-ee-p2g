package org.mifos.pheebillpay.zeebe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

class JobGuardsTest {

    @Test
    void requiredReturnsThePresentValue() {
        assertThat(JobGuards.required(Map.of("tenantId", "greenbank"), "tenantId")).isEqualTo("greenbank");
    }

    @Test
    void requiredNamesTheMissingVariable() {
        assertThatThrownBy(() -> JobGuards.required(Map.of(), "tenantId")).isInstanceOf(JobGuards.MissingJobVariableException.class)
                .hasMessage("Missing job variable tenantId");
    }

    @Test
    void aMissingVariableFailsTheJobWithoutRetries() throws Exception {
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getKey()).thenReturn(42L);

        JobGuards.failOnMissingVariables((c, j) -> JobGuards.required(Map.of(), "callbackUrl")).handle(client, job);

        verify(client.newFailCommand(42L)).retries(0);
        verify(client.newFailCommand(42L).retries(0)).errorMessage("Missing job variable callbackUrl");
    }

    @Test
    void aFailedRouteFailsTheJobWithOneRetryFewer() {
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getKey()).thenReturn(42L);
        when(job.getRetries()).thenReturn(3);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setException(new IllegalStateException("connection refused"));

        assertThat(JobGuards.routeFailed(exchange, client, job, "direct:bill-inquiry")).isTrue();
        verify(client.newFailCommand(42L)).retries(2);
    }

    @Test
    void aWorkingRouteLeavesTheJobAlone() {
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());

        assertThat(JobGuards.routeFailed(exchange, client, mock(ActivatedJob.class), "direct:bill-inquiry")).isFalse();
        verifyNoInteractions(client);
    }
}
