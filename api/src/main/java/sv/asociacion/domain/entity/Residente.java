package sv.asociacion.domain.entity;

import java.io.Serializable;

/**
 * Persona que habita una vivienda de la comunidad.
 *
 * <p>Corresponde a la tabla {@code residente_vivienda}. A diferencia del
 * {@link Miembro}, un residente puede no estar asociado: la columna
 * {@code id_miembro} es nula para hijos, inquilinos y familiares que viven en la
 * vivienda pero no tienen ficha de miembro.</p>
 *
 * <p>Por esa razon el registro guarda unicamente {@code nombre_completo} en un
 * solo campo y no exige DUI, telefono ni correo: son datos que la asociacion no
 * recoge de quien no es asociado. Esta clase refleja exactamente esas columnas,
 * sin agregar atributos que la base de datos no tiene.</p>
 */
public class Residente extends Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Clasificacion que usa el censo de viviendas para el conteo de habitantes. */
    public enum TipoPersona { ADULTO, MENOR }

    private Integer idResidente;

    /** Ficha de miembro asociada, o {@code null} si el residente no es asociado. */
    private Integer idMiembro;

    private String nombreCompleto;
    private TipoPersona tipoPersona;
    private boolean representante;

    public Residente() { }

    public Residente(Integer idResidente, Integer idVivienda, Integer idMiembro,
                     String nombreCompleto, TipoPersona tipoPersona, boolean representante) {
        super(idVivienda);
        this.idResidente = idResidente;
        this.idMiembro = idMiembro;
        this.nombreCompleto = nombreCompleto;
        this.tipoPersona = tipoPersona;
        this.representante = representante;
    }

    public Integer getIdResidente() { return idResidente; }
    public void setIdResidente(Integer idResidente) { this.idResidente = idResidente; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public TipoPersona getTipoPersona() { return tipoPersona; }
    public void setTipoPersona(TipoPersona tipoPersona) { this.tipoPersona = tipoPersona; }

    /** Representante de la vivienda ante la asociacion. */
    public boolean isRepresentante() { return representante; }
    public void setRepresentante(boolean representante) { this.representante = representante; }

    /** Un menor de edad no participa en asambleas ni vota. */
    public boolean esMenor() { return tipoPersona == TipoPersona.MENOR; }

    /** El residente ya trae su nombre en un unico campo. */
    @Override
    public String getNombreCompleto() { return nombreCompleto; }

    /** Solo es asociado si tiene ficha de miembro vinculada. */
    @Override
    public boolean esAsociado() { return idMiembro != null; }

    /** Convierte el valor textual de {@code tipo_persona} sin romper si llega vacio. */
    public static TipoPersona tipoDesde(String valor) {
        if (valor == null) return null;
        try {
            return TipoPersona.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
