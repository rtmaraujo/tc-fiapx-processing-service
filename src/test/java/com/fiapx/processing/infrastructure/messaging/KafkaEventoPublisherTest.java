package com.fiapx.processing.infrastructure.messaging;

import com.fiapx.processing.application.event.VideoProcessadoEvent;
import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventoPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaEventoPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaEventoPublisher(kafkaTemplate,
            JsonMapper.builder().findAndAddModules().build());
    }

    @AfterEach
    void limparInterrupcao() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("publica evento JSON no topico informado")
    void devePublicar() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publicar("fiapx.video.processado", "video-1",
            new VideoProcessadoEvent("evt", UUID.randomUUID(), "frames/a.zip", 2, LocalDateTime.now()));

        verify(kafkaTemplate).send(eq("fiapx.video.processado"), eq("video-1"),
            org.mockito.ArgumentMatchers.argThat(payload ->
                payload.contains("\"framesZipKey\":\"frames/a.zip\"") && payload.contains("\"totalFrames\":2")));
    }

    @Test
    @DisplayName("encapsula falha de publicacao")
    void deveEncapsularFalha() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka off")));

        assertThatThrownBy(() -> publisher.publicar("topico", "chave", "evento"))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("topico");
    }

    @Test
    @DisplayName("interrompe a thread em publicacao interrompida")
    void deveTratarInterrupcao() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(new CompletableFuture<>() {
                @Override
                public SendResult<String, String> get(long timeout, TimeUnit unit) throws InterruptedException {
                    throw new InterruptedException("interrompido");
                }
            });

        assertThatThrownBy(() -> publisher.publicar("topico", "chave", "evento"))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("interrompida");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
