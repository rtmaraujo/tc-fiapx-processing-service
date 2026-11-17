package com.fiapx.processing.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoProcessadoEvent(
    String eventId,
    UUID videoId,
    String framesZipKey,
    int totalFrames,
    LocalDateTime processadoEm
) {
}
