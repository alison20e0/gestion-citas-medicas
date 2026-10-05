package com.citasmedicas.auditoria.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaAccesoDenegadoRepository extends JpaRepository<AuditoriaAccesoDenegado, UUID> {

    List<AuditoriaAccesoDenegado> findAllByOrderByFechaRegistroDesc(Pageable pageable);
}