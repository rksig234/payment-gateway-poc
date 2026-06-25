package com.sigmoid.paymentgateway.status.repo;

import com.sigmoid.paymentgateway.status.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
}
