package com.citasmedicas.medico;

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

import com.citasmedicas.medico.dto.CrearMedicoRequest;
import com.citasmedicas.medico.dto.MedicoResponse;
import com.citasmedicas.seguridad.Permitido;
import com.citasmedicas.seguridad.Rol;

@RestController
@RequestMapping("/api/v1/medicos")
public class MedicoController {

    private final MedicoService service;

    public MedicoController(MedicoService service) {
        this.service = service;
    }

    @PostMapping
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public ResponseEntity<MedicoResponse> crear(@Valid @RequestBody CrearMedicoRequest request) {
        MedicoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/medicos/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public MedicoResponse buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    public List<MedicoResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}/desactivar")
    @Permitido({Rol.RECEPCION, Rol.ADMIN})
    public MedicoResponse desactivar(@PathVariable UUID id) {
        return service.desactivar(id);
    }
}