package com.citasmedicas.cita;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.citasmedicas.auditoria.dto.AuditarCitaRequest;
import com.citasmedicas.auditoria.dto.AuditoriaCitaResponse;
import com.citasmedicas.cita.dto.CancelarCitaRequest;
import com.citasmedicas.cita.dto.CitaResponse;
import com.citasmedicas.cita.dto.DisponibilidadResponse;
import com.citasmedicas.cita.dto.ReservarCitaRequest;
import com.citasmedicas.seguridad.ContextoUsuario;
import com.citasmedicas.seguridad.Permitido;
import com.citasmedicas.seguridad.Rol;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    private final CitaService service;
    private final ContextoUsuario contextoUsuario;

    public CitaController(CitaService service, ContextoUsuario contextoUsuario) {
        this.service = service;
        this.contextoUsuario = contextoUsuario;
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @RequestParam UUID medicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.disponibilidad(contextoUsuario.exigir(), medicoId, fecha);
    }

    @PostMapping
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody ReservarCitaRequest request) {
        CitaResponse response = service.reservar(request);
        return ResponseEntity.created(URI.create("/api/v1/citas/" + response.id())).body(response);
    }

    @PutMapping("/{id}/cancelar")
    @Permitido({Rol.PACIENTE, Rol.RECEPCION, Rol.ADMIN})
    public CitaResponse cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarCitaRequest request) {
        return service.cancelar(contextoUsuario.exigir(), id, request);
    }

    @PostMapping("/{id}/auditoria")
    @Permitido(Rol.ADMIN)
    public ResponseEntity<AuditoriaCitaResponse> registrarAuditoria(@PathVariable UUID id,
            @Valid @RequestBody AuditarCitaRequest request) {
        AuditoriaCitaResponse response = service.registrarAuditoria(id, request);
        return ResponseEntity
                .created(URI.create("/api/v1/citas/" + id + "/auditoria/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}/auditoria")
    @Permitido(Rol.ADMIN)
    public List<AuditoriaCitaResponse> listarAuditoria(@PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limite) {
        return service.listarAuditoria(id, limite);
    }

    @GetMapping("/{id}")
    public CitaResponse buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(contextoUsuario.exigir(), id);
    }

    @GetMapping
    public List<CitaResponse> listar(@RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) UUID medicoId,
            @RequestParam(defaultValue = "50") int limite) {
        if (pacienteId != null) {
            return service.listarPorPaciente(contextoUsuario.exigir(), pacienteId, limite);
        }
        if (medicoId != null) {
            return service.listarPorMedico(contextoUsuario.exigir(), medicoId, limite);
        }
        return List.of();
    }
}