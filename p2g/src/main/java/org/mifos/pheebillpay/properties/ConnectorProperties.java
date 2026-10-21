package org.mifos.pheebillpay.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The connector the payer request-to-pay is sent to. Every value is required, as it was when it was a bare
 * {@code @Value} field.
 */
@Validated
@ConfigurationProperties(prefix = "connector")
public record ConnectorProperties(@NotNull String contactpoint, @NotNull @Valid Endpoint endpoint) {

    /** {@code connector.endpoint.payerRtp}: the path the payer request-to-pay is posted to. */
    public record Endpoint(@NotNull String payerRtp) {
    }
}
