package com.citasmedicas.auditoria;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.citasmedicas.auditoria.domain.AuditoriaAccesoDenegado;
import com.citasmedicas.auditoria.domain.AuditoriaAccesoDenegadoRepository;
import com.citasmedicas.auditoria.domain.AuditoriaCita;
import com.citasmedicas.auditoria.domain.AuditoriaCitaRepository;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;
import com.citasmedicas.auditoria.dto.AccesoDenegadoResponse;
import com.citasmedicas.auditoria.dto.AuditoriaCitaResponse;
import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.config.JpaAuditingConfig;
import com.citasmedicas.seguridad.ContextoUsuario;
import com.citasmedicas.seguridad.Rol;
import com.citasmedicas.seguridad.UsuarioActual;

@Service
public class AuditoriaService {

    private static final int LIMITE_POR_DEFECTO = 100;

    private final AuditoriaCitaRepository repository;
    private final AuditoriaAccesoDenegadoRepository accesoDenegadoRepository;
    private final ContextoUsuario contextoUsuario;

    public AuditoriaService(AuditoriaCitaRepository repository,
            AuditoriaAccesoDenegadoRepository accesoDenegadoRepository, ContextoUsuario contextoUsuario) {
        this.repository = repository;
        this.accesoDenegadoRepository = accesoDenegadoRepository;
        this.contextoUsuario = contextoUsuario;
    }

    @Transactional
    public AuditoriaCita registrar(Cita cita, TipoAccionAuditoria accion, String estadoAnterior, String estadoNuevo,
            String detalle, String usuario) {
        AuditoriaCita auditoria = AuditoriaCita.registrar(cita, accion, estadoAnterior, estadoNuevo, detalle,
                usuarioEnCurso(usuario), direccionIpActual(), Instant.now());
        return repository.save(auditoria);
    }

    @Transactional
    public void registrarAccesoDenegado(UUID usuarioId, Rol rol, String metodo, String ruta, String motivo,
            String direccionIp) {
        accesoDenegadoRepository.save(AuditoriaAccesoDenegado.registrar(usuarioId, rol, metodo, ruta, motivo,
                direccionIp, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<AccesoDenegadoResponse> listarAccesosDenegados(int limite) {
        int max = limite <= 0 ? LIMITE_POR_DEFECTO : Math.min(limite, LIMITE_POR_DEFECTO);
        return accesoDenegadoRepository.findAllByOrderByFechaRegistroDesc(PageRequest.of(0, max)).stream()
                .map(AccesoDenegadoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaCitaResponse> listarPorCita(UUID citaId, int limite) {
        int max = limite <= 0 ? LIMITE_POR_DEFECTO : Math.min(limite, LIMITE_POR_DEFECTO);
        return repository.findByCitaIdOrderByFechaRegistroDesc(citaId, PageRequest.of(0, max)).stream()
                .map(AuditoriaCitaResponse::from)
                .toList();
    }

    private String usuarioEnCurso(String usuario) {
        if (usuario != null && !usuario.isBlank()) {
            return usuario;
        }
        return contextoUsuario.opcional()
                .map(UsuarioActual::etiqueta)
                .orElse(JpaAuditingConfig.USUARIO_ANONIMO);
    }

    private String direccionIpActual() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}