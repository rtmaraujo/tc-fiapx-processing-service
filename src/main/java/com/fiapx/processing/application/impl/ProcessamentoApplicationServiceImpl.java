package com.fiapx.processing.application.impl;

import com.fiapx.processing.application.ProcessamentoApplicationService;
import com.fiapx.processing.application.event.VideoFalhaEvent;
import com.fiapx.processing.application.event.VideoProcessadoEvent;
import com.fiapx.processing.application.event.VideoSolicitadoEvent;
import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.EventoPublisher;
import com.fiapx.processing.application.port.FrameExtractorPort;
import com.fiapx.processing.application.port.StoragePort;
import com.fiapx.processing.config.KafkaTopicsProperties;
import com.fiapx.processing.domain.Processamento;
import com.fiapx.processing.infrastructure.media.ZipService;
import com.fiapx.processing.infrastructure.persistence.ProcessamentoEntity;
import com.fiapx.processing.infrastructure.repository.ProcessamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ProcessamentoApplicationServiceImpl implements ProcessamentoApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoApplicationServiceImpl.class);
    private static final String PREFIXO_FRAMES = "frames/";

    private final ProcessamentoRepository processamentoRepository;
    private final StoragePort storagePort;
    private final FrameExtractorPort frameExtractor;
    private final ZipService zipService;
    private final EventoPublisher eventoPublisher;
    private final KafkaTopicsProperties topics;

    public ProcessamentoApplicationServiceImpl(ProcessamentoRepository processamentoRepository,
                                               StoragePort storagePort,
                                               FrameExtractorPort frameExtractor,
                                               ZipService zipService,
                                               EventoPublisher eventoPublisher,
                                               KafkaTopicsProperties topics) {
        this.processamentoRepository = processamentoRepository;
        this.storagePort = storagePort;
        this.frameExtractor = frameExtractor;
        this.zipService = zipService;
        this.eventoPublisher = eventoPublisher;
        this.topics = topics;
    }

    @Override
    public void processar(VideoSolicitadoEvent evento) {
        ProcessamentoEntity entity = processamentoRepository.findById(evento.videoId())
            .orElseGet(() -> new ProcessamentoEntity(
                Processamento.recebido(evento.videoId(), evento.eventId())));
        Processamento processamento = entity.toDomain();

        Path diretorioTemporario = Path.of(System.getProperty("java.io.tmpdir"),
            "fiapx-processing-" + UUID.randomUUID());
        try {
            Files.createDirectories(diretorioTemporario);
            Path video = diretorioTemporario.resolve("input." + extensao(evento.nomeArquivo()));
            storagePort.baixarParaArquivo(evento.storageKey(), video);

            processamento.marcarProcessando();
            salvar(entity, processamento);

            Path diretorioFrames = diretorioTemporario.resolve("frames");
            List<Path> frames = frameExtractor.extrair(video, diretorioFrames);

            Path zip = diretorioTemporario.resolve("frames.zip");
            zipService.zipar(frames, zip);

            String chaveZip = PREFIXO_FRAMES + evento.videoId() + "/frames.zip";
            try (InputStream conteudo = Files.newInputStream(zip)) {
                storagePort.salvar(chaveZip, conteudo, Files.size(zip), "application/zip");
            }

            processamento.concluir(chaveZip, frames.size());
            salvar(entity, processamento);

            eventoPublisher.publicar(topics.getProcessado(), evento.videoId().toString(),
                new VideoProcessadoEvent(UUID.randomUUID().toString(), evento.videoId(), chaveZip,
                    frames.size(), LocalDateTime.now()));
            log.info("Video {} processado com {} frames", evento.videoId(), frames.size());
        } catch (Exception ex) {
            String motivo = motivo(ex);
            processamento.falhar(motivo);
            salvar(entity, processamento);
            eventoPublisher.publicar(topics.getFalha(), evento.videoId().toString(),
                new VideoFalhaEvent(UUID.randomUUID().toString(), evento.videoId(), motivo,
                    LocalDateTime.now()));
            log.error("Falha ao processar video {}: {}", evento.videoId(), motivo, ex);
        } finally {
            limpar(diretorioTemporario);
        }
    }

    private void salvar(ProcessamentoEntity entity, Processamento processamento) {
        entity.aplicar(processamento);
        processamentoRepository.save(entity);
    }

    private String motivo(Exception ex) {
        if (ex instanceof FalhaProcessamentoException && ex.getMessage() != null) {
            return ex.getMessage();
        }
        String mensagem = ex.getMessage();
        return mensagem == null || mensagem.isBlank()
            ? ex.getClass().getSimpleName()
            : mensagem;
    }

    private String extensao(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "bin";
        }
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeArquivo.length() - 1) {
            return "bin";
        }
        String candidata = nomeArquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
        return candidata.matches("[a-z0-9]{1,8}") ? candidata : "bin";
    }

    private void limpar(Path diretorio) {
        try (Stream<Path> caminhos = Files.walk(diretorio)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> {
                try {
                    Files.deleteIfExists(caminho);
                } catch (Exception ex) {
                    log.debug("Nao foi possivel remover {}", caminho, ex);
                }
            });
        } catch (Exception ex) {
            log.debug("Nao foi possivel limpar o diretorio temporario {}", diretorio, ex);
        }
    }
}
