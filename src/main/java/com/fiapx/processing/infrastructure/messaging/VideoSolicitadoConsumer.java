package com.fiapx.processing.infrastructure.messaging;

import com.fiapx.processing.application.ProcessamentoApplicationService;
import com.fiapx.processing.application.event.VideoSolicitadoEvent;
import com.fiapx.processing.config.KafkaTopicsProperties;
import com.fiapx.processing.infrastructure.persistence.EventoProcessadoEntity;
import com.fiapx.processing.infrastructure.repository.EventoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consome os pedidos de processamento com garantia de idempotencia por eventId.
 * O commit do offset acontece somente apos o processamento terminar.
 */
@Component
public class VideoSolicitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoSolicitadoConsumer.class);

    private final ProcessamentoApplicationService processamentoService;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topics;

    public VideoSolicitadoConsumer(ProcessamentoApplicationService processamentoService,
                                   EventoProcessadoRepository eventoProcessadoRepository,
                                   ObjectMapper objectMapper,
                                   KafkaTopicsProperties topics) {
        this.processamentoService = processamentoService;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    @KafkaListener(topics = "${fiapx.kafka.topics.solicitado}",
        groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(String payload) {
        VideoSolicitadoEvent evento = objectMapper.readValue(payload, VideoSolicitadoEvent.class);
        if (evento.eventId() == null || evento.eventId().isBlank()) {
            throw new IllegalArgumentException("Evento sem eventId");
        }
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            log.info("Evento {} ja processado; ignorando", evento.eventId());
            return;
        }

        processamentoService.processar(evento);
        eventoProcessadoRepository.save(new EventoProcessadoEntity(evento.eventId(), topics.getSolicitado()));
        log.info("Evento {} processado para o video {}", evento.eventId(), evento.videoId());
    }
}
