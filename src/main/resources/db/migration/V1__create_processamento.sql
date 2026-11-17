CREATE TABLE processamento (
    video_id UUID PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    frames_zip_key VARCHAR(512),
    total_frames INTEGER NOT NULL DEFAULT 0,
    erro_mensagem VARCHAR(1000),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_processamento_status ON processamento (status);
CREATE INDEX idx_processamento_atualizado ON processamento (atualizado_em);

CREATE TABLE evento_processado (
    event_id VARCHAR(120) PRIMARY KEY,
    topico VARCHAR(160) NOT NULL,
    processado_em TIMESTAMP NOT NULL
);
