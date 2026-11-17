package com.fiapx.processing.support;

/**
 * Processo auxiliar usado no teste de timeout do DefaultProcessRunner.
 */
public final class Sleeper {

    private Sleeper() {
    }

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(10_000L);
    }
}
