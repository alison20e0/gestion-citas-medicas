package com.citasmedicas.seguridad;

import java.util.UUID;

import com.citasmedicas.shared.error.AccesoDenegadoException;

/**
 * Alcance de cada rol sobre las citas.
 *
 * PACIENTE: solo ve y cancela sus propias citas.
 * RECEPCION: ve, crea, modifica y cancela todas.
 * MEDICO: ve solo su propia agenda.
 * ADMIN: ve todas y accede a la auditoria.
 */
public final class Alcances {

    private Alcances() {
    }

    public static void exigirLecturaDeCita(UsuarioActual usuario, UUID pacienteId, UUID medicoId) {
        if (usuario.es(Rol.PACIENTE) && !usuario.id().equals(pacienteId)) {
            throw new AccesoDenegadoException("El paciente solo puede consultar sus propias citas");
        }
        if (usuario.es(Rol.MEDICO) && !usuario.id().equals(medicoId)) {
            throw new AccesoDenegadoException("El medico solo puede consultar las citas de su propia agenda");
        }
    }

    public static void exigirCancelacionDeCita(UsuarioActual usuario, UUID pacienteId) {
        if (usuario.es(Rol.PACIENTE) && !usuario.id().equals(pacienteId)) {
            throw new AccesoDenegadoException("El paciente solo puede cancelar sus propias citas");
        }
    }

    public static void exigirAlcancePaciente(UsuarioActual usuario, UUID pacienteId) {
        if (usuario.es(Rol.MEDICO)) {
            throw new AccesoDenegadoException("El medico solo puede consultar su propia agenda");
        }
        if (usuario.es(Rol.PACIENTE) && !usuario.id().equals(pacienteId)) {
            throw new AccesoDenegadoException("El paciente solo puede consultar sus propias citas");
        }
    }

    public static void exigirAlcanceMedico(UsuarioActual usuario, UUID medicoId) {
        if (usuario.es(Rol.PACIENTE)) {
            throw new AccesoDenegadoException("El paciente solo puede consultar sus propias citas");
        }
        if (usuario.es(Rol.MEDICO) && !usuario.id().equals(medicoId)) {
            throw new AccesoDenegadoException("El medico solo puede consultar su propia agenda");
        }
    }

    public static void exigirDisponibilidad(UsuarioActual usuario, UUID medicoId) {
        if (usuario.es(Rol.MEDICO) && !usuario.id().equals(medicoId)) {
            throw new AccesoDenegadoException("El medico solo puede consultar la disponibilidad de su propia agenda");
        }
    }

    public static void exigirLecturaDeFicha(UsuarioActual usuario, UUID pacienteId) {
        if (usuario.es(Rol.PACIENTE) && !usuario.id().equals(pacienteId)) {
            throw new AccesoDenegadoException("El paciente solo puede consultar su propia ficha");
        }
        if (usuario.es(Rol.MEDICO)) {
            throw new AccesoDenegadoException("El medico no tiene acceso a la ficha de pacientes");
        }
    }
}