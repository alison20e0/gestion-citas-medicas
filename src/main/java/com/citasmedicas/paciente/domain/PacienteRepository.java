package com.citasmedicas.paciente.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

    Optional<Paciente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}