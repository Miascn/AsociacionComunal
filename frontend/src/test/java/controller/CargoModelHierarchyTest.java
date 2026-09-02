package controller;

import models.CargoModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CargoModelHierarchyTest {

    @Test
    void descripcionJerarquiaDevuelveTextosClaros() {
        assertEquals("1 - Nivel Máximo (Presidencia)", CargoModel.getDescripcionJerarquia(1));
        assertEquals("2 - Nivel Alto (Vicepresidencia)", CargoModel.getDescripcionJerarquia(2));
        assertEquals("3 - Nivel Medio-Alto (Secretaría)", CargoModel.getDescripcionJerarquia(3));
        assertEquals("4 - Nivel Medio (Tesorería)", CargoModel.getDescripcionJerarquia(4));
        assertEquals("5 - Nivel Operativo (Vocalía / Síndico)", CargoModel.getDescripcionJerarquia(5));
        assertEquals("6 - Nivel de Apoyo", CargoModel.getDescripcionJerarquia(6));
    }

    @Test
    void descripcionCortaJerarquiaDevuelveTextosAmigables() {
        assertEquals("Nivel Máximo", CargoModel.getDescripcionCortaJerarquia(1));
        assertEquals("Nivel Alto", CargoModel.getDescripcionCortaJerarquia(2));
        assertEquals("Nivel Medio-Alto", CargoModel.getDescripcionCortaJerarquia(3));
        assertEquals("Nivel Medio", CargoModel.getDescripcionCortaJerarquia(4));
        assertEquals("Nivel Operativo", CargoModel.getDescripcionCortaJerarquia(5));
        assertEquals("Nivel 6", CargoModel.getDescripcionCortaJerarquia(6));
    }

    @Test
    void getNivelDisplayUsaDescripcionDescriptiva() {
        CargoModel cargo = new CargoModel(1, "Presidente", "Lider", 1, true, 0);
        assertEquals("1 - Nivel Máximo (Presidencia)", cargo.getNivelDisplay());

        cargo.setNivelJerarquico(2);
        assertEquals("2 - Nivel Alto (Vicepresidencia)", cargo.getNivelDisplay());
    }
}
