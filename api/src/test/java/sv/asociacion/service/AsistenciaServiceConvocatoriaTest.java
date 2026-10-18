package sv.asociacion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.asociacion.dao.AsistenciaDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.dto.ConvocatoriaMasivaRequest;
import sv.asociacion.domain.dto.ResultadoConvocatoria;
import sv.asociacion.domain.entity.Reunion;
import sv.asociacion.exception.ConvocatoriaIncompletaException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reglas de AsistenciaService alrededor de la convocatoria, con sus cuatro colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceConvocatoriaTest {

    @Mock private AsistenciaDAO asistenciaDAO;
    @Mock private ReunionDAO reunionDAO;
    @Mock private MiembroDAO miembroDAO;
    @Mock private ConvocatoriaConcurrenteService convocatoria;

    private AsistenciaService service;

    @BeforeEach
    void setUp() {
        service = new AsistenciaService(asistenciaDAO, reunionDAO, miembroDAO, convocatoria);
    }

    @Test
    @DisplayName("Reunion inexistente: validacion y el proceso concurrente no se inicia")
    void reunionInexistente() {
        when(reunionDAO.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.convocarConResultado(1, null));
        verifyNoInteractions(convocatoria);
    }

    @Test
    @DisplayName("Reunion cancelada: conflicto y el proceso concurrente no se inicia")
    void reunionCancelada() {
        when(reunionDAO.findById(1)).thenReturn(Optional.of(reunion(Reunion.Estado.CANCELADA)));
        assertThrows(IllegalStateException.class, () -> service.convocarConResultado(1, null));
        verify(convocatoria, never()).convocar(anyInt(), any());
    }

    @Test
    @DisplayName("Delega en el proceso concurrente con la lista indicada")
    void delegaConLista() {
        when(reunionDAO.findById(1)).thenReturn(Optional.of(reunion(Reunion.Estado.PROGRAMADA)));
        ResultadoConvocatoria esperado = new ResultadoConvocatoria(1, 2, 2, 0, 1, 0, 1, 5, List.of());
        when(convocatoria.convocar(1, List.of(10, 20))).thenReturn(esperado);

        assertSame(esperado, service.convocarConResultado(1, new ConvocatoriaMasivaRequest(List.of(10, 20))));
    }

    @Test
    @DisplayName("Sin lista explicita convoca a todos los activos (null al proceso concurrente)")
    void sinListaConvocaATodos() {
        when(reunionDAO.findById(1)).thenReturn(Optional.of(reunion(Reunion.Estado.PROGRAMADA)));
        when(convocatoria.convocar(1, null)).thenReturn(new ResultadoConvocatoria(1, 0, 0, 0, 0, 0, 0, 0, List.of()));

        service.convocarMiembros(1, null);

        verify(convocatoria).convocar(1, null);
        verify(asistenciaDAO).findByReunion(1);
    }

    @Test
    @DisplayName("Una convocatoria incompleta se propaga y no se devuelve una lista como si hubiera terminado")
    void incompletaSePropaga() {
        when(reunionDAO.findById(1)).thenReturn(Optional.of(reunion(Reunion.Estado.PROGRAMADA)));
        ResultadoConvocatoria parcial = new ResultadoConvocatoria(1, 50, 25, 0, 2, 1, 2, 9, List.of(1));
        when(convocatoria.convocar(1, null)).thenThrow(new ConvocatoriaIncompletaException(parcial, null));

        assertThrows(ConvocatoriaIncompletaException.class, () -> service.convocarMiembros(1, null));
        verify(asistenciaDAO, never()).findByReunion(anyInt());
    }

    private static Reunion reunion(Reunion.Estado estado) {
        return new Reunion(1, "Asamblea", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, estado);
    }
}
