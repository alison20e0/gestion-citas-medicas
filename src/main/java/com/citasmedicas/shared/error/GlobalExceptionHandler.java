package com.citasmedicas.shared.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.citasmedicas.auditoria.AuditoriaService;
import com.citasmedicas.seguridad.ContextoUsuario;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditoriaService auditoriaService;
    private final ContextoUsuario contextoUsuario;

    public GlobalExceptionHandler(AuditoriaService auditoriaService, ContextoUsuario contextoUsuario) {
        this.auditoriaService = auditoriaService;
        this.contextoUsuario = contextoUsuario;
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        registrarAccesoDenegado(request, ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<ApiError> handleNoAutenticado(NoAutenticadoException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridadDatos(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacion de integridad en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICTO_BASE_DATOS",
                "La operacion viola una restriccion de integridad o el recurso ya existe", request, null);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConcurrencia(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflicto de concurrencia en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "El recurso fue modificado por otra transaccion, reintente la operacion", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDACION", "La peticion no es valida", request, errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleFormato(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "FORMATO_INVALIDO", "El cuerpo o parametro enviado no es valido", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleRutaNoEncontrada(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA", "El recurso solicitado no existe", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrio un error inesperado", request, null);
    }

    private void registrarAccesoDenegado(HttpServletRequest request, String motivo) {
        contextoUsuario.opcional().ifPresent(usuario -> {
            try {
                auditoriaService.registrarAccesoDenegado(usuario.id(), usuario.rol(), request.getMethod(),
                        request.getRequestURI(), motivo, request.getRemoteAddr());
            } catch (RuntimeException ex) {
                log.error("No se pudo registrar el acceso denegado a {}", request.getRequestURI(), ex);
            }
        });
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String codigo, String mensaje,
            HttpServletRequest request, Map<String, String> erroresCampos) {
        ApiError error = new ApiError(Instant.now(), status.value(), codigo, mensaje,
                request.getRequestURI(), erroresCampos);
        return ResponseEntity.status(status).body(error);
    }
}