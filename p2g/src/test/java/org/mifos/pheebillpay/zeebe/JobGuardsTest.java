package org.mifos.pheebillpay.zeebe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import java.util.Map;
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
}
