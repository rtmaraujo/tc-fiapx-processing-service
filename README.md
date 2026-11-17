# tc-fiapx-processing-service

Microsservico de processamento do FIAP X (Fase 5). Consome pedidos no Kafka, baixa o video do storage S3, extrai 1 frame por segundo com FFmpeg, gera um ZIP e publica o resultado.

## Stack

- Java 21 + Spring Boot 4.1.2
- Apache Kafka (consumidor idempotente, produtor, retry + DLT)
- PostgreSQL 16 + Flyway
- Object storage S3-compativel (cliente MinIO SDK)
- FFmpeg (instalado na imagem Docker)
- Actuator, Micrometer, Prometheus, Zipkin
- JUnit 6, Mockito, AssertJ, JaCoCo 85%

## Fluxo

1. Consome `fiapx.video.solicitado` (um registro por poll).
2. Verifica idempotencia em `evento_processado`.
3. Baixa o original do storage S3 para um diretorio temporario.
4. Executa `ffmpeg -vf fps=1` e valida os frames gerados.
5. Compacta os frames em `frames/{videoId}/frames.zip` e envia ao storage S3.
6. Publica `fiapx.video.processado` ou `fiapx.video.falha`.
7. Remove o diretorio temporario em `finally`.

## Configuracao

| Variavel | Padrao |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fiapx_processing` |
| `DB_USER` / `DB_PASSWORD` | `fiapx_processing_user` / `fiapx_processing_pass` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_GROUP_ID` | `fiapx-processing-group` |
| `STORAGE_ENDPOINT` | `http://localhost:8333` |
| `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` | `fiapx` / `fiapx12345` |
| `STORAGE_BUCKET` | `fiapx-videos` |
| `FFMPEG_BINARY` | `ffmpeg` |
| `FFMPEG_FPS` | `1` |
| `FFMPEG_QUALIDADE` | `2` |
| `FFMPEG_TIMEOUT_SECONDS` | `600` |

## Executar

Requer FFmpeg no PATH:

```bash
ffmpeg -version
mvn spring-boot:run
```

Ou com a infraestrutura completa:

```bash
cd .. && docker compose up --build -d processing-service
```

## Testes

```bash
mvn -B verify
```

48 testes e cobertura minima de 85% em linhas e branches. Os testes cobrem timeout real de processo, truncamento de log, falhas de FFmpeg, ZIP invalido, storage S3 e publicacao Kafka.

## Docker

A imagem instala o FFmpeg:

```bash
docker build -t tc-fiapx-processing-service:local .
```

## Kubernetes

Manifests em `k8s/`: namespace, configmap, secret de exemplo, deployment (com volume temporario), service e HPA. Nao expoe API de negocio; apenas actuator interno.

## Documentacao

Arquitetura completa, C4, banco, mensageria e roteiro de execucao: repositorio `tc-fiapx-docs`.
