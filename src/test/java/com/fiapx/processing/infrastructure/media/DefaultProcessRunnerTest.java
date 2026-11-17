package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultProcessRunnerTest {

    private final DefaultProcessRunner runner = new DefaultProcessRunner();

    @TempDir
    private Path tempDir;

    @AfterEach
    void limparInterrupcao() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("executa processo com sucesso e captura a saida")
    void deveExecutarComSucesso() {
        Path log = tempDir.resolve("java.log");

        ProcessResult resultado = runner.executar(
            List.of(javaBin(), "-version"), Duration.ofSeconds(30), log);

        assertThat(resultado.timeout()).isFalse();
        assertThat(resultado.exitCode()).isZero();
        assertThat(Files.exists(log)).isTrue();
    }

    @Test
    @DisplayName("encerra processo que excede o timeout")
    void deveEncerrarPorTimeout() {
        List<String> comando = List.of(javaBin(), "-cp", System.getProperty("java.class.path"),
            "com.fiapx.processing.support.Sleeper");

        ProcessResult resultado = runner.executar(comando, Duration.ofMillis(300), tempDir.resolve("sleep.log"));

        assertThat(resultado.timeout()).isTrue();
        assertThat(resultado.exitCode()).isEqualTo(-1);
    }

    @Test
    @DisplayName("aceita caminho de log relativo sem diretorio pai")
    void deveAceitarLogRelativo() throws Exception {
        Path log = Path.of("fiapx-runner-relative.log");
        try {
            ProcessResult resultado = runner.executar(
                List.of(javaBin(), "-version"), Duration.ofSeconds(30), log);

            assertThat(resultado.exitCode()).isZero();
            assertThat(Files.exists(log)).isTrue();
        } finally {
            Files.deleteIfExists(log);
        }
    }

    @Test
    @DisplayName("trunca a saida do processo em 2000 caracteres")
    void deveTruncarSaidaLonga() {
        List<String> comando = List.of(javaBin(), "-cp", System.getProperty("java.class.path"),
            "com.fiapx.processing.support.Verbose");

        ProcessResult resultado = runner.executar(comando, Duration.ofSeconds(30), tempDir.resolve("verbose.log"));

        assertThat(resultado.exitCode()).isZero();
        assertThat(resultado.saida()).hasSize(2_000);
        assertThat(resultado.saida()).isEqualTo("v".repeat(2_000));
    }

    @Test
    @DisplayName("falha quando o executavel nao existe")
    void deveFalharExecutavelInexistente() {
        assertThatThrownBy(() -> runner.executar(
            List.of("binario-que-nao-existe-fiapx"), Duration.ofSeconds(5), tempDir.resolve("x.log")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("executar o processo");
    }

    @Test
    @DisplayName("propaga interrupcao da thread")
    void devePropagarInterrupcao() {
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> runner.executar(
            List.of(javaBin(), "-version"), Duration.ofSeconds(30), tempDir.resolve("y.log")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("interrompida");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    private String javaBin() {
        String executavel = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executavel).toString();
    }
}
