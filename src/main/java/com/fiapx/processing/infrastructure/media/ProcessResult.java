package com.fiapx.processing.infrastructure.media;

public record ProcessResult(int exitCode, boolean timeout, String saida) {
}
