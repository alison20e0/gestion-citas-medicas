package com.citasmedicas.seguridad;

/**
 * Roles simulados de la clinica. La identidad viaja en los encabezados
 * X-Usuario-Id y X-Rol, no hay autenticacion real (ver README).
 */
public enum Rol {
    PACIENTE,
    RECEPCION,
    MEDICO,
    ADMIN
}