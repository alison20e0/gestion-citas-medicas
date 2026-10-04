package com.citasmedicas.notificacion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.citasmedicas.notificacion.domain.NotificacionTipo;
import com.citasmedicas.shared.error.ReglaNegocioException;

@Component
public class CanalesNotificacion {

    private final Map<NotificacionTipo, CanalNotificacion> canales = new EnumMap<>(NotificacionTipo.class);

    public CanalesNotificacion(List<CanalNotificacion> disponible) {
        disponible.forEach(canal -> canales.put(canal.tipo(), canal));
    }

    public CanalNotificacion obtener(NotificacionTipo tipo) {
        CanalNotificacion canal = canales.get(tipo);
        if (canal == null) {
            throw new ReglaNegocioException("No hay canal configurado para el tipo " + tipo);
        }
        return canal;
    }
}