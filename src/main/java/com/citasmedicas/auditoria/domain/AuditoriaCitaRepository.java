package com.citasmedicas.auditoria.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaCitaRepository extends JpaRepository<AuditoriaCita, UUID> {

    List<AuditoriaCita> findByCitaIdOrderByFechaRegistroDesc(UUID citaId, Pageable pageable);
}