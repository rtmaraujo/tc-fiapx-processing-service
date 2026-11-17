package com.fiapx.processing.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoFalhaEvent(
    String eventId,
    UUID videoId,
    String motivo,
    LocalDateTime ocorridoEm
) {
}
