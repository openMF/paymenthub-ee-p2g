package org.mifos.connector.crm.zeebe;

import static org.mifos.connector.crm.zeebe.JobGuards.failOnMissingVariables;
import static org.mifos.connector.crm.zeebe.JobGuards.required;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.json.JSONObject;
import org.mifos.connector.crm.data.BillPaymentsReqDTO;
import org.mifos.connector.crm.properties.StatusProperties;
import org.mifos.connector.crm.utils.Headers;
import org.mifos.connector.crm.utils.SpringWrapperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ZeebeWorkers {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    private final String billReqAcceptedId;

    private final int billTimeout;

    public ZeebeWorkers(StatusProperties statusProperties) {
        this.billReqAcceptedId = statusProperties.billReqAcceptedId();
        this.billTimeout = statusProperties.billTimeout();
    }

    @PostConstruct
    public void setupWorkers() {

        // fetch bill details worker for sync mock api with response
        zeebeClient.newWorker().jobType("fetch-bill").handler(failOnMissingVariables((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logWorkerDetails(job);
            Map<String, Object> variables = job.getVariablesAsMap();
            Headers headers = new Headers.HeaderBuilder().addHeader(PLATFORM_TENANT, required(variables, TENANT_ID))
                    .addHeader(CLIENTCORRELATIONID, required(variables, CLIENTCORRELATIONID))
                    .addHeader(PAYER_FSP, required(variables, "payerFspId")).addHeader(BILL_ID, required(variables, BILL_ID)).build();
            Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers, null);
            exchange.setProperty(BILLER_ID, required(variables, BILLER_ID));
            exchange.setProperty(BILLER_NAME, required(variables, BILLER_NAME));
            producerTemplate.send("direct:bill-inquiry", exchange);
            variables.put(BILL_INQUIRY_RESPONSE, exchange.getProperty(BILL_INQUIRY_RESPONSE));
            variables.put(BILL_FETCH_FAILED, exchange.getProperty(BILL_FETCH_FAILED));
            variables.put(AMOUNT, exchange.getProperty(AMOUNT));
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            logger.debug("Zeebe variable {}", job.getVariablesAsMap());
        })).name("fetch-bill").maxJobsActive(workerMaxJobs).open();

        // pay bill status worker for sync mock api with response
        zeebeClient.newWorker().jobType("billPay").handler(failOnMissingVariables((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logWorkerDetails(job);
            Map<String, Object> variables = job.getVariablesAsMap();
            if (billReqAcceptedId.equals(variables.get(BILL_ID))) {
                pauseExec();
            }
            Headers headers = new Headers.HeaderBuilder().addHeader(PLATFORM_TENANT, required(variables, TENANT_ID))
                    .addHeader(CLIENTCORRELATIONID, required(variables, CLIENTCORRELATIONID))
                    .addHeader(PAYER_FSP, required(variables, "payerFspId")).build();
            Gson gson = new Gson();
            BillPaymentsReqDTO requestBody = gson.fromJson((required(variables, BILL_PAYMENTS_REQ)), BillPaymentsReqDTO.class);
            Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers,
                    objectMapper.writeValueAsString(requestBody));
            producerTemplate.send("direct:bill-payments", exchange);
            variables.put(BILL_PAY_RESPONSE, exchange.getProperty(BILL_PAY_RESPONSE));
            variables.put("code", exchange.getProperty("code"));
            variables.put("status", exchange.getProperty("status"));
            variables.put("reason", exchange.getProperty("reason"));
            variables.put("state", "ACCEPTED");
            variables.put(BILL_PAY_FAILED, exchange.getProperty(BILL_PAY_FAILED));
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            logger.debug("Zeebe variable {}", job.getVariablesAsMap());
        })).name("billPay").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("billRtpAck").handler(failOnMissingVariables((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logWorkerDetails(job);
            Map<String, Object> variables = job.getVariablesAsMap();
            Headers headers = new Headers.HeaderBuilder().addHeader("X-Platform-TenantId", required(variables, TENANT_ID))
                    .addHeader(CLIENTCORRELATIONID, required(variables, CLIENTCORRELATIONID)).build();
            Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers, null);
            // check before implementing
            producerTemplate.send("direct:send-ack", exchange);
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            logger.debug("Zeebe variable {}", job.getVariablesAsMap());
        })).name("billRtpAck").maxJobsActive(workerMaxJobs).open();

        // billRTPResp
        zeebeClient.newWorker().jobType("billRTPResp").handler(failOnMissingVariables((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logWorkerDetails(job);
            Map<String, Object> variables = job.getVariablesAsMap();
            Headers headers = new Headers.HeaderBuilder().addHeader("X-Platform-TenantId", required(variables, TENANT_ID))
                    .addHeader(CLIENTCORRELATIONID, required(variables, CLIENTCORRELATIONID)).build();
            Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers, null);
            producerTemplate.send("direct:bill-rtp-resp", exchange);
            variables.put("billRTPResponse", exchange.getIn().getBody(String.class));
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            logger.debug("Zeebe variable {}", job.getVariablesAsMap());
        })).name("billRTPResp").maxJobsActive(workerMaxJobs).open();

    }

    private void pauseExec() {
        try {
            logger.info("Pausing execution for capturing intermediary status ");
            TimeUnit.SECONDS.sleep(billTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        logger.info("Resuming execution post pause");
    }

    private void logWorkerDetails(ActivatedJob job) {
        JSONObject jsonJob = new JSONObject();
        jsonJob.put("bpmnProcessId", job.getBpmnProcessId());
        jsonJob.put("elementInstanceKey", job.getElementInstanceKey());
        jsonJob.put("jobKey", job.getKey());
        jsonJob.put("jobType", job.getType());
        jsonJob.put("workflowElementId", job.getElementId());
        jsonJob.put("workflowDefinitionVersion", job.getProcessDefinitionVersion());
        jsonJob.put("workflowKey", job.getProcessDefinitionKey());
        jsonJob.put("workflowInstanceKey", job.getProcessInstanceKey());
        logger.info("Job started: {}", jsonJob.toString(4));
    }
}
