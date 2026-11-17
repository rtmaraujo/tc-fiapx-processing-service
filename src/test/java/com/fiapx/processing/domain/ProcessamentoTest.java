package com.fiapx.processing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessamentoTest {

    @Test
    @DisplayName("cria processamento RECEBIDO")
    void deveCriarRecebido() {
        UUID videoId = UUID.randomUUID();

        Processamento processamento = Processamento.recebido(videoId, "evt-1");

        assertThat(processamento.getVideoId()).isEqualTo(videoId);
        assertThat(processamento.getEventId()).isEqualTo("evt-1");
        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.RECEBIDO);
        assertThat(processamento.getCriadoEm()).isNotNull();
        assertThat(processamento.getTotalFrames()).isZero();
    }

    @Test
    @DisplayName("marca como PROCESSANDO")
    void deveMarcarProcessando() {
        Processamento processamento = Processamento.recebido(UUID.randomUUID(), "evt");

        processamento.marcarProcessando();

        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.PROCESSANDO);
    }

    @Test
    @DisplayName("conclui com chave e total de frames")
    void deveConcluir() {
        Processamento processamento = Processamento.recebido(UUID.randomUUID(), "evt");
        processamento.falhar("erro anterior");

        processamento.concluir("frames/a.zip", 12);

        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.CONCLUIDO);
        assertThat(processamento.getFramesZipKey()).isEqualTo("frames/a.zip");
        assertThat(processamento.getTotalFrames()).isEqualTo(12);
        assertThat(processamento.getErroMensagem()).isNull();
    }

    @Test
    @DisplayName("falha com motivo")
    void deveFalhar() {
        Processamento processamento = Processamento.recebido(UUID.randomUUID(), "evt");

        processamento.falhar("codec invalido");

        assertThat(processamento.getStatus()).isEqualTo(ProcessamentoStatus.FALHA);
        assertThat(processamento.getErroMensagem()).isEqualTo("codec invalido");
    }
}
