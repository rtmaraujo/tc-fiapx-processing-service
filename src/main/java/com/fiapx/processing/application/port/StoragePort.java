package com.fiapx.processing.application.port;

import java.io.InputStream;
import java.nio.file.Path;

public interface StoragePort {

    void baixarParaArquivo(String chave, Path destino);

    void salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType);
}
