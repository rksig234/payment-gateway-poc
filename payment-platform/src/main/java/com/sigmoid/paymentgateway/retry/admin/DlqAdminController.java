package com.sigmoid.paymentgateway.retry.admin;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.retry.domain.DlqRecordEntity;
import com.sigmoid.paymentgateway.retry.events.RetryEventPublisher;
import com.sigmoid.paymentgateway.retry.repo.DlqRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DLQ inspection + manual replay (deck: POST /admin/dlq/{paymentId}/replay). */
@RestController
@RequestMapping("/admin/dlq")
public class DlqAdminController {

    private static final Logger log = LoggerFactory.getLogger(DlqAdminController.class);

    private final DlqRecordRepository repo;
    private final RetryEventPublisher publisher;

    public DlqAdminController(DlqRecordRepository repo, RetryEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @GetMapping
    public List<DlqRecordEntity> list() {
        return repo.findAll();
    }

    @PostMapping("/{paymentId}/replay")
    public Map<String, Object> replay(@PathVariable String paymentId) {
        DlqRecordEntity record = repo.findTopByPaymentIdOrderByMovedAtDesc(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No DLQ record for " + paymentId));

        PaymentRetryEvent replay = new PaymentRetryEvent(
                paymentId, null, 1, "MANUAL_REPLAY", Instant.now().plusSeconds(1), record.getPaymentId(), Instant.now());
        publisher.publishRetry(replay);

        log.info("dlq_replay_requested paymentId={}", paymentId);
        return Map.of("paymentId", paymentId, "action", "replayed", "fromAttempt", record.getAttemptCount());
    }
}
