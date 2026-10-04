package com.citasmedicas.notificacion.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionEnvioRepository extends JpaRepository<NotificacionEnvio, UUID> {

    List<NotificacionEnvio> findByCitaIdOrderByCreadoEnDesc(UUID citaId, Pageable pageable);

    boolean existsByCitaIdAndReferenciaAndEstadoIn(UUID citaId, String referencia, List<EstadoEnvio> estados);

    @Query("select n from NotificacionEnvio n where n.estado in "
            + "(com.citasmedicas.notificacion.domain.EstadoEnvio.PENDIENTE, "
            + "com.citasmedicas.notificacion.domain.EstadoEnvio.FALLIDO) "
            + "and n.proximoIntento <= :ahora and n.intentos < :maxIntentos order by n.proximoIntento asc")
    List<NotificacionEnvio> findListosParaEnvio(@Param("ahora") Instant ahora, @Param("maxIntentos") int maxIntentos,
            Pageable pageable);

    Optional<NotificacionEnvio> findByCitaIdAndReferenciaAndEstado(UUID citaId, String referencia, EstadoEnvio estado);
}