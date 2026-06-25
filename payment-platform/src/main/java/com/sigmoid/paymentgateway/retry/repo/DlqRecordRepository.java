package com.sigmoid.paymentgateway.retry.repo;

import com.sigmoid.paymentgateway.retry.domain.DlqRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DlqRecordRepository extends JpaRepository<DlqRecordEntity, Long> {

    Optional<DlqRecordEntity> findTopByPaymentIdOrderByMovedAtDesc(String paymentId);
}
