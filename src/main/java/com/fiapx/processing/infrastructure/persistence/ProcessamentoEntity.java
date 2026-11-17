package com.fiapx.processing.infrastructure.persistence;

import com.fiapx.processing.domain.Processamento;
import com.fiapx.processing.domain.ProcessamentoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processamento")
public class ProcessamentoEntity {

    @Id
    @Column(name = "video_id")
    private UUID videoId;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessamentoStatus status;

    @Column(name = "frames_zip_key", length = 512)
    private String framesZipKey;

    @Column(name = "total_frames", nullable = false)
    private int totalFrames;

    @Column(name = "erro_mensagem", length = 1000)
    private String erroMensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected ProcessamentoEntity() {
    }

    public ProcessamentoEntity(Processamento processamento) {
        this.videoId = processamento.getVideoId();
        this.eventId = processamento.getEventId();
        this.status = processamento.getStatus();
        this.framesZipKey = processamento.getFramesZipKey();
        this.totalFrames = processamento.getTotalFrames();
        this.erroMensagem = processamento.getErroMensagem();
        this.criadoEm = processamento.getCriadoEm();
        this.atualizadoEm = processamento.getAtualizadoEm();
    }

    public void aplicar(Processamento processamento) {
        this.status = processamento.getStatus();
        this.framesZipKey = processamento.getFramesZipKey();
        this.totalFrames = processamento.getTotalFrames();
        this.erroMensagem = processamento.getErroMensagem();
        this.atualizadoEm = processamento.getAtualizadoEm();
    }

    public Processamento toDomain() {
        return new Processamento(videoId, eventId, status, framesZipKey, totalFrames, erroMensagem,
            criadoEm, atualizadoEm);
    }

    public UUID getVideoId() {
        return videoId;
    }

    public String getEventId() {
        return eventId;
    }

    public ProcessamentoStatus getStatus() {
        return status;
    }

    public String getFramesZipKey() {
        return framesZipKey;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public String getErroMensagem() {
        return erroMensagem;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
