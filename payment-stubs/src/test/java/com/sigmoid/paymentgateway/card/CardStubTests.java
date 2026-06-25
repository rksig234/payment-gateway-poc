package com.sigmoid.paymentgateway.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.card.config.CardStubConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// timeout-ms tiny so the timeout path returns immediately.
@SpringBootTest(properties = {
        "stub.card.timeout-rate=0",
        "stub.card.challenge-rate=0",
        "stub.card.timeout-ms=10"
})
@AutoConfigureMockMvc
class CardStubTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CardStubConfig config;

    @AfterEach
    void reset() {
        config.apply(0, 0, null); // reset rates; keep small timeoutMs from props
    }

    @Test
    void charge_frictionless_returnsSuccess3dsPassed() throws Exception {
        mvc.perform(post("/internal/card/charge")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":2999.00,\"panToken\":\"tok_4242\",\"refId\":\"PAY-1e7b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.challenge").value("3DS_PASSED"));
    }

    @Test
    void charge_whenTimeoutForced_returns504() throws Exception {
        mvc.perform(post("/internal/admin/card/config")
                        .contentType(APPLICATION_JSON)
                        .content("{\"timeoutRate\":100}"))
                .andExpect(status().isOk());

        mvc.perform(post("/internal/card/charge")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"panToken\":\"tok_x\",\"refId\":\"PAY-t\"}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error.code").value("PROVIDER_TIMEOUT"));
    }

    @Test
    void charge_whenChallengeForced_thenCompletes() throws Exception {
        mvc.perform(post("/internal/admin/card/config")
                        .contentType(APPLICATION_JSON)
                        .content("{\"challengeRate\":100}"))
                .andExpect(status().isOk());

        MvcResult challenged = mvc.perform(post("/internal/card/charge")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":500.00,\"panToken\":\"tok_y\",\"refId\":\"PAY-c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRES_CHALLENGE"))
                .andExpect(jsonPath("$.challengeId").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(challenged.getResponse().getContentAsString());
        String challengeId = body.get("challengeId").asText();

        mvc.perform(post("/internal/card/challenge/" + challengeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.challenge").value("3DS_PASSED"));
    }

    @Test
    void charge_withInvalidBody_returnsValidationError() throws Exception {
        mvc.perform(post("/internal/card/charge")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":0,\"panToken\":\"\",\"refId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
