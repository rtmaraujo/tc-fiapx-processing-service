package com.fiapx.processing.application.impl;

import com.fiapx.processing.application.event.VideoFalhaEvent;
import com.fiapx.processing.application.event.VideoProcessadoEvent;
import com.fiapx.processing.application.event.VideoSolicitadoEvent;
import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.EventoPublisher;
import com.fiapx.processing.application.port.FrameExtractorPort;
import com.fiapx.processing.application.port.StoragePort;
import com.fiapx.processing.config.KafkaTopicsProperties;
import com.fiapx.processing.domain.Processamento;
import com.fiapx.processing.domain.ProcessamentoStatus;
import com.fiapx.processing.infrastructure.media.ZipService;
import com.fiapx.processing.infrastructure.persistence.ProcessamentoEntity;
import com.fiapx.processing.infrastructure.repository.ProcessamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessamentoApplicationServiceImplTest {

    @Mock
    private ProcessamentoRepository processamentoRepository;
    @Mock
    private StoragePort storagePort;
    @Mock
    private FrameExtractorPort frameExtractor;
    @Mock
    private EventoPublisher eventoPublisher;

    private ProcessamentoApplicationServiceImpl service;
    private KafkaTopicsProperties topics;

    @BeforeEach
    void setUp() {
        topics = new KafkaTopicsProperties();
        service = new ProcessamentoApplicationServiceImpl(processamentoRepository, storagePort,
            frameExtractor, new ZipService(), eventoPublisher, topics);
        when(processamentoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("processa video, salva ZIP e publica evento de conclusao")
    void deveProcessarComSucesso() {
        UUID videoId = UUID.randomUUID();
        doAnswer(invocacao -> {
            Path destino = invocacao.getArgument(1);
            Files.writeString(destino, "video-bytes");
            return null;
        }).when(storagePort).baixarParaArquivo(eq("videos/a.mp4"), any(Path.class));
        doAnswer(invocacao -> {
            Path destino = invocacao.getArgument(1);
            Files.createDirectories(destino);
            return List.of(
                Files.writeString(destino.resolve("frame_00001.jpg"), "1"),
                Files.writeString(destino.resolve("frame_00002.jpg"), "2"));
        }).when(frameExtractor).extrair(any(Path.class), any(Path.class));

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<ProcessamentoEntity> entityCaptor = ArgumentCaptor.forClass(ProcessamentoEntity.class);
        verify(processamentoRepository, org.mockito.Mockito.atLeast(2)).save(entityCaptor.capture());
        ProcessamentoEntity ultimo = entityCaptor.getValue();
        assertThat(ultimo.getStatus()).isEqualTo(ProcessamentoStatus.CONCLUIDO);
        assertThat(ultimo.getFramesZipKey()).isEqualTo("frames/" + videoId + "/frames.zip");
        assertThat(ultimo.getTotalFrames()).isEqualTo(2);

        verify(storagePort).salvar(eq("frames/" + videoId + "/frames.zip"), any(InputStream.class),
            anyLong(), eq("application/zip"));
        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getProcessado()), eq(videoId.toString()),
            eventoCaptor.capture());
        VideoProcessadoEvent publicado = (VideoProcessadoEvent) eventoCaptor.getValue();
        assertThat(publicado.videoId()).isEqualTo(videoId);
        assertThat(publicado.totalFrames()).isEqualTo(2);
    }

    @Test
    @DisplayName("reaproveita registro existente do processamento")
    void deveReaproveitarRegistroExistente() {
        UUID videoId = UUID.randomUUID();
        ProcessamentoEntity existente = new ProcessamentoEntity(
            Processamento.recebido(videoId, "evt-antigo"));
        when(processamentoRepository.findById(videoId)).thenReturn(Optional.of(existente));
        stubProcessamentoOk();

        service.processar(evento(videoId, "aula.mp4"));

        verify(processamentoRepository, org.mockito.Mockito.atLeastOnce()).save(existente);
        assertThat(existente.getEventId()).isEqualTo("evt-antigo");
        assertThat(existente.getStatus()).isEqualTo(ProcessamentoStatus.CONCLUIDO);
    }

    @Test
    @DisplayName("publica falha quando a extracao falha")
    void devePublicarFalhaDeExtracao() {
        UUID videoId = UUID.randomUUID();
        stubDownloadOk();
        when(frameExtractor.extrair(any(Path.class), any(Path.class)))
            .thenThrow(new FalhaProcessamentoException("codec invalido"));

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<ProcessamentoEntity> entityCaptor = ArgumentCaptor.forClass(ProcessamentoEntity.class);
        verify(processamentoRepository, org.mockito.Mockito.atLeast(2)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(ProcessamentoStatus.FALHA);
        assertThat(entityCaptor.getValue().getErroMensagem()).isEqualTo("codec invalido");

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), eventoCaptor.capture());
        VideoFalhaEvent falha = (VideoFalhaEvent) eventoCaptor.getValue();
        assertThat(falha.motivo()).isEqualTo("codec invalido");
        verify(storagePort, never()).salvar(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("usa nome da excecao quando a falha nao tem mensagem")
    void deveUsarNomeDaExcecaoComoMotivo() {
        UUID videoId = UUID.randomUUID();
        stubDownloadOk();
        when(frameExtractor.extrair(any(Path.class), any(Path.class)))
            .thenThrow(new IllegalStateException());

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), eventoCaptor.capture());
        assertThat(((VideoFalhaEvent) eventoCaptor.getValue()).motivo()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("usa nome da excecao quando a falha de processamento nao tem mensagem")
    void deveUsarNomeDaExcecaoDeFalhaSemMensagem() {
        UUID videoId = UUID.randomUUID();
        stubDownloadOk();
        when(frameExtractor.extrair(any(Path.class), any(Path.class)))
            .thenThrow(new FalhaProcessamentoException(null));

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), eventoCaptor.capture());
        assertThat(((VideoFalhaEvent) eventoCaptor.getValue()).motivo()).isEqualTo("FalhaProcessamentoException");
    }

    @Test
    @DisplayName("usa mensagem de excecao comum quando preenchida")
    void deveUsarMensagemDeExcecaoComum() {
        UUID videoId = UUID.randomUUID();
        stubDownloadOk();
        when(frameExtractor.extrair(any(Path.class), any(Path.class)))
            .thenThrow(new IllegalStateException("disco cheio"));

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), eventoCaptor.capture());
        assertThat(((VideoFalhaEvent) eventoCaptor.getValue()).motivo()).isEqualTo("disco cheio");
    }

    @Test
    @DisplayName("usa nome da excecao quando a mensagem e branca")
    void deveUsarNomeDaExcecaoComMensagemBranca() {
        UUID videoId = UUID.randomUUID();
        stubDownloadOk();
        when(frameExtractor.extrair(any(Path.class), any(Path.class)))
            .thenThrow(new IllegalStateException("   "));

        service.processar(evento(videoId, "aula.mp4"));

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), eventoCaptor.capture());
        assertThat(((VideoFalhaEvent) eventoCaptor.getValue()).motivo()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("publica falha quando o download do video falha")
    void devePublicarFalhaDeDownload() {
        UUID videoId = UUID.randomUUID();
        doThrow(new FalhaProcessamentoException("minio off"))
            .when(storagePort).baixarParaArquivo(anyString(), any(Path.class));

        service.processar(evento(videoId, "aula.mp4"));

        verify(eventoPublisher).publicar(eq(topics.getFalha()), eq(videoId.toString()), any());
    }

    @Test
    @DisplayName("propaga falha ao publicar evento de falha")
    void devePropagarFalhaDePublicacao() {
        UUID videoId = UUID.randomUUID();
        doThrow(new FalhaProcessamentoException("minio off"))
            .when(storagePort).baixarParaArquivo(anyString(), any(Path.class));
        doThrow(new FalhaProcessamentoException("kafka off"))
            .when(eventoPublisher).publicar(anyString(), anyString(), any());

        assertThatThrownBy(() -> service.processar(evento(videoId, "aula.mp4")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("kafka off");
    }

    @Test
    @DisplayName("usa extensao do nome do arquivo e cai para bin quando invalida")
    void deveMontarNomeDeEntrada() {
        UUID videoId = UUID.randomUUID();
        stubProcessamentoOk();

        service.processar(evento(videoId, "AULA.MP4"));
        service.processar(new VideoSolicitadoEvent("evt-2", videoId, null, "sem-extensao",
            "video/mp4", 10L, "videos/b", LocalDateTime.now()));
        service.processar(new VideoSolicitadoEvent("evt-3", videoId, null, null,
            "video/mp4", 10L, "videos/c", LocalDateTime.now()));

        ArgumentCaptor<Path> destinoCaptor = ArgumentCaptor.forClass(Path.class);
        verify(storagePort, org.mockito.Mockito.times(3))
            .baixarParaArquivo(anyString(), destinoCaptor.capture());
        assertThat(destinoCaptor.getAllValues())
            .extracting(path -> path.getFileName().toString())
            .containsExactly("input.mp4", "input.bin", "input.bin");
    }

    @Test
    @DisplayName("trata nome de arquivo terminando em ponto")
    void deveTratarNomeComPontoFinal() {
        UUID videoId = UUID.randomUUID();
        stubProcessamentoOk();

        service.processar(evento(videoId, "video."));

        ArgumentCaptor<Path> destinoCaptor = ArgumentCaptor.forClass(Path.class);
        verify(storagePort).baixarParaArquivo(anyString(), destinoCaptor.capture());
        assertThat(destinoCaptor.getValue().getFileName().toString()).isEqualTo("input.bin");
    }

    @Test
    @DisplayName("trata extensao com caracteres invalidos")
    void deveTratarExtensaoInvalida() {
        UUID videoId = UUID.randomUUID();
        stubProcessamentoOk();

        service.processar(evento(videoId, "video.extensao-longa"));

        ArgumentCaptor<Path> destinoCaptor = ArgumentCaptor.forClass(Path.class);
        verify(storagePort).baixarParaArquivo(anyString(), destinoCaptor.capture());
        assertThat(destinoCaptor.getValue().getFileName().toString()).isEqualTo("input.bin");
    }

    private void stubDownloadOk() {
        doAnswer(invocacao -> {
            Path destino = invocacao.getArgument(1);
            Files.writeString(destino, "video");
            return null;
        }).when(storagePort).baixarParaArquivo(anyString(), any(Path.class));
    }

    private void stubProcessamentoOk() {
        stubDownloadOk();
        doAnswer(invocacao -> {
            Path destino = invocacao.getArgument(1);
            Files.createDirectories(destino);
            return List.of(Files.writeString(destino.resolve("frame_00001.jpg"), "1"));
        }).when(frameExtractor).extrair(any(Path.class), any(Path.class));
    }

    private VideoSolicitadoEvent evento(UUID videoId, String nomeArquivo) {
        return new VideoSolicitadoEvent("evt-1", videoId, UUID.randomUUID(), nomeArquivo,
            "video/mp4", 100L, "videos/a.mp4", LocalDateTime.now());
    }
}
