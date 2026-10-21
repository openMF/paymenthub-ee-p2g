package org.mifos.pheebillpay.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import org.mifos.pheebillpay.properties.ZeebeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("${zeebe.enabled:true}")
public class ZeebeClientConfiguration {

    // No defaultJobPollInterval override: it was 1 ms, a poll every millisecond per worker.
    // The client default (100 ms, with long polling on) picks jobs up as soon as they exist.
    @Bean
    public ZeebeClient setup(ZeebeProperties zeebeProperties) {
        return ZeebeClient.newClientBuilder().gatewayAddress(zeebeProperties.broker().contactpoint()).usePlaintext()
                .defaultJobWorkerMaxJobsActive(2000).numJobWorkerExecutionThreads(zeebeProperties.client().maxExecutionThreads()).build();
    }
}
