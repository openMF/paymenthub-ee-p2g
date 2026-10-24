package org.mifos.connector.crm.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * The fields were renamed from ResponseCode, ResponseDescription and RequestID to satisfy checkstyle. Lombok generates
 * the same getters and setters for both spellings, so the JSON names stay what billRTPResp has always returned.
 */
class ResponseDTOJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesTheSamePropertyNames() throws Exception {
        assertThat(mapper.writeValueAsString(new ResponseDTO("00", "ok", "req-1")))
                .isEqualTo("{\"responseCode\":\"00\",\"responseDescription\":\"ok\",\"requestID\":\"req-1\"}");
    }

    @Test
    void readsTheSamePropertyNames() throws Exception {
        ResponseDTO dto = mapper.readValue("{\"responseCode\":\"00\",\"responseDescription\":\"ok\",\"requestID\":\"req-1\"}",
                ResponseDTO.class);
        assertThat(dto.getResponseCode()).isEqualTo("00");
        assertThat(dto.getResponseDescription()).isEqualTo("ok");
        assertThat(dto.getRequestID()).isEqualTo("req-1");
    }
}
