package com.citasmedicas.seguridad;

import java.util.Arrays;
import java.util.UUID;

public record UsuarioActual(UUID id, Rol rol) {

    public static UsuarioActual de(UUID id, Rol rol) {
        return new UsuarioActual(id, rol);
    }

    public boolean tieneAlguno(Rol... roles) {
        return Arrays.stream(roles).anyMatch(rol -> rol == this.rol);
    }

    public boolean es(Rol otro) {
        return this.rol == otro;
    }

    public String etiqueta() {
        return rol.name() + ":" + id;
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}