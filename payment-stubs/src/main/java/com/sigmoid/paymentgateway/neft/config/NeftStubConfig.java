package com.sigmoid.paymentgateway.neft.config;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime-tunable config knobs for the NEFT stub (deck: ackDelayMin 5-120,
 * batchWindow 30m). {@code ackDelaySeconds} is a demo-only override so a live
 * walkthrough doesn't have to wait minutes for an acknowledgement; when it is
 * greater than zero it is used instead of {@code ackDelayMin}.
 */
@Component
@ConfigurationProperties(prefix = "stub.neft")
public class NeftStubConfig {

    /** Delay before the async acknowledgement, in minutes. */
    private volatile int ackDelayMin = 5;

    /** Conceptual batch settlement cadence, in minutes (reported to callers). */
    private volatile int batchWindow = 30;

    /** Demo override in seconds; if > 0 it takes precedence over ackDelayMin. */
    private volatile int ackDelaySeconds = 0;

    public int getAckDelayMin() {
        return ackDelayMin;
    }

    public void setAckDelayMin(int ackDelayMin) {
        this.ackDelayMin = ackDelayMin;
    }

    public int getBatchWindow() {
        return batchWindow;
    }

    public void setBatchWindow(int batchWindow) {
        this.batchWindow = batchWindow;
    }

    public int getAckDelaySeconds() {
        return ackDelaySeconds;
    }

    public void setAckDelaySeconds(int ackDelaySeconds) {
        this.ackDelaySeconds = ackDelaySeconds;
    }

    /** Effective delay (seconds) before a transfer is settled. */
    public long effectiveDelaySeconds() {
        return ackDelaySeconds > 0 ? ackDelaySeconds : (long) ackDelayMin * 60L;
    }

    /** Human-readable ETA label, e.g. "T+5m" or "T+30s" for demo mode. */
    public String ackEtaLabel() {
        return ackDelaySeconds > 0 ? "T+" + ackDelaySeconds + "s" : "T+" + ackDelayMin + "m";
    }

    /** Validated runtime update from the admin endpoint. */
    public synchronized void apply(Integer newAckDelayMin, Integer newBatchWindow, Integer newAckDelaySeconds) {
        if (newAckDelayMin != null) {
            if (newAckDelayMin < 0 || newAckDelayMin > 1440) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "ackDelayMin must be between 0 and 1440");
            }
            this.ackDelayMin = newAckDelayMin;
        }
        if (newBatchWindow != null) {
            if (newBatchWindow < 1 || newBatchWindow > 1440) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "batchWindow must be between 1 and 1440");
            }
            this.batchWindow = newBatchWindow;
        }
        if (newAckDelaySeconds != null) {
            if (newAckDelaySeconds < 0 || newAckDelaySeconds > 86400) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "ackDelaySeconds must be between 0 and 86400");
            }
            this.ackDelaySeconds = newAckDelaySeconds;
        }
    }
}
