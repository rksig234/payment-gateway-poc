package com.sigmoid.paymentgateway.common.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Reference-id generators that mirror the formats shown in the decks. */
public final class Ids {

    private Ids() {
    }

    /** e.g. PAY-7f3a... (used by the Gateway in Module A; handy for stub testing). */
    public static String paymentId() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** UPI / NEFT settlement reference, e.g. UTR829107654321. */
    public static String utr() {
        long n = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
        return "UTR" + n;
    }

    /** Card 3DS challenge handle, e.g. 3DS-9a1b2c3d4e5f. */
    public static String challengeId() {
        return "3DS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
