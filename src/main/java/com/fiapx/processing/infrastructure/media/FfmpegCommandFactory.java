package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.config.FfmpegProperties;

import java.nio.file.Path;
import java.util.List;

public final class FfmpegCommandFactory {

    private FfmpegCommandFactory() {
    }

    /**
     * Extrai 1 frame por segundo (fps=1), como no projeto original em Go.
     */
    public static List<String> construir(FfmpegProperties properties, Path entrada, Path saida) {
        return List.of(
            properties.getBinary(),
            "-hide_banner",
            "-loglevel", properties.getLogLevel(),
            "-y",
            "-i", entrada.toString(),
            "-vf", "fps=" + properties.getFps(),
            "-q:v", String.valueOf(properties.getQualidade()),
            saida.toString()
        );
    }
}
