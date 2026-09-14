package sv.asociacion.domain.entity;

import java.io.Serializable;

/**
 * Persona registrada en los libros de la asociacion comunal.
 *
 * <p>El sistema conoce dos clases de persona y ambas existen ya en la base de datos:
 * el {@link Miembro}, que esta asociado con derechos de aportacion y voto, y el
 * {@link Residente}, que habita una vivienda de la comunidad y puede no estar
 * asociado (hijos, inquilinos, familiares).</p>
 *
 * <p>Lo unico que ambas comparten realmente es la vivienda donde habitan y la
 * necesidad de presentarse con un nombre en listados, actas y reportes. El resto
 * de sus datos es distinto porque su papel en la asociacion es distinto: un
 * residente menor de edad no tiene DUI ni fecha de ingreso, y un miembro no tiene
 * condicion de representante de vivienda.</p>
 *
 * <p>Por eso esta clase declara un contrato minimo y deja que cada subclase
 * resuelva a su manera como se obtiene el nombre y si la persona es asociada.</p>
 */
public abstract class Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Vivienda de la comunidad donde habita la persona. Puede ser desconocida. */
    private Integer idVivienda;

    protected Persona() { }

    protected Persona(Integer idVivienda) {
        this.idVivienda = idVivienda;
    }

    public Integer getIdVivienda() { return idVivienda; }
    public void setIdVivienda(Integer idVivienda) { this.idVivienda = idVivienda; }

    /**
     * Nombre con el que la persona aparece en listados, actas de asamblea y
     * reportes. Cada subclase lo resuelve segun como almacena sus datos.
     */
    public abstract String getNombreCompleto();

    /**
     * Indica si la persona tiene ficha de miembro de la asociacion y, por tanto,
     * derechos de aportacion y voto. Permite distinguir, dentro de una misma
     * vivienda, quien es asociado y quien solo habita en ella.
     */
    public abstract boolean esAsociado();

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
