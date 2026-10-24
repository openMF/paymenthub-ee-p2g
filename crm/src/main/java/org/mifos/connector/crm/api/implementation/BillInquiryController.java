package org.mifos.connector.crm.api.implementation;

import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_ID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.CLIENTCORRELATIONID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.FIELDS;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.PAYER_FSP;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.PLATFORM_TENANT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.connector.crm.api.definition.BillInquiryApi;
import org.mifos.connector.crm.data.BillInquiryResponseDTO;
import org.mifos.connector.crm.utils.Headers;
import org.mifos.connector.crm.utils.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BillInquiryController implements BillInquiryApi {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public BillInquiryResponseDTO billInquiry(String tenantId, String correlationId, String payerFspId, String billId, String field)
            throws ExecutionException, InterruptedException, JsonProcessingException {
        Headers headers = new Headers.HeaderBuilder().addHeader(PLATFORM_TENANT, tenantId).addHeader(CLIENTCORRELATIONID, correlationId)
                .addHeader(PAYER_FSP, payerFspId).addHeader(BILL_ID, billId).addHeader(FIELDS, field).build();
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers, null);
        producerTemplate.send("direct:bill-inquiry", exchange);
        if (exchange.getException() != null) {
            // send() leaves the failure on the exchange; answering 200 with an empty body hid it.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "direct:bill-inquiry failed: " + exchange.getException().getMessage(), exchange.getException());
        }
        return exchange.getIn().getBody(BillInquiryResponseDTO.class);
    }
}
