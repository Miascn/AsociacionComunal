# Encapsulamiento — evidencia en el código

> **Avance 2 de Programación II · Criterio 2 (0.70 pts) · SCRUM-317**
> Evidencia tomada de la rama `isolated/advance2-compliance-and-fixes`.
> Este documento describe el código real del proyecto. No contiene definiciones de manual.

---

## Resumen medible

| Medida | Valor |
|---|---:|
| Clases de entidad en `domain/entity/` | 17 |
| Campos `private` en esas clases | **106** |
| Campos `public` en esas clases | **0** |

Ningún dato del dominio es accesible directamente desde fuera de su clase. Todo pasa por métodos.

---

## 1. El estado nunca se expone: `Miembro`

`api/src/main/java/sv/asociacion/domain/entity/Miembro.java`

Los doce atributos del miembro son privados y solo se alcanzan por sus accesores:

```java
private Integer idMiembro;
private String dui;
private String tipoDocumento;
private String nombres;
private String apellidos;
private LocalDate fechaIngreso;
private Estado estado;
```

**Qué protege:** que nadie escriba `miembro.estado = "activo"` con un texto arbitrario, ni deje el DUI en un formato inesperado desde fuera de la clase.

### El estado es un `enum`, no un `String` suelto

```java
public enum Estado { ACTIVO, INACTIVO }
```

Un miembro **no puede estar en un estado inventado**. Si el estado fuera `String`, `"Activo"`, `"ACITVO"` o `""` compilarían igual y el error aparecería en producción. Con el `enum`, el compilador lo impide.

`Usuario` (línea 7) aplica la misma idea con tres valores: `ACTIVO`, `BLOQUEADO`, `INACTIVO`.

### Comportamiento derivado, no un campo más

```java
@Override
public String getNombreCompleto() {
    String primeros = nombres == null ? "" : nombres.trim();
    String ultimos  = apellidos == null ? "" : apellidos.trim();
    return (primeros + " " + ultimos).trim();
}

public boolean estaActivo() { return estado == Estado.ACTIVO; }
```

El nombre completo **no se almacena**: se calcula. Así no puede quedar desincronizado respecto a `nombres` y `apellidos`. Y `estaActivo()` evita que cada punto del sistema repita la comparación `estado == ACTIVO`, que es donde se cuelan los errores.

---

## 2. La clase base controla lo que sus hijas pueden hacer: `Persona`

`api/src/main/java/sv/asociacion/domain/entity/Persona.java`

```java
public abstract class Persona implements Serializable {
    private Integer idVivienda;                       // línea 27

    protected Persona() { }                           // línea 29
    protected Persona(Integer idVivienda) { ... }     // línea 31

    public abstract String getNombreCompleto();       // línea 42
    public abstract boolean esAsociado();             // línea 49
}
```

Tres decisiones de encapsulamiento en una sola clase:

1. **`idVivienda` es privado incluso para las subclases.** `Miembro` y `Residente` heredan el dato pero deben usar `getIdVivienda()` / `setIdVivienda()`. No pueden manipular el campo por dentro.
2. **Los constructores son `protected`.** `Persona` no se puede instanciar desde fuera: no existe "una persona" suelta en el padrón, siempre es un miembro o un residente.
3. **El contrato es abstracto.** La clase base obliga a que cada subclase responda *cómo se llama* y *si es asociada*, sin imponerle cómo guardarlo.

---

## 3. Validación dentro del objeto: `Residente`

`api/src/main/java/sv/asociacion/domain/entity/Residente.java`

```java
public enum TipoPersona { ADULTO, MENOR }             // línea 23

private Integer idMiembro;                            // línea 28, puede ser null
private TipoPersona tipoPersona;                      // línea 31

public boolean esMenor()    { return tipoPersona == TipoPersona.MENOR; }   // línea 62
public boolean esAsociado() { return idMiembro != null; }                  // línea 70

public static TipoPersona tipoDesde(String valor) {   // línea 73
    if (valor == null) return null;
    try { return TipoPersona.valueOf(valor.trim().toUpperCase()); }
    catch (IllegalArgumentException ignored) { return null; }
}
```

**`esAsociado()` es la regla de negocio encapsulada.** La columna `residente_vivienda.id_miembro` es nullable: un hijo o un inquilino vive en la vivienda sin ser asociado. En lugar de que cada pantalla y cada servicio repitan `residente.getIdMiembro() != null`, la pregunta se le hace al objeto.

**`tipoDesde()` es una fábrica tolerante.** La base guarda `tipo_persona` como texto. Este método es el único punto donde ese texto entra al modelo, y absorbe valores nulos o desconocidos sin reventar. El resto del sistema ya solo trabaja con el `enum`.

---

## 4. Encapsulamiento a nivel de clase: `PasswordHasher`

`api/src/main/java/sv/asociacion/util/PasswordHasher.java`

```java
private static final int SALT_LENGTH = 16;            // línea 12
private static final int ITERATIONS  = 120_000;       // línea 13
private static final int KEY_LENGTH  = 256;           // línea 14

private PasswordHasher() {}                           // línea 16

public  static String  hash(String password)                       // línea 18
public  static boolean verify(String password, String stored)      // línea 24

private static boolean verifyLegacy(...)              // línea 32
private static boolean verifyModular(...)             // línea 45
private static String  pbkdf2(...)                    // línea 61
private static byte[]  generateSalt()                 // línea 72
```

De siete métodos, **solo dos son públicos**. El algoritmo, el número de iteraciones, la generación de sal y el manejo de los dos formatos de hash quedan encerrados.

**Qué protege:** nadie fuera de la clase puede equivocarse al construir un hash. Si mañana se cambia PBKDF2 por otro algoritmo, no hay una sola línea que tocar fuera de este archivo. Y el constructor privado impide instanciarla: es una utilidad, no un objeto.

En la entidad `Usuario` el campo se llama `claveHash` (línea 13), no `clave`. **El objeto nunca llega a tener la contraseña en claro.**

---

## 5. Acceso controlado que falla a tiempo: `SessionManager`

`frontend/src/main/java/security/SessionManager.java`

```java
private static final SessionManager INSTANCE = new SessionManager();  // línea 9
private AuthUser currentUser;                                         // línea 11
private String jwtToken;                                              // línea 12
private SessionManager() { }                                          // línea 14

public Optional<AuthUser> getCurrentUser() { ... }                    // línea 26

public String requireToken() {                                        // línea 34
    if (jwtToken == null || jwtToken.isBlank()) {
        throw new IllegalStateException("No hay un token de sesión activo.");
    }
    return jwtToken;
}

public void clear() { currentUser = null; jwtToken = null; }           // línea 45
```

El token de sesión **nunca se entrega crudo**. `requireToken()` falla de inmediato con un mensaje claro si no hay sesión, en vez de devolver `null` y provocar un `NullPointerException` tres capas más abajo. Y `getCurrentUser()` devuelve `Optional`, que obliga a quien llama a considerar el caso de que no haya nadie autenticado.

`clear()` es el único modo de cerrar sesión: no se puede dejar el objeto a medias, con usuario pero sin token.

---

## 6. Validación antes de construir: `CreateMemberRequest`

`api/src/main/java/sv/asociacion/domain/dto/CreateMemberRequest.java`, línea 13

```java
public String validationError() {
    if (!type.matches("DUI|PASAPORTE|CARNET_RESIDENTE")) return "El tipo de documento no es válido.";
    if ("DUI".equals(type) && !document.matches("\\d{9}")) return "El DUI debe contener 9 dígitos.";
    if (idVivienda == null || idVivienda <= 0) return "La vivienda es obligatoria.";
    if (correo != null && !correo.isBlank()
        && !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "El correo electrónico no es válido.";
    return null;
}
```

El `record` es inmutable y **lleva sus propias reglas**. `MiembroService.createMember()` lo primero que hace es preguntarle si es válido; si no lo es, ningún dato llega a la base. La validación viaja con el dato, no dispersa por los controladores.

---

## 7. Encapsulamiento en la capa de datos: `AbstractArchivoDAO`

`api/src/main/java/sv/asociacion/dao/archivo/AbstractArchivoDAO.java`

```java
private   final Path archivo;                                  // línea 41

protected abstract ID   idDe(T entidad);                       // línea 52
protected abstract void asignarIdentidad(T e, List<T> lista);  // línea 61

protected final List<T> leerTodo();                            // línea 135
protected final void    escribirTodo(List<T> registros);       // línea 153

public final Path    getArchivo();                             // línea 167
public final boolean existeArchivo();                          // línea 170
public final long    count();                                  // línea 173
```

Tres niveles de visibilidad, cada uno elegido:

- **`private`** la ruta del archivo: las subclases la reciben por constructor y no pueden cambiarla después.
- **`protected final`** la lectura y escritura: las subclases las **reutilizan** pero **no pueden redefinirlas**. Así ninguna implementación concreta rompe el formato del archivo.
- **`protected abstract`** los dos puntos de extensión: lo único que cada subclase debe aportar.

---

## 8. La dependencia tampoco se filtra: `CensoMiembrosService`

`api/src/main/java/sv/asociacion/service/CensoMiembrosService.java`, línea 28

```java
private final DAO<Miembro, Integer> origen;
```

No hay `getOrigen()`. Quien usa el servicio **no puede averiguar** si detrás hay MySQL o un archivo `.dat`, ni alcanzar el DAO para saltarse la capa de servicio. La prueba `CensoMiembrosServiceTest.sinCastsNiInstanceof()` verifica justamente que ningún método devuelva el `DAO`.

---

## Para la defensa

Si preguntan por encapsulamiento, el recorrido más corto y sólido es:

1. **`Miembro`** — abrir el archivo: doce campos privados, cero públicos, y el `enum Estado` que hace imposible un estado inválido.
2. **`PasswordHasher`** — cinco de siete métodos privados y constructor privado: el algoritmo de contraseñas está encerrado y `Usuario` solo conoce `claveHash`.
3. **`SessionManager.requireToken()`** — falla con un mensaje claro en vez de devolver `null`.

Si piden un ejemplo de encapsulamiento que **proteja una regla de negocio**, el mejor es `Residente.esAsociado()`: la regla "es asociado si tiene ficha de miembro" vive en un solo lugar.

---

## Nota de alcance

**No se modificó ninguna clase para producir esta evidencia.** El encapsulamiento ya estaba correctamente implementado; este documento solo lo localiza y explica. Las únicas clases nuevas citadas (`Persona`, `Residente`, `AbstractArchivoDAO`, `CensoMiembrosService`) se crearon en SCRUM-313, SCRUM-39 y SCRUM-314, con sus propios criterios.
