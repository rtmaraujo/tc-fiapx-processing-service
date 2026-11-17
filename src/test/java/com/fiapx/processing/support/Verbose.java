package com.fiapx.processing.support;

/**
 * Processo auxiliar que gera uma saida longa para exercitar o truncamento do log.
 */
public final class Verbose {

    private Verbose() {
    }

    public static void main(String[] args) {
        System.out.print("v".repeat(2_500));
    }
}
