package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.FrameExtractorPort;
import com.fiapx.processing.config.FfmpegProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class FfmpegFrameExtractor implements FrameExtractorPort {

    private final ProcessRunner processRunner;
    private final FfmpegProperties properties;

    public FfmpegFrameExtractor(ProcessRunner processRunner, FfmpegProperties properties) {
        this.processRunner = processRunner;
        this.properties = properties;
    }

    @Override
    public List<Path> extrair(Path video, Path diretorioDestino) {
        try {
            Files.createDirectories(diretorioDestino);
        } catch (IOException ex) {
            throw new FalhaProcessamentoException("Nao foi possivel criar o diretorio de frames", ex);
        }

        Path saida = diretorioDestino.resolve("frame_%05d.jpg");
        List<String> comando = FfmpegCommandFactory.construir(properties, video, saida);
        ProcessResult resultado = processRunner.executar(comando,
            Duration.ofSeconds(properties.getTimeoutSeconds()),
            diretorioDestino.resolve("ffmpeg.log"));

        if (resultado.timeout()) {
            throw new FalhaProcessamentoException(
                "ffmpeg excedeu o tempo limite de " + properties.getTimeoutSeconds() + "s");
        }
        if (resultado.exitCode() != 0) {
            throw new FalhaProcessamentoException("ffmpeg retornou codigo " + resultado.exitCode()
                + ": " + resumir(resultado.saida()));
        }

        List<Path> frames;
        try (Stream<Path> arquivos = Files.list(diretorioDestino)) {
            frames = arquivos
                .filter(Files::isRegularFile)
                .filter(this::ehFrame)
                .sorted()
                .toList();
        } catch (IOException ex) {
            throw new FalhaProcessamentoException("Nao foi possivel listar os frames extraidos", ex);
        }

        if (frames.isEmpty()) {
            throw new FalhaProcessamentoException("Nenhum frame foi extraido do video");
        }
        return frames;
    }

    private boolean ehFrame(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!nome.startsWith("frame_")) {
            return false;
        }
        return nome.endsWith(".jpg");
    }

    private String resumir(String saida) {
        if (saida == null || saida.isBlank()) {
            return "sem detalhes";
        }
        String limpa = saida.trim().replaceAll("\\s+", " ");
        return limpa.length() > 300 ? limpa.substring(0, 300) : limpa;
    }
}
