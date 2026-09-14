package sv.asociacion.service;

import java.util.List;
import java.util.Objects;
import sv.asociacion.dao.DAO;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.domain.entity.Miembro;

/**
 * Consulta de solo lectura del padron de miembros.
 *
 * <p>A diferencia de {@link MiembroService}, que administra el censo y depende de
 * reglas propias de la implementacion JDBC —unicidad de DUI y el cambio de estado
 * que sincroniza {@code miembro} y {@code usuario} en una sola transaccion—, este
 * servicio se limita a lo que el contrato generico ofrece.</p>
 *
 * <p>Por eso recibe un {@link DAO} y <b>no sabe de donde vienen los datos</b>:
 * puede estar respaldado por {@code MiembroDAO} contra MySQL o por
 * {@code MiembroArchivoDAO} contra un archivo {@code .dat}. Esa es la razon de
 * ser de esta clase: el secretario necesita consultar el padron durante una
 * asamblea, y esa consulta debe funcionar igual con o sin conexion.</p>
 *
 * <p>No hay ningun {@code instanceof} ni conversion de tipo en esta clase. La
 * implementacion concreta se decide al construirla, fuera de aqui.</p>
 */
public class CensoMiembrosService {

    private final DAO<Miembro, Integer> origen;

    public CensoMiembrosService(DAO<Miembro, Integer> origen) {
        this.origen = Objects.requireNonNull(origen, "El origen de datos del censo es obligatorio.");
    }

    /** Padron completo, tal como lo entregue el origen configurado. */
    public List<MiembroResponse> findAll() {
        return origen.findAll().stream().map(MiembroResponse::from).toList();
    }

    /** Ficha de un miembro, o {@code null} si no esta en el padron. */
    public MiembroResponse findById(int id) {
        return origen.findById(id).map(MiembroResponse::from).orElse(null);
    }
}
