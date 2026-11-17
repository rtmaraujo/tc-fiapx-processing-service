package com.fiapx.processing.application;

import com.fiapx.processing.application.event.VideoSolicitadoEvent;

public interface ProcessamentoApplicationService {

    void processar(VideoSolicitadoEvent evento);
}
