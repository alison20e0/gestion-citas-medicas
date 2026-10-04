package com.citasmedicas.cita.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c join fetch c.medico join fetch c.paciente where c.id = :id")
    Optional<Cita> findByIdBloqueado(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.medico.id = :medicoId and c.fechaHora = :fechaHora")
    List<Cita> findBloqueadasPorMedicoYHora(@Param("medicoId") UUID medicoId,
            @Param("fechaHora") OffsetDateTime fechaHora);

    @Query("select case when count(c) > 0 then true else false end from Cita c "
            + "where c.medico.id = :medicoId and c.fechaHora = :fechaHora and c.estado <> "
            + "com.citasmedicas.cita.domain.EstadoCita.CANCELADA")
    boolean existeReservaActiva(@Param("medicoId") UUID medicoId, @Param("fechaHora") OffsetDateTime fechaHora);

    @Query("select c from Cita c where c.medico.id = :medicoId and c.fechaHora >= :desde and c.fechaHora < :hasta "
            + "and c.estado <> com.citasmedicas.cita.domain.EstadoCita.CANCELADA order by c.fechaHora")
    List<Cita> findReservadasEnRango(@Param("medicoId") UUID medicoId, @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);

    @Query("select c from Cita c join fetch c.medico join fetch c.paciente "
            + "where c.estado = com.citasmedicas.cita.domain.EstadoCita.CONFIRMADA "
            + "and c.fechaHora >= :desde and c.fechaHora <= :hasta order by c.fechaHora")
    List<Cita> findConfirmadasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("select c from Cita c join fetch c.medico join fetch c.paciente where c.id = :id")
    Optional<Cita> findDetallePorId(@Param("id") UUID id);

    @Query("select c from Cita c join fetch c.medico join fetch c.paciente "
            + "where c.paciente.id = :pacienteId order by c.fechaHora desc")
    List<Cita> findPorPaciente(@Param("pacienteId") UUID pacienteId, Pageable pageable);

    @Query("select c from Cita c join fetch c.medico join fetch c.paciente "
            + "where c.medico.id = :medicoId order by c.fechaHora desc")
    List<Cita> findPorMedico(@Param("medicoId") UUID medicoId, Pageable pageable);
}