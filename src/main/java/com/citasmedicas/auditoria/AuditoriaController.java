package com.citasmedicas.auditoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.citasmedicas.auditoria.dto.AccesoDenegadoResponse;
import com.citasmedicas.seguridad.Permitido;
import com.citasmedicas.seguridad.Rol;

@RestController
@RequestMapping("/api/v1/auditoria")
@Permitido(Rol.ADMIN)
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/accesos-denegados")
    public List<AccesoDenegadoResponse> listarAccesosDenegados(@RequestParam(defaultValue = "50") int limite) {
        return auditoriaService.listarAccesosDenegados(limite);
    }
}