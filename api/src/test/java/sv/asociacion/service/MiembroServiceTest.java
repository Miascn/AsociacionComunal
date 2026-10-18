package sv.asociacion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.exception.PersistenciaException;
import sv.asociacion.util.PasswordHasher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MiembroService depende de tres colaboradores con acceso a datos. Mockito los
 * reemplaza para probar las reglas sin MySQL y verificar las interacciones.
 */
@ExtendWith(MockitoExtension.class)
class MiembroServiceTest {

    @Mock private MiembroDAO miembroDAO;
    @Mock private MemberProvisioningService provisioning;
    @Mock private UsuarioDAO usuarioDAO;

    private MiembroService service;

    @BeforeEach
    void setUp() {
        service = new MiembroService(miembroDAO, provisioning, usuarioDAO);
    }

    @Test
    @DisplayName("No permite construir el servicio sin sus dependencias")
    void dependenciasObligatorias() {
        assertThrows(NullPointerException.class, () -> new MiembroService(miembroDAO, provisioning, null));
    }

    @Test
    @DisplayName("Crear un miembro con datos invalidos es error de validacion y no toca la base")
    void crearConDatosInvalidos() {
        CreateMemberRequest invalido = new CreateMemberRequest(null, null, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createMember(invalido));
        verifyNoInteractions(provisioning);
    }

    @Test
    @DisplayName("Crear un miembro con documento repetido es conflicto y no se aprovisiona la cuenta")
    void crearConDocumentoRepetido() {
        // documento, tipoDocumento, paisOrigen, nombres, apellidos, telefono, correo, idVivienda
        CreateMemberRequest req = new CreateMemberRequest("012345678", "DUI", null, "Ana", "Gomez", null, null, 1);
        when(miembroDAO.existsByDui("012345678")).thenReturn(true);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.createMember(req));

        assertTrue(e.getMessage().contains("documento"));
        verify(provisioning, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Generar credenciales para un miembro inexistente devuelve null")
    void credencialesMiembroInexistente() {
        when(miembroDAO.findById(99)).thenReturn(Optional.empty());

        assertNull(service.generarCredenciales(99));
        verifyNoInteractions(usuarioDAO);
    }

    @Test
    @DisplayName("Si el miembro ya tiene cuenta, se restablece con una clave provisional cuyo hash la verifica")
    void restableceCuentaExistente() {
        when(miembroDAO.findById(5)).thenReturn(Optional.of(miembro(5, "01234567-8")));
        Usuario existente = new Usuario();
        existente.setIdUsuario(40);
        existente.setNombreUsuario("01234567-8");
        when(usuarioDAO.findByIdMiembro(5)).thenReturn(Optional.of(existente));

        var cred = service.generarCredenciales(5);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarioDAO).resetPassword(eq(40), hash.capture(), eq(true), eq(cred.claveTemporal()));
        assertTrue(PasswordHasher.verify(cred.claveTemporal(), hash.getValue()), "se guardo el hash de la clave entregada");
        assertTrue(cred.claveTemporal().startsWith("Tmp!"));
        verify(usuarioDAO, never()).crearCuentaMiembro(anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("Si no tiene cuenta, se crea con el documento como usuario a traves del DAO (sin SQL en el servicio)")
    void creaCuentaNueva() {
        when(miembroDAO.findById(6)).thenReturn(Optional.of(miembro(6, " 09876543-2 ")));
        when(usuarioDAO.findByIdMiembro(6)).thenReturn(Optional.empty());
        when(usuarioDAO.crearCuentaMiembro(eq(6), eq("09876543-2"), anyString(), anyString())).thenReturn(true);

        var cred = service.generarCredenciales(6);

        assertEquals("09876543-2", cred.nombreUsuario());
        assertTrue(cred.requiereCambioClave());
    }

    @Test
    @DisplayName("Sin documento, el usuario se deriva del id del miembro")
    void usuarioSinDocumento() {
        assertEquals("miembro12", MiembroService.nombreUsuarioPara(miembro(12, " ")));
        assertEquals("miembro12", MiembroService.nombreUsuarioPara(miembro(12, null)));
    }

    @Test
    @DisplayName("Si la base no tiene roles para crear la cuenta, es un conflicto explicito")
    void sinRoles() {
        when(miembroDAO.findById(7)).thenReturn(Optional.of(miembro(7, "1")));
        when(usuarioDAO.findByIdMiembro(7)).thenReturn(Optional.empty());
        when(usuarioDAO.crearCuentaMiembro(anyInt(), any(), any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.generarCredenciales(7));
    }

    @Test
    @DisplayName("Un fallo de persistencia del DAO se propaga tal cual: no se convierte en exito ni en null")
    void falloDeDependenciaSePropaga() {
        when(miembroDAO.findById(8)).thenThrow(new PersistenciaException("No fue posible consultar.", null));

        assertThrows(PersistenciaException.class, () -> service.generarCredenciales(8));
    }

    @Test
    @DisplayName("Cambiar a un estado inexistente es error de validacion")
    void estadoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.changeState(1, "SUSPENDIDO"));
        verify(miembroDAO, never()).changeEstado(anyInt(), any());
    }

    @Test
    @DisplayName("Consultar credenciales sin clave provisional pendiente la genera y la persiste")
    void credencialesPendientesSinClave() {
        Usuario u = new Usuario();
        u.setIdUsuario(3);
        u.setNombreUsuario("ana");
        u.setRequiereCambioClave(true);
        when(usuarioDAO.findByIdMiembro(2)).thenReturn(Optional.of(u));

        var cred = service.getCredenciales(2);

        assertNotNull(cred.claveTemporal());
        verify(usuarioDAO).resetPassword(eq(3), anyString(), eq(true), eq(cred.claveTemporal()));
    }

    private static Miembro miembro(int id, String dui) {
        Miembro m = new Miembro();
        m.setIdMiembro(id);
        m.setDui(dui);
        m.setEstado(Miembro.Estado.ACTIVO);
        return m;
    }
}
