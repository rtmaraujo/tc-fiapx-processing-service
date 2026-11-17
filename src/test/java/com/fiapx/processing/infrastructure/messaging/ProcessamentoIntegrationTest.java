package com.fiapx.processing.infrastructure.messaging;

import com.fiapx.processing.application.event.VideoFalhaEvent;
import com.fiapx.processing.application.event.VideoProcessadoEvent;
import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.EventoPublisher;
import com.fiapx.processing.application.port.FrameExtractorPort;
import com.fiapx.processing.application.port.StoragePort;
import com.fiapx.processing.domain.ProcessamentoStatus;
import com.fiapx.processing.infrastructure.repository.EventoProcessadoRepository;
import com.fiapx.processing.infrastructure.repository.ProcessamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProcessamentoIntegrationTest {

    @Autowired
    private VideoSolicitadoConsumer consumer;
    @Autowired
    private ProcessamentoRepository processamentoRepository;
    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;
    @Autowired
    private FakeStorage storage;
    @Autowired
    private FakeExtractor extractor;
    @Autowired
    private FakePublisher publisher;

    @BeforeEach
    void setUp() {
        processamentoRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
        storage.limpar();
        publisher.limpar();
        extractor.setFalhar(false);
    }

    @Test
    @DisplayName("processa evento de ponta a ponta e publica conclusao")
    void deveProcessarPontaAPonta() {
        UUID videoId = UUID.randomUUID();

        consumer.consumir(payload("evt-1", videoId, "aula.mp4"));

        var processamento = processamentoRepository.findById(videoId).orElseThrow();
        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.CONCLUIDO);
        assertThat(processamento.getFramesZipKey()).isEqualTo("frames/" + videoId + "/frames.zip");
        assertThat(processamento.getTotalFrames()).isEqualTo(3);
        assertThat(storage.conteudo("frames/" + videoId + "/frames.zip")).isNotEmpty();

        assertThat(publisher.eventos()).hasSize(1);
        assertThat(publisher.eventos().getFirst()).isInstanceOf(VideoProcessadoEvent.class);
        VideoProcessadoEvent event = (VideoProcessadoEvent) publisher.eventos().getFirst();
        assertThat(event.videoId()).isEqualTo(videoId);
        assertThat(event.totalFrames()).isEqualTo(3);
        assertThat(eventoProcessadoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("nao reprocessa evento duplicado")
    void deveIgnorarDuplicado() {
        UUID videoId = UUID.randomUUID();
        String payload = payload("evt-1", videoId, "aula.mp4");

        consumer.consumir(payload);
        consumer.consumir(payload);

        assertThat(publisher.eventos()).hasSize(1);
        assertThat(eventoProcessadoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("publica falha quando a extracao falha")
    void devePublicarFalha() {
        UUID videoId = UUID.randomUUID();
        extractor.setFalhar(true);

        consumer.consumir(payload("evt-falha", videoId, "aula.mp4"));

        var processamento = processamentoRepository.findById(videoId).orElseThrow();
        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.FALHA);
        assertThat(processamento.getErroMensagem()).contains("falha simulada");
        assertThat(publisher.eventos()).hasSize(1);
        assertThat(publisher.eventos().getFirst()).isInstanceOf(VideoFalhaEvent.class);
    }

    private String payload(String eventId, UUID videoId, String nomeArquivo) {
        return """
            {"eventId":"%s","videoId":"%s","usuarioId":"%s","nomeArquivo":"%s",
             "contentType":"video/mp4","tamanhoBytes":100,"storageKey":"videos/%s",
             "solicitadoEm":"2026-01-01T10:00:00"}
            """.formatted(eventId, videoId, UUID.randomUUID(), nomeArquivo, nomeArquivo);
    }

    @TestConfiguration
    static class FakesConfig {

        @Bean
        @Primary
        FakeStorage fakeStorage() {
            return new FakeStorage();
        }

        @Bean
        @Primary
        FakeExtractor fakeExtractor() {
            return new FakeExtractor();
        }

        @Bean
        @Primary
        FakePublisher fakePublisher() {
            return new FakePublisher();
        }
    }

    static class FakeStorage implements StoragePort {

        private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

        @Override
        public void baixarParaArquivo(String chave, Path destino) {
            try {
                Files.writeString(destino, "video-fake");
            } catch (Exception ex) {
                throw new FalhaProcessamentoException("erro no download fake", ex);
            }
        }

        @Override
        public void salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType) {
            try {
                objetos.put(chave, conteudo.readAllBytes());
            } catch (Exception ex) {
                throw new FalhaProcessamentoException("erro no upload fake", ex);
            }
        }

        byte[] conteudo(String chave) {
            return objetos.getOrDefault(chave, new byte[0]);
        }

        void limpar() {
            objetos.clear();
        }
    }

    static class FakeExtractor implements FrameExtractorPort {

        private boolean falhar;

        @Override
        public List<Path> extrair(Path video, Path diretorioDestino) {
            if (falhar) {
                throw new FalhaProcessamentoException("falha simulada");
            }
            try {
                Files.createDirectories(diretorioDestino);
                return List.of(
                    Files.writeString(diretorioDestino.resolve("frame_00001.jpg"), "1"),
                    Files.writeString(diretorioDestino.resolve("frame_00002.jpg"), "2"),
                    Files.writeString(diretorioDestino.resolve("frame_00003.jpg"), "3"));
            } catch (Exception ex) {
                throw new FalhaProcessamentoException("erro no extrator fake", ex);
            }
        }

        void setFalhar(boolean falhar) {
            this.falhar = falhar;
        }
    }

    static class FakePublisher implements EventoPublisher {

        private final List<Object> eventos = new ArrayList<>();
        private final List<String> topicos = new ArrayList<>();

        @Override
        public void publicar(String topico, String chave, Object evento) {
            topicos.add(topico);
            eventos.add(evento);
        }

        List<Object> eventos() {
            return eventos;
        }

        List<String> topicos() {
            return topicos;
        }

        void limpar() {
            eventos.clear();
            topicos.clear();
        }
    }
}
