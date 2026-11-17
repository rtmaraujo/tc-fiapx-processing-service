package com.fiapx.processing.infrastructure.storage;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.application.port.StoragePort;
import com.fiapx.processing.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.DownloadObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;

public class MinioStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final MinioClient minioClient;
    private final StorageProperties properties;
    private volatile boolean bucketVerificado;

    public MinioStorageAdapter(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void baixarParaArquivo(String chave, Path destino) {
        garantirBucket();
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(chave)
                .filename(destino.toString())
                .overwrite(true)
                .build());
        } catch (Exception ex) {
            throw new FalhaProcessamentoException("Falha ao baixar o video " + chave, ex);
        }
    }

    @Override
    public void salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType) {
        garantirBucket();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(chave)
                .stream(conteudo, tamanhoBytes, -1)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build());
        } catch (Exception ex) {
            throw new FalhaProcessamentoException("Falha ao salvar o ZIP em " + chave, ex);
        }
    }

    private synchronized void garantirBucket() {
        if (bucketVerificado) {
            return;
        }
        try {
            boolean existe = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("Bucket {} criado no MinIO", properties.getBucket());
            }
            bucketVerificado = true;
        } catch (Exception ex) {
            throw new FalhaProcessamentoException("Falha ao preparar bucket " + properties.getBucket(), ex);
        }
    }
}
