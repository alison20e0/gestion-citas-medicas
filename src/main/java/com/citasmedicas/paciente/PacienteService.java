package com.citasmedicas.paciente;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.paciente.domain.Paciente;
import com.citasmedicas.paciente.domain.PacienteRepository;
import com.citasmedicas.paciente.dto.CrearPacienteRequest;
import com.citasmedicas.paciente.dto.PacienteResponse;
import com.citasmedicas.seguridad.Alcances;
import com.citasmedicas.seguridad.UsuarioActual;
import com.citasmedicas.shared.error.ConflictException;
import com.citasmedicas.shared.error.NotFoundException;

@Service
public class PacienteService {

    private final PacienteRepository repository;

    public PacienteService(PacienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PacienteResponse crear(CrearPacienteRequest request) {
        if (repository.existsByDocumento(request.documento())) {
            throw new ConflictException("PACIENTE_DUPLICADO",
                    "Ya existe un paciente con el documento " + request.documento());
        }
        Paciente paciente = Paciente.crear(request.documento(), request.nombres(), request.apellidos(),
                request.fechaNacimiento(), request.telefono(), request.email());
        return PacienteResponse.from(repository.save(paciente));
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(UsuarioActual usuario, UUID id) {
        Paciente paciente = obtener(id);
        Alcances.exigirLecturaDeFicha(usuario, paciente.getId());
        return PacienteResponse.from(paciente);
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return repository.findAll().stream().map(PacienteResponse::from).toList();
    }

    @Transactional
    public PacienteResponse desactivar(UUID id) {
        Paciente paciente = obtener(id);
        paciente.desactivar();
        return PacienteResponse.from(paciente);
    }

    private Paciente obtener(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Paciente", id));
    }
}