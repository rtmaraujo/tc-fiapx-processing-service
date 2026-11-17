package com.fiapx.processing.infrastructure.storage;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;
import com.fiapx.processing.config.StorageProperties;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioStorageAdapterTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageAdapter adapter;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setBucket("teste");
        adapter = new MinioStorageAdapter(minioClient, properties);
    }

    @Test
    @DisplayName("baixa objeto para arquivo criando bucket quando necessario")
    void deveBaixarParaArquivo() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(false);

        adapter.baixarParaArquivo("videos/a.mp4", tempDir.resolve("input.mp4"));

        verify(minioClient).makeBucket(any());
        verify(minioClient).downloadObject(any(DownloadObjectArgs.class));
    }

    @Test
    @DisplayName("salva objeto reaproveitando bucket existente")
    void deveSalvarObjeto() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);

        adapter.salvar("frames/a.zip", new ByteArrayInputStream("zip".getBytes()), 3L, "application/zip");
        adapter.salvar("frames/b.zip", new ByteArrayInputStream("zip".getBytes()), 3L, null);

        verify(minioClient, org.mockito.Mockito.times(1)).bucketExists(any());
        verify(minioClient, org.mockito.Mockito.times(2)).putObject(any(PutObjectArgs.class));
        verify(minioClient, never()).makeBucket(any());
    }

    @Test
    @DisplayName("encapsula falha de download")
    void deveFalharDownload() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        doThrow(new RuntimeException("minio off")).when(minioClient).downloadObject(any(DownloadObjectArgs.class));

        assertThatThrownBy(() -> adapter.baixarParaArquivo("videos/a.mp4", tempDir.resolve("input.mp4")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("baixar o video");
    }

    @Test
    @DisplayName("encapsula falha de upload")
    void deveFalharUpload() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        doThrow(new RuntimeException("minio off")).when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> adapter.salvar("frames/a.zip",
            new ByteArrayInputStream(new byte[0]), 0L, "application/zip"))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("salvar o ZIP");
    }

    @Test
    @DisplayName("encapsula falha ao preparar bucket")
    void deveFalharBucket() throws Exception {
        when(minioClient.bucketExists(any())).thenThrow(new RuntimeException("off"));

        assertThatThrownBy(() -> adapter.baixarParaArquivo("a", tempDir.resolve("a")))
            .isInstanceOf(FalhaProcessamentoException.class)
            .hasMessageContaining("preparar bucket");
    }
}
