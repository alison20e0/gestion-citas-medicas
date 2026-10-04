package com.citasmedicas.medico;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.medico.domain.HorarioAtencion;
import com.citasmedicas.medico.domain.Medico;
import com.citasmedicas.medico.domain.MedicoRepository;
import com.citasmedicas.medico.dto.CrearMedicoRequest;
import com.citasmedicas.medico.dto.MedicoResponse;
import com.citasmedicas.shared.error.ConflictException;
import com.citasmedicas.shared.error.NotFoundException;
import com.citasmedicas.shared.error.ReglaNegocioException;

@Service
public class MedicoService {

    private final MedicoRepository repository;

    public MedicoService(MedicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MedicoResponse crear(CrearMedicoRequest request) {
        if (repository.existsByDocumento(request.documento())) {
            throw new ConflictException("MEDICO_DUPLICADO",
                    "Ya existe un medico con el documento " + request.documento());
        }
        Set<HorarioAtencion> horarios = new LinkedHashSet<>();
        try {
            for (CrearMedicoRequest.HorarioRequest horario : request.horarios()) {
                horarios.add(new HorarioAtencion(horario.diaSemana(), horario.horaInicio(), horario.horaFin(),
                        horario.duracionMinutos()));
            }
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException(ex.getMessage());
        }
        Medico medico = Medico.crear(request.documento(), request.nombres(), request.apellidos(),
                request.especialidad(), request.telefono(), request.email(), horarios);
        return MedicoResponse.from(repository.save(medico));
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscarPorId(UUID id) {
        return MedicoResponse.from(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<MedicoResponse> listar() {
        return repository.findByActivoTrueOrderByNombresAsc().stream().map(MedicoResponse::from).toList();
    }

    @Transactional
    public MedicoResponse desactivar(UUID id) {
        Medico medico = obtener(id);
        medico.desactivar();
        return MedicoResponse.from(medico);
    }

    private Medico obtener(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medico", id));
    }
}