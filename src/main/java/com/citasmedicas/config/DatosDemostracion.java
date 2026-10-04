package com.citasmedicas.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.medico.domain.Especialidad;
import com.citasmedicas.medico.domain.HorarioAtencion;
import com.citasmedicas.medico.domain.Medico;
import com.citasmedicas.medico.domain.MedicoRepository;
import com.citasmedicas.paciente.domain.Paciente;
import com.citasmedicas.paciente.domain.PacienteRepository;

@Component
public class DatosDemostracion implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemostracion.class);

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;

    public DatosDemostracion(MedicoRepository medicoRepository, PacienteRepository pacienteRepository) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (medicoRepository.count() > 0 || pacienteRepository.count() > 0) {
            return;
        }
        Medico medicaGeneral = medicoRepository.save(Medico.crear("MED-0001", "Ana", "Rivas",
                Especialidad.MEDICINA_GENERAL, "+18095550101", "ana.rivas@clinica.local", semana(8, 12)));
        Medico cardiologo = medicoRepository.save(Medico.crear("MED-0002", "Luis", "Ferrer",
                Especialidad.CARDIOLOGIA, "+18095550102", "luis.ferrer@clinica.local", semana(13, 17)));
        pacienteRepository.save(Paciente.crear("CED-1001", "Maria", "Lopez", null, "+18095550201",
                "maria.lopez@correo.local"));
        pacienteRepository.save(Paciente.crear("CED-1002", "Jose", "Perez", null, "+18095550202",
                "jose.perez@correo.local"));
        log.info("Datos de demostracion creados. Medico general={} ({}), Cardiologo={} ({}), 2 pacientes",
                medicaGeneral.nombreCompleto(), medicaGeneral.getId(), cardiologo.nombreCompleto(),
                cardiologo.getId());
    }

    private Set<HorarioAtencion> semana(int horaInicio, int horaFin) {
        Set<HorarioAtencion> horarios = new LinkedHashSet<>();
        for (DayOfWeek dia : new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            horarios.add(new HorarioAtencion(dia, LocalTime.of(horaInicio, 0), LocalTime.of(horaFin, 0), 30));
        }
        return horarios;
    }
}