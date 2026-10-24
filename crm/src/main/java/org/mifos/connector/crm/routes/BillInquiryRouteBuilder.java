package org.mifos.connector.crm.routes;

import static org.mifos.connector.crm.utils.BillPayEnum.FAILED_RESPONSE_CODE;
import static org.mifos.connector.crm.utils.BillPayEnum.SUCCESS_RESPONSE_CODE;
import static org.mifos.connector.crm.utils.BillPayEnum.SUCCESS_RESPONSE_MESSAGE;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.AMOUNT;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILLER_ID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILLER_NAME;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_FETCH_FAILED;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_ID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.BILL_INQUIRY_RESPONSE;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.CLIENTCORRELATIONID;
import static org.mifos.connector.crm.zeebe.ZeebeVariables.ERROR_INFORMATION;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.mifos.connector.common.camel.ErrorHandlerRouteBuilder;
import org.mifos.connector.crm.data.Bill;
import org.mifos.connector.crm.data.BillInquiryResponseDTO;
import org.mifos.connector.crm.properties.BillPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BillInquiryRouteBuilder extends ErrorHandlerRouteBuilder {

    private final ObjectMapper objectMapper;

    private final String billIdInvalidId;

    private final String billIdEmptyId;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public BillInquiryRouteBuilder(BillPayProperties billPayProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.billIdInvalidId = billPayProperties.billIdInvalidId();
        this.billIdEmptyId = billPayProperties.billIdEmptyId();
    }

    @Override
    public void configure() {

        from("direct:bill-inquiry").routeId("bill-inquiry").log("Received request for bill inquiry")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200)).process(exchange -> {
                    logger.debug("Bill Inquiry Id: {}", exchange.getIn().getHeader(BILL_ID).toString());
                    BillInquiryResponseDTO billInquiryResponseDTO;
                    String billId = exchange.getIn().getHeader(BILL_ID).toString();
                    String billerID = exchange.getProperty(BILLER_ID).toString();
                    String billerName = exchange.getProperty(BILLER_NAME).toString();
                    if (billId.equals(billIdInvalidId)) {
                        logger.info("Bill Id is Invalid");
                        billInquiryResponseDTO = setResponseBodyForInvalidBill(exchange.getIn().getHeader(CLIENTCORRELATIONID).toString());
                        exchange.setProperty(BILL_FETCH_FAILED, true);
                        exchange.setProperty(ERROR_INFORMATION, "Bill Fetch failed: Invalid Bill Id");
                    } else if (billId.equals(billIdEmptyId)) {
                        logger.info("Bill Id is Empty");
                        billInquiryResponseDTO = setResponseBodyForEmptyBill(exchange.getIn().getHeader(CLIENTCORRELATIONID).toString());
                        exchange.setProperty(BILL_FETCH_FAILED, true);
                        exchange.setProperty(ERROR_INFORMATION, "Bill Fetch failed: Empty bill ID");
                    } else {
                        billInquiryResponseDTO = setResponseBody(exchange.getIn().getHeader(CLIENTCORRELATIONID).toString(), billId,
                                billerID, billerName);
                        exchange.setProperty(BILL_FETCH_FAILED, false);
                    }
                    exchange.setProperty(BILL_INQUIRY_RESPONSE, billInquiryResponseDTO);
                    // the failure responses carry no bill details
                    Bill billDetails = billInquiryResponseDTO.getBillDetails();
                    exchange.setProperty(AMOUNT, billDetails == null ? null : billDetails.getAmountonDueDate());
                    String jsonString = objectMapper.writeValueAsString(billInquiryResponseDTO);
                    exchange.getIn().setBody(jsonString);
                    logger.debug("Bill Inquiry Response: {}", jsonString);
                });
    }

    // A new response per request: these used to be injected singletons that every request
    // wrote into, so concurrent inquiries could be answered with each other's bill.
    private BillInquiryResponseDTO setResponseBody(String clientCorrelationId, String billId, String billerID, String billerName) {
        Bill billDetails = new Bill();
        BillInquiryResponseDTO billInquiryResponseDTO = new BillInquiryResponseDTO();
        billDetails.setBillerId(billerID);
        billDetails.setBillerName(billerName);
        billDetails.setAmountonDueDate("1000");
        billDetails.setAmountAfterDueDate("1100");
        billDetails.setDueDate("2021-07-01");
        billDetails.setBillStatus("PAID");
        billInquiryResponseDTO.setBillDetails(billDetails);
        billInquiryResponseDTO.setBillId(billId);
        billInquiryResponseDTO.setCode(SUCCESS_RESPONSE_CODE.getValue());
        billInquiryResponseDTO.setReason(SUCCESS_RESPONSE_MESSAGE.getValue());
        billInquiryResponseDTO.setClientCorrelationId(clientCorrelationId);
        return billInquiryResponseDTO;
    }

    private BillInquiryResponseDTO setResponseBodyForInvalidBill(String clientCorrelationId) {
        BillInquiryResponseDTO billInquiryResponseDTO = new BillInquiryResponseDTO();
        billInquiryResponseDTO.setCode(FAILED_RESPONSE_CODE.getValue());
        billInquiryResponseDTO.setReason("Invalid Bill ID");
        billInquiryResponseDTO.setClientCorrelationId(clientCorrelationId);
        return billInquiryResponseDTO;
    }

    private BillInquiryResponseDTO setResponseBodyForEmptyBill(String clientCorrelationId) {
        BillInquiryResponseDTO billInquiryResponseDTO = new BillInquiryResponseDTO();
        billInquiryResponseDTO.setCode(FAILED_RESPONSE_CODE.getValue());
        billInquiryResponseDTO.setReason("Empty Bill ID");
        billInquiryResponseDTO.setClientCorrelationId(clientCorrelationId);
        return billInquiryResponseDTO;
    }

}
