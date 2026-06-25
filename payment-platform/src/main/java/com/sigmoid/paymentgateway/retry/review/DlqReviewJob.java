package com.sigmoid.paymentgateway.retry.review;

import com.sigmoid.paymentgateway.retry.repo.DlqRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily DLQ review (deck: "Spring Batch job processes DLQ records daily for manual
 * review"). Kept as a scheduled summary here; swap in a Spring Batch job for richer
 * reporting/replay workflows.
 */
@Component
public class DlqReviewJob {

    private static final Logger log = LoggerFactory.getLogger(DlqReviewJob.class);

    private final DlqRecordRepository repo;

    public DlqReviewJob(DlqRecordRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "${dlq.review.cron:0 0 9 * * *}")
    public void review() {
        long depth = repo.count();
        log.info("dlq_daily_review depth={}", depth);
    }
}
