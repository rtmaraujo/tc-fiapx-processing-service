package com.fiapx.processing.infrastructure.messaging;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.EventoPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventoPublisher implements EventoPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventoPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(String topico, String chave, Object evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topico, chave, payload).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FalhaProcessamentoException("Publicacao interrompida no topico " + topico, ex);
        } catch (Exception ex) {
            throw new FalhaProcessamentoException("Falha ao publicar evento no topico " + topico, ex);
        }
    }
}
