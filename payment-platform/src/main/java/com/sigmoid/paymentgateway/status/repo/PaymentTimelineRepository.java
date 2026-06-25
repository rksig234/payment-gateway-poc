package com.sigmoid.paymentgateway.status.repo;

import com.sigmoid.paymentgateway.status.domain.PaymentTimelineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTimelineRepository extends JpaRepository<PaymentTimelineEntity, Long> {

    List<PaymentTimelineEntity> findByPaymentIdOrderByEventTimeAsc(String paymentId);
}
