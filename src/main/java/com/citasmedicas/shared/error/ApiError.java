package com.citasmedicas.shared.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String codigo,
        String mensaje,
        String ruta,
        Map<String, String> erroresCampos) {
}