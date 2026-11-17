package com.fiapx.processing.application.port;

public interface EventoPublisher {

    void publicar(String topico, String chave, Object evento);
}
