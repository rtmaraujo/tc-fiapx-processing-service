package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.config.FfmpegProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegCommandFactoryTest {

    @Test
    @DisplayName("monta comando ffmpeg com fps e qualidade configurados")
    void deveMontarComando() {
        FfmpegProperties properties = new FfmpegProperties();
        properties.setBinary("ffmpeg-custom");
        properties.setFps(2);
        properties.setQualidade(3);
        properties.setLogLevel("warning");

        List<String> comando = FfmpegCommandFactory.construir(properties,
            Path.of("input.mp4"), Path.of("frame_%05d.jpg"));

        assertThat(comando).containsSubsequence("ffmpeg-custom", "-loglevel", "warning", "-i", "input.mp4",
            "-vf", "fps=2", "-q:v", "3");
        assertThat(comando.getLast()).isEqualTo("frame_%05d.jpg");
    }
}
