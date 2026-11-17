package com.fiapx.processing.infrastructure.media;

import java.time.Duration;
import java.nio.file.Path;
import java.util.List;

public interface ProcessRunner {

    ProcessResult executar(List<String> comando, Duration timeout, Path arquivoLog);
}
