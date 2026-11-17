package com.fiapx.processing.application.port;

import java.nio.file.Path;
import java.util.List;

public interface FrameExtractorPort {

    List<Path> extrair(Path video, Path diretorioDestino);
}
