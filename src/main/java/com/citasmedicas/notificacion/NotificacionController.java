package com.citasmedicas.notificacion;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.citasmedicas.notificacion.dto.NotificacionEnvioResponse;
import com.citasmedicas.seguridad.ContextoUsuario;
import com.citasmedicas.seguridad.Permitido;
import com.citasmedicas.seguridad.Rol;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final NotificacionProcesador procesador;
    private final ContextoUsuario contextoUsuario;

    public NotificacionController(NotificacionService notificacionService, NotificacionProcesador procesador,
            ContextoUsuario contextoUsuario) {
        this.notificacionService = notificacionService;
        this.procesador = procesador;
        this.contextoUsuario = contextoUsuario;
    }

    @GetMapping
    public List<NotificacionEnvioResponse> listarPorCita(@RequestParam UUID citaId) {
        return notificacionService.listarPorCita(contextoUsuario.exigir(), citaId);
    }

    @PostMapping("/{id}/reintentar")
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public NotificacionEnvioResponse reintentar(@PathVariable UUID id) {
        return procesador.reintentar(id);
    }

    @PostMapping("/barrido")
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public ResponseEntity<Void> forzarBarrido() {
        int programados = notificacionService.programarRecordatorios();
        int enviados = procesador.procesarPendientes();
        return ResponseEntity.accepted()
                .header("X-Notificaciones-Programadas", String.valueOf(programados))
                .header("X-Notificaciones-Enviadas", String.valueOf(enviados))
                .build();
    }
}