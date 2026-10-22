package org.mifos.connector.crm.data;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillRTPReqDTO implements Serializable {

    private String billRequestId;
    private String billId;
    private String status;
    private String rejectionReason;

}
