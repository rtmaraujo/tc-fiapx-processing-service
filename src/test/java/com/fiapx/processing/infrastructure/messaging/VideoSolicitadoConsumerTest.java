package com.fiapx.processing.infrastructure.messaging;

import com.fiapx.processing.application.ProcessamentoApplicationService;
import com.fiapx.processing.config.KafkaTopicsProperties;
import com.fiapx.processing.infrastructure.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoSolicitadoConsumerTest {

    @Mock
    private ProcessamentoApplicationService processamentoService;
    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    private VideoSolicitadoConsumer consumer;
    private KafkaTopicsProperties topics;

    @BeforeEach
    void setUp() {
        topics = new KafkaTopicsProperties();
        consumer = new VideoSolicitadoConsumer(processamentoService, eventoProcessadoRepository,
            JsonMapper.builder().findAndAddModules().build(), topics);
    }

    @Test
    @DisplayName("processa evento novo e registra idempotencia")
    void deveProcessarEventoNovo() {
        UUID videoId = UUID.randomUUID();
        when(eventoProcessadoRepository.existsById("evt-1")).thenReturn(false);

        consumer.consumir(payload("evt-1", videoId));

        verify(processamentoService).processar(org.mockito.ArgumentMatchers.argThat(
            evento -> evento.videoId().equals(videoId) && "evt-1".equals(evento.eventId())));
        ArgumentCaptor<com.fiapx.processing.infrastructure.persistence.EventoProcessadoEntity> captor =
            ArgumentCaptor.forClass(com.fiapx.processing.infrastructure.persistence.EventoProcessadoEntity.class);
        verify(eventoProcessadoRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(captor.getValue().getTopico()).isEqualTo(topics.getSolicitado());
    }

    @Test
    @DisplayName("ignora evento ja processado")
    void deveIgnorarEventoDuplicado() {
        when(eventoProcessadoRepository.existsById("evt-1")).thenReturn(true);

        consumer.consumir(payload("evt-1", UUID.randomUUID()));

        verifyNoInteractions(processamentoService);
        verify(eventoProcessadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita evento sem eventId")
    void deveRejeitarEventoSemId() {
        assertThatThrownBy(() -> consumer.consumir(payload(null, UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eventId");
    }

    @Test
    @DisplayName("rejeita evento com eventId em branco")
    void deveRejeitarEventoComIdBranco() {
        assertThatThrownBy(() -> consumer.consumir(payload(" ", UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private String payload(String eventId, UUID videoId) {
        return """
            {"eventId":%s,"videoId":"%s","usuarioId":"%s","nomeArquivo":"a.mp4",
             "contentType":"video/mp4","tamanhoBytes":10,"storageKey":"videos/a.mp4",
             "solicitadoEm":"2026-01-01T10:00:00"}
            """.formatted(eventId == null ? "null" : "\"" + eventId + "\"", videoId, UUID.randomUUID());
    }
}
