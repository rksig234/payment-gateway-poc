package com.sigmoid.paymentgateway.upi;

import com.sigmoid.paymentgateway.upi.config.UpiStubConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"stub.upi.latency-ms=0", "stub.upi.failure-rate=0"})
@AutoConfigureMockMvc
class UpiStubTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UpiStubConfig config;

    @AfterEach
    void reset() {
        config.apply(0, 0); // isolate tests that mutate the shared config
    }

    @Test
    void debit_returnsSuccessWithUtr_andEchoesTraceId() throws Exception {
        mvc.perform(post("/internal/upi/debit")
                        .contentType(APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-abc")
                        .content("{\"amount\":1500.00,\"vpa\":\"merchant@bank\",\"refId\":\"PAY-7f3a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.utr").exists())
                .andExpect(header().string("X-Trace-Id", "trace-abc"));
    }

    @Test
    void debit_withInvalidBody_returnsValidationEnvelope() throws Exception {
        mvc.perform(post("/internal/upi/debit")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":-5,\"vpa\":\"\",\"refId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.traceId").exists());
    }

    @Test
    void adminCanForceFailure_thenDebitReturns503() throws Exception {
        mvc.perform(post("/internal/admin/upi/config")
                        .contentType(APPLICATION_JSON)
                        .content("{\"failureRate\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureRate").value(100));

        mvc.perform(post("/internal/upi/debit")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":10,\"vpa\":\"a@bank\",\"refId\":\"PAY-x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("PROVIDER_UNAVAILABLE"));
    }

    @Test
    void adminConfig_isReadable() throws Exception {
        mvc.perform(get("/internal/admin/upi/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("UPI"));
    }
}
