package com.fiapx.processing.infrastructure.media;

import com.fiapx.processing.application.exception.FalhaProcessamentoException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipService {

    public void zipar(List<Path> arquivos, Path destino) {
        try (OutputStream saida = Files.newOutputStream(destino);
             ZipOutputStream zip = new ZipOutputStream(saida)) {
            for (Path arquivo : arquivos) {
                zip.putNextEntry(new ZipEntry(arquivo.getFileName().toString()));
                Files.copy(arquivo, zip);
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw new FalhaProcessamentoException("Nao foi possivel gerar o ZIP dos frames", ex);
        }
    }
}
