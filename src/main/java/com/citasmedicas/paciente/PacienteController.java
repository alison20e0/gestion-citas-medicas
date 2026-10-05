package com.citasmedicas.paciente;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.citasmedicas.paciente.dto.CrearPacienteRequest;
import com.citasmedicas.paciente.dto.PacienteResponse;
import com.citasmedicas.seguridad.ContextoUsuario;
import com.citasmedicas.seguridad.Permitido;
import com.citasmedicas.seguridad.Rol;

@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {

    private final PacienteService service;
    private final ContextoUsuario contextoUsuario;

    public PacienteController(PacienteService service, ContextoUsuario contextoUsuario) {
        this.service = service;
        this.contextoUsuario = contextoUsuario;
    }

    @PostMapping
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody CrearPacienteRequest request) {
        PacienteResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/pacientes/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Permitido({Rol.PACIENTE, Rol.RECEPCION, Rol.ADMIN})
    public PacienteResponse buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(contextoUsuario.exigir(), id);
    }

    @GetMapping
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public List<PacienteResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}/desactivar")
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public PacienteResponse desactivar(@PathVariable UUID id) {
        return service.desactivar(id);
    }
}