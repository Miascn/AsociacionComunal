package sv.asociacion.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la jerarquia de dominio Persona -> Miembro / Residente.
 *
 * <p>El padron de la asociacion registra dos clases de persona con reglas
 * distintas: el miembro asociado y el residente que habita una vivienda sin
 * estar necesariamente asociado.</p>
 */
class PersonaJerarquiaTest {

    private static Miembro miembroDePrueba() {
        Miembro miembro = new Miembro();
        miembro.setIdMiembro(7);
        miembro.setDui("01234567-8");
        miembro.setNombres("  Ana Maria  ");
        miembro.setApellidos("  Gonzalez Reyes ");
        miembro.setTelefono("7777-0000");
        miembro.setFechaIngreso(LocalDate.of(2026, 1, 15));
        miembro.setEstado(Miembro.Estado.ACTIVO);
        miembro.setIdVivienda(12);
        return miembro;
    }

    @Test
    @DisplayName("Persona es abstracta y no puede instanciarse directamente")
    void personaEsAbstracta() {
        assertTrue(Modifier.isAbstract(Persona.class.getModifiers()),
            "Persona debe ser abstracta: no existe una persona del padron que no sea miembro o residente");
        assertTrue(Serializable.class.isAssignableFrom(Persona.class),
            "Persona debe ser serializable para la persistencia en archivos .dat");
    }

    @Test
    @DisplayName("Un Miembro es una Persona")
    void miembroEsPersona() {
        Miembro miembro = miembroDePrueba();

        assertInstanceOf(Persona.class, miembro);
        assertEquals("Ana Maria Gonzalez Reyes", miembro.getNombreCompleto(),
            "el miembro guarda nombres y apellidos por separado y debe componerlos sin espacios sobrantes");
        assertTrue(miembro.esAsociado(), "todo miembro registrado es asociado");
    }

    @Test
    @DisplayName("Un Residente es una Persona")
    void residenteEsPersona() {
        Residente hijo = new Residente(3, 12, null, "Carlos Gonzalez", Residente.TipoPersona.MENOR, false);

        assertInstanceOf(Persona.class, hijo);
        assertEquals("Carlos Gonzalez", hijo.getNombreCompleto(),
            "el residente ya trae su nombre en un unico campo");
        assertFalse(hijo.esAsociado(), "un residente sin ficha de miembro no es asociado");
    }

    @Test
    @DisplayName("El residente vinculado a una ficha de miembro si es asociado")
    void residenteVinculadoEsAsociado() {
        Residente representante = new Residente(1, 12, 7, "Ana Maria Gonzalez Reyes",
            Residente.TipoPersona.ADULTO, true);

        assertTrue(representante.esAsociado(), "tiene id_miembro, por lo tanto es asociado");
        assertTrue(representante.isRepresentante());
        assertFalse(representante.esMenor());
    }

    @Test
    @DisplayName("Cada subclase resuelve el contrato de Persona a su manera")
    void despachoPolimorfico() {
        List<Persona> padron = List.of(
            miembroDePrueba(),
            new Residente(3, 12, null, "Carlos Gonzalez", Residente.TipoPersona.MENOR, false)
        );

        // Se recorre la lista sin preguntar por el tipo concreto.
        List<String> nombres = padron.stream().map(Persona::getNombreCompleto).toList();
        assertEquals(List.of("Ana Maria Gonzalez Reyes", "Carlos Gonzalez"), nombres);

        long asociados = padron.stream().filter(Persona::esAsociado).count();
        assertEquals(1, asociados, "de los dos habitantes de la vivienda, solo uno es asociado");
    }

    @Test
    @DisplayName("La vivienda es el unico estado compartido en Persona")
    void viviendaEsEstadoCompartido() {
        Persona miembro = miembroDePrueba();
        Persona residente = new Residente(3, 12, null, "Carlos Gonzalez",
            Residente.TipoPersona.MENOR, false);

        assertEquals(12, miembro.getIdVivienda());
        assertEquals(12, residente.getIdVivienda(),
            "ambos habitan la misma vivienda y ese dato vive en la clase base");
    }

    @Test
    @DisplayName("Cada subclase conserva su comportamiento especifico")
    void comportamientoEspecificoDeCadaSubclase() {
        Miembro miembro = miembroDePrueba();
        Residente menor = new Residente(3, 12, null, "Carlos Gonzalez",
            Residente.TipoPersona.MENOR, false);

        // Propio del miembro: documento, fecha de ingreso y estado de asociacion.
        assertEquals("01234567-8", miembro.getDui());
        assertEquals(LocalDate.of(2026, 1, 15), miembro.getFechaIngreso());
        assertTrue(miembro.estaActivo());

        // Propio del residente: clasificacion del censo y condicion de representante.
        assertEquals(Residente.TipoPersona.MENOR, menor.getTipoPersona());
        assertTrue(menor.esMenor());
        assertFalse(menor.isRepresentante());
    }

    @Test
    @DisplayName("El residente no exige los datos que la base no le pide")
    void residenteNoArrastraDatosDeMiembro() {
        Residente menor = new Residente(3, 12, null, "Carlos Gonzalez",
            Residente.TipoPersona.MENOR, false);

        // residente_vivienda no tiene columnas de DUI, telefono ni correo:
        // la clase no debe inventarlas solo para parecerse al miembro.
        assertThrows(NoSuchMethodException.class, () -> Residente.class.getMethod("getDui"));
        assertThrows(NoSuchMethodException.class, () -> Residente.class.getMethod("getTelefono"));
        assertThrows(NoSuchMethodException.class, () -> Residente.class.getMethod("getCorreo"));
        assertNull(menor.getIdMiembro());
    }

    @Test
    @DisplayName("Usuario se relaciona con Miembro por composicion, no por herencia")
    void usuarioNoHeredaDePersona() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setIdMiembro(7);
        usuario.setNombreUsuario("agonzalez");

        assertFalse(Persona.class.isAssignableFrom(Usuario.class),
            "Usuario es una credencial de acceso, no una persona: referencia al miembro por id");
        assertEquals(7, usuario.getIdMiembro(), "la relacion con el miembro es por referencia");
        assertNotNull(usuario.getNombreUsuario());
    }

    @Test
    @DisplayName("El tipo de persona tolera valores ausentes o desconocidos")
    void conversionDeTipoPersona() {
        assertEquals(Residente.TipoPersona.ADULTO, Residente.tipoDesde("ADULTO"));
        assertEquals(Residente.TipoPersona.MENOR, Residente.tipoDesde(" menor "));
        assertNull(Residente.tipoDesde(null));
        assertNull(Residente.tipoDesde("DESCONOCIDO"));
    }
}
