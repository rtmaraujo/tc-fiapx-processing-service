package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.config.FfmpegProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FfmpegFrameExtractorTest {

    @Mock
    private ProcessRunner processRunner;

    private FfmpegFrameExtractor extractor;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        FfmpegProperties properties = new FfmpegProperties();
        properties.setTimeoutSeconds(30);
        extractor = new FfmpegFrameExtractor(processRunner, properties);
    }

    @Test
    @DisplayName("extrai e ordena os frames gerados")
    void deveExtrairFrames() throws Exception {
        Path destino = tempDir.resolve("frames");
        when(processRunner.executar(anyList(), any(), any())).thenAnswer(invocacao -> {
            Path padrao = Path.of(invocacao.<List<String>>getArgument(0).getLast());
            Files.writeString(padrao.resolveSibling("frame_00002.jpg"), "b");
            Files.writeString(padrao.resolveSibling("frame_00001.jpg"), "a");
            Files.writeString(padrao.resolveSibling("ffmpeg.log"), "log");
            return new ProcessResult(0, false, "");
        });

        List<Path> frames = extractor.extrair(tempDir.resolve("input.mp4"), destino);

        assertThat(frames).hasSize(2);
        assertThat(frames.getFirst().getFileName().toString()).isEqualTo("frame_00001.jpg");
        assertThat(frames.getLast().getFileName().toString()).isEqualTo("frame_00002.jpg");
    }

    @Test
    @DisplayName("falha quando ffmpeg retorna codigo diferente de zero")
    void deveFalharComCodigoDeErro() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(1, false, "Invalid data found when processing input"));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("codigo 1")
            .hasMessageContaining("Invalid data");
    }

    @Test
    @DisplayName("falha quando ffmpeg excede o tempo limite")
    void deveFalharPorTimeout() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(-1, true, ""));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("tempo limite");
    }

    @Test
    @DisplayName("trata saida nula do ffmpeg")
    void deveTratarSaidaNula() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(2, false, null));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("sem detalhes");
    }

    @Test
    @DisplayName("trunca a saida do ffmpeg em 300 caracteres")
    void deveTruncarSaidaLonga() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(2, false, "A".repeat(500)));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("A".repeat(300))
            .hasMessageNotContaining("A".repeat(301));
    }

    @Test
    @DisplayName("falha quando nenhum frame e gerado")
    void deveFalharSemFrames() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(0, false, ""));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("Nenhum frame");
    }

    @Test
    @DisplayName("trata saida vazia do ffmpeg")
    void deveTratarSaidaVazia() {
        when(processRunner.executar(anyList(), any(), any()))
            .thenReturn(new ProcessResult(2, false, "  "));

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), tempDir.resolve("frames")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("sem detalhes");
    }

    @Test
    @DisplayName("falha quando o diretorio de destino nao pode ser criado")
    void deveFalharAoPrepararDiretorio() throws Exception {
        Path arquivo = tempDir.resolve("arquivo");
        Files.writeString(arquivo, "x");

        assertThatThrownBy(() -> extractor.extrair(tempDir.resolve("input.mp4"), arquivo))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("diretorio de frames");
    }
}
