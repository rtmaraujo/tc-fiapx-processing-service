package com.fiapx.processing.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Processamento {

    private UUID videoId;
    private String eventId;
    private ProcessamentoStatus status;
    private String framesZipKey;
    private int totalFrames;
    private String erroMensagem;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Processamento() {
    }

    public Processamento(UUID videoId, String eventId, ProcessamentoStatus status, String framesZipKey,
                         int totalFrames, String erroMensagem, LocalDateTime criadoEm,
                         LocalDateTime atualizadoEm) {
        this.videoId = videoId;
        this.eventId = eventId;
        this.status = status;
        this.framesZipKey = framesZipKey;
        this.totalFrames = totalFrames;
        this.erroMensagem = erroMensagem;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static Processamento recebido(UUID videoId, String eventId) {
        LocalDateTime agora = LocalDateTime.now();
        return new Processamento(videoId, eventId, ProcessamentoStatus.RECEBIDO, null, 0, null, agora, agora);
    }

    public void marcarProcessando() {
        this.status = ProcessamentoStatus.PROCESSANDO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void concluir(String framesZipKey, int totalFrames) {
        this.status = ProcessamentoStatus.CONCLUIDO;
        this.framesZipKey = framesZipKey;
        this.totalFrames = totalFrames;
        this.erroMensagem = null;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void falhar(String motivo) {
        this.status = ProcessamentoStatus.FALHA;
        this.erroMensagem = motivo;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public ProcessamentoStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessamentoStatus status) {
        this.status = status;
    }

    public String getFramesZipKey() {
        return framesZipKey;
    }

    public void setFramesZipKey(String framesZipKey) {
        this.framesZipKey = framesZipKey;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public void setTotalFrames(int totalFrames) {
        this.totalFrames = totalFrames;
    }

    public String getErroMensagem() {
        return erroMensagem;
    }

    public void setErroMensagem(String erroMensagem) {
        this.erroMensagem = erroMensagem;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
