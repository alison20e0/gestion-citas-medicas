package com.citasmedicas.config;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citas")
public record CitasProperties(String zonaHoraria, Notificaciones notificaciones) {

    public ZoneId zoneId() {
        return ZoneId.of(zonaHoraria == null || zonaHoraria.isBlank() ? "UTC" : zonaHoraria);
    }

    public ZoneOffset offsetPorDefecto() {
        return zoneId().getRules().getOffset(Instant.now());
    }

    public record Notificaciones(
            boolean habilitadas,
            int horasAnticipacion,
            int maxIntentos,
            Duration esperaReintentoInicial,
            double backoffMultiplicador,
            long intervaloBarridoMilis,
            int loteMaximo,
            boolean simulacionFallo,
            String disparadorFallo,
            Duration simulacionLatencia) {

        public Duration esperaEntreIntentos(int intento) {
            double factor = Math.pow(backoffMultiplicador, Math.max(0, intento - 1));
            return esperaReintentoInicial.multipliedBy(Math.max(1L, (long) factor));
        }

        public boolean debeFallar(String mensaje) {
            return simulacionFallo || (disparadorFallo != null && mensaje.contains(disparadorFallo));
        }
    }
}