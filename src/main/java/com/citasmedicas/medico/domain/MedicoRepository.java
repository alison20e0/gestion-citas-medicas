package com.citasmedicas.medico.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {

    Optional<Medico> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    List<Medico> findByActivoTrueOrderByNombresAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.id = :id")
    Optional<Medico> findByIdBloqueado(@Param("id") UUID id);
}