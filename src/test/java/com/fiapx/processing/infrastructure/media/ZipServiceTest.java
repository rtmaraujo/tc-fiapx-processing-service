package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("gera ZIP com todos os frames")
    void deveGerarZip() throws Exception {
        Path frame1 = Files.writeString(tempDir.resolve("frame_00001.jpg"), "a");
        Path frame2 = Files.writeString(tempDir.resolve("frame_00002.jpg"), "b");
        Path zip = tempDir.resolve("frames.zip");

        new ZipService().zipar(List.of(frame1, frame2), zip);

        assertThat(Files.exists(zip)).isTrue();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            assertThat(zipFile.size()).isEqualTo(2);
            assertThat(zipFile.getEntry("frame_00001.jpg")).isNotNull();
            assertThat(zipFile.getEntry("frame_00002.jpg")).isNotNull();
        }
    }

    @Test
    @DisplayName("falha quando o destino do ZIP e invalido")
    void deveFalharDestinoInvalido() throws Exception {
        Path frame = Files.writeString(tempDir.resolve("frame_00001.jpg"), "a");
        Path destinoInvalido = tempDir.resolve("diretorio-inexistente").resolve("frames.zip");

        assertThatThrownBy(() -> new ZipService().zipar(List.of(frame), destinoInvalido))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("ZIP");
    }
}
