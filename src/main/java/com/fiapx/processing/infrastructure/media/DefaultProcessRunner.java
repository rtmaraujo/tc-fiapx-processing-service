package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultProcessRunner implements ProcessRunner {

    @Override
    public ProcessResult executar(List<String> comando, Duration timeout, Path arquivoLog) {
        Process processo = null;
        try {
            if (arquivoLog.getParent() != null) {
                Files.createDirectories(arquivoLog.getParent());
            }
            ProcessBuilder builder = new ProcessBuilder(comando)
                .redirectErrorStream(true)
                .redirectOutput(arquivoLog.toFile());
            processo = builder.start();
            boolean terminou = processo.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!terminou) {
                processo.destroyForcibly();
                processo.waitFor(5, TimeUnit.SECONDS);
                return new ProcessResult(-1, true, lerLog(arquivoLog));
            }
            return new ProcessResult(processo.exitValue(), false, lerLog(arquivoLog));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            processo.destroyForcibly();
            throw new FalhaProcessamentoException("Execucao do processo foi interrompida", ex);
        } catch (IOException ex) {
            throw new FalhaProcessamentoException("Nao foi possivel executar o processo", ex);
        }
    }

    private String lerLog(Path arquivoLog) {
        try {
            String conteudo = Files.readString(arquivoLog, StandardCharsets.UTF_8);
            return conteudo.length() > 2_000 ? conteudo.substring(conteudo.length() - 2_000) : conteudo;
        } catch (IOException ex) {
            return "";
        }
    }
}
