package org.mifos.connector.crm.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILLER_ID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILLER_NAME;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_FETCH_FAILED;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_ID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_INQUIRY_RESPONSE;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.CLIENTCORRELATIONID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mifos.connector.crm.data.BillInquiryResponseDTO;
import org.mifos.connector.crm.properties.BillPayProperties;

class BillInquiryRouteBuilderTest {

    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;

    @BeforeEach
    void start() throws Exception {
        BillPayProperties properties = new BillPayProperties("001", "002", "003", "004");
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new BillInquiryRouteBuilder(properties, new ObjectMapper()));
        camelContext.start();
        producerTemplate = camelContext.createProducerTemplate();
    }

    @AfterEach
    void stop() {
        camelContext.stop();
    }

    private Exchange inquire(String billId, String correlationId) {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(BILL_ID, billId);
        exchange.getIn().setHeader(CLIENTCORRELATIONID, correlationId);
        exchange.setProperty(BILLER_ID, "biller-1");
        exchange.setProperty(BILLER_NAME, "Biller One");
        return producerTemplate.send("direct:bill-inquiry", exchange);
    }

    @Test
    void eachInquiryGetsItsOwnResponse() {
        BillInquiryResponseDTO first = inquire("100", "corr-1").getProperty(BILL_INQUIRY_RESPONSE, BillInquiryResponseDTO.class);
        BillInquiryResponseDTO second = inquire("200", "corr-2").getProperty(BILL_INQUIRY_RESPONSE, BillInquiryResponseDTO.class);

        assertThat(first).isNotSameAs(second);
        assertThat(first.getBillId()).isEqualTo("100");
        assertThat(first.getClientCorrelationId()).isEqualTo("corr-1");
        assertThat(second.getBillId()).isEqualTo("200");
    }

    @Test
    void anInvalidBillIsAnsweredWithoutBillDetails() {
        Exchange exchange = inquire("002", "corr-3");

        assertThat(exchange.getException()).isNull();
        assertThat(exchange.getProperty(BILL_FETCH_FAILED, Boolean.class)).isTrue();
        assertThat(exchange.getProperty(BILL_INQUIRY_RESPONSE, BillInquiryResponseDTO.class).getReason()).isEqualTo("Invalid Bill ID");
    }
}
