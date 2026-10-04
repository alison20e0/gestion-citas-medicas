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

@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody CrearPacienteRequest request) {
        PacienteResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/pacientes/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public PacienteResponse buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}/desactivar")
    public PacienteResponse desactivar(@PathVariable UUID id) {
        return service.desactivar(id);
    }
}