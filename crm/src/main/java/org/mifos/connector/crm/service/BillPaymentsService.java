package org.mifos.connector.crm.service;

import org.mifos.connector.crm.data.BillPaymentsReqDTO;
import org.mifos.connector.crm.data.BillPaymentsResponseDTO;
import org.mifos.connector.crm.utils.BillPayEnum;
import org.springframework.stereotype.Service;

@Service
public class BillPaymentsService {

    public BillPaymentsResponseDTO billPayments(String tenantId, String correlationId, String payerFspId, BillPaymentsReqDTO body) {
        BillPaymentsResponseDTO billPaymentsResponseDTO = new BillPaymentsResponseDTO();
        billPaymentsResponseDTO.setBillId(body.getBillId());
        billPaymentsResponseDTO.setCode(BillPayEnum.SUCCESS_RESPONSE_CODE.toString());
        billPaymentsResponseDTO.setReason(BillPayEnum.SUCCESS_RESPONSE_MESSAGE.toString());
        billPaymentsResponseDTO.setStatus(BillPayEnum.SUCCESS_STATUS.toString());
        billPaymentsResponseDTO.setRequestID(correlationId);
        return billPaymentsResponseDTO;
    }

}
