package com.sigmoid.paymentgateway.status.api;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.status.api.dto.StatusResponse;
import com.sigmoid.paymentgateway.status.api.dto.TimelineResponse;
import com.sigmoid.paymentgateway.status.domain.PaymentEntity;
import com.sigmoid.paymentgateway.status.repo.PaymentRepository;
import com.sigmoid.paymentgateway.status.repo.PaymentTimelineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read API over the durable payment state machine (deck timeline contract). */
@RestController
@RequestMapping("/v1/payments")
public class StatusController {

    private final PaymentRepository payments;
    private final PaymentTimelineRepository timeline;

    public StatusController(PaymentRepository payments, PaymentTimelineRepository timeline) {
        this.payments = payments;
        this.timeline = timeline;
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<StatusResponse> status(@PathVariable String paymentId) {
        PaymentEntity p = payments.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No payment " + paymentId));
        return ResponseEntity.ok(new StatusResponse(
                p.getPaymentId(),
                p.getStatus() == null ? null : p.getStatus().name(),
                p.getProvider(),
                p.getRetries(),
                p.getUpdatedAt()));
    }

    @GetMapping("/{paymentId}/timeline")
    public ResponseEntity<TimelineResponse> timeline(@PathVariable String paymentId) {
        PaymentEntity p = payments.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No payment " + paymentId));
        List<TimelineResponse.Transition> transitions = timeline.findByPaymentIdOrderByEventTimeAsc(paymentId).stream()
                .map(t -> new TimelineResponse.Transition(t.getState().name(), t.getEventTime()))
                .toList();
        return ResponseEntity.ok(new TimelineResponse(
                paymentId,
                p.getStatus() == null ? null : p.getStatus().name(),
                transitions));
    }
}
