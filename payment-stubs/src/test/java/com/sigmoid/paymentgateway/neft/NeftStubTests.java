package com.sigmoid.paymentgateway.neft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Settle quickly so the async acknowledgement can be asserted within the test.
@SpringBootTest(properties = {
        "stub.neft.ack-delay-min=0",
        "stub.neft.ack-delay-seconds=0",
        "stub.neft.batch-scan-ms=200"
})
@AutoConfigureMockMvc
class NeftStubTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void transfer_accepted_thenSettledAsync() throws Exception {
        String refId = "PAY-neft-1";
        mvc.perform(post("/internal/neft/transfer")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":45000.00,\"ifsc\":\"HDFC0001234\",\"refId\":\"" + refId + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.ackEta").exists());

        // The batch settler flips the transfer to SUCCESS on a later tick.
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MvcResult result = mvc.perform(get("/internal/neft/transfer/" + refId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(body.get("utr").asText()).startsWith("UTR");
        });
    }

    @Test
    void unknownRefId_returnsNotFoundEnvelope() throws Exception {
        mvc.perform(get("/internal/neft/transfer/PAY-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void transfer_withBadIfsc_returnsValidationError() throws Exception {
        mvc.perform(post("/internal/neft/transfer")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"ifsc\":\"bad\",\"refId\":\"PAY-x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
