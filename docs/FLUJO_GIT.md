# Flujo de trabajo con Git

Este documento fija la estrategia de ramas y el procedimiento de integración del
proyecto. Es la referencia del equipo: si una práctica no está aquí, no es la
convención acordada.

## Ramas permanentes

| Rama | Rol |
| --- | --- |
| `main` | Versión estable y protegida. Solo recibe cambios desde `develop`. |
| `develop` | Rama de integración y **fuente de verdad del desarrollo**. Todo el trabajo se integra aquí. |

No se desarrolla directamente sobre ninguna de las dos.

Los commits directos a `main` o `develop` son **excepcionales**. Para que uno sea
admisible deben cumplirse las tres condiciones:

1. **Acordado previamente con el equipo**, no decidido sobre la marcha ni
   justificado después.
2. **Respeta las protecciones del repositorio.** Nunca se elude una protección de
   rama, ni se fuerza un push, ni se desactiva temporalmente una regla para
   poder integrar.
3. **Queda registrado**, indicando el motivo y quién lo acordó.

Si una de las tres no se cumple, el cambio va por rama y Pull Request como
cualquier otro.

## Convención de nombres

Toda rama nace desde un `develop` actualizado y sigue este formato:

```text
feat/SCRUM-###-descripcion
fix/SCRUM-###-descripcion
docs/SCRUM-###-descripcion
```

Cuando el trabajo no tiene ticket asociado —una corrección menor de
documentación, por ejemplo— se omite el código:

```text
docs/descripcion
```

**El código de Jira debe aparecer en el nombre de la rama siempre que exista
ticket.** Permite saber de un vistazo qué se está trabajando y enlazar la rama
con su criterio de aceptación sin abrir el Pull Request.

La descripción va en minúsculas, sin espacios ni tildes, separada por guiones:

```text
feat/SCRUM-291-historial-participacion
fix/SCRUM-273-voto-transaccional
docs/SCRUM-154-manual-tecnico
```

## Tamaño y alcance de una rama

Una rama resuelve **un objetivo coherente**. Cuanto más pequeña, más fácil de
revisar y menos probable que entre en conflicto con el trabajo de otra persona.

Evitar las ramas grandes con trabajo no relacionado: una rama que toca la
votación, el manual de usuario y la configuración de despliegue es imposible de
revisar con criterio y bloquea su propia integración.

**Varios tickets pueden compartir una rama únicamente si tienen la misma causa
raíz.** En ese caso, el Pull Request debe enumerarlos todos para conservar la
trazabilidad. Ejemplo: si tres tickets reportan la ausencia del mismo control de
autorización en distintos controladores, corregirlos por separado duplicaría el
trabajo y la revisión.

## Procedimiento por ticket

1. Partir de `develop` actualizado:

   ```powershell
   git switch develop
   git pull --ff-only origin develop
   ```

2. Crear la rama:

   ```powershell
   git switch -c feat/SCRUM-###-descripcion
   ```

3. Implementar el cambio y ejecutar las pruebas de los módulos afectados.

4. Revisar antes de preparar el commit:

   ```powershell
   git status
   git diff
   ```

   **No usar `git add .` sin haber mirado antes qué se está incluyendo.** Es la
   vía más habitual por la que se cuela un archivo de configuración local o un
   secreto.

5. Crear commits pequeños con prefijo:

   | Prefijo | Uso |
   | --- | --- |
   | `feat:` | Funcionalidad nueva |
   | `fix:` | Corrección de un defecto |
   | `docs:` | Documentación |
   | `test:` | Pruebas |
   | `refactor:` | Reestructuración sin cambio de comportamiento |
   | `chore:` | Mantenimiento, configuración, dependencias |

   Conviene incluir el código del ticket en el asunto:
   `fix(SCRUM-273): registrar el voto dentro de una transaccion`

6. Publicar la rama:

   ```powershell
   git push -u origin feat/SCRUM-###-descripcion
   ```

7. Abrir un Pull Request **hacia `develop`** con:

   - ticket o tickets de Jira que resuelve;
   - alcance del cambio;
   - archivos o áreas afectadas;
   - pruebas ejecutadas y su resultado;
   - pendientes conocidos, si los hay.

8. Solicitar revisión. **La integración se realiza únicamente después de aprobar
   el Pull Request.**

## Después de integrar

1. **Eliminar la rama** local y remota en cuanto el Pull Request se ha mergeado:

   ```powershell
   git switch develop
   git pull --ff-only origin develop
   git branch -d feat/SCRUM-###-descripcion
   git push origin --delete feat/SCRUM-###-descripcion
   ```

2. **No reutilizar ramas ya mergeadas.** Cada trabajo nuevo parte de una rama
   nueva desde `develop` actualizado. Retomar una rama vieja arrastra su
   historial, reintroduce commits ya integrados y produce diffs engañosos.

3. **QA se ejecuta después del merge**, sobre el ambiente DEV ya actualizado. No
   antes: lo que se valida es el estado integrado, no el de una rama aislada.

## Mergear no es cerrar

**Un merge a `develop` no pone el ticket en Done automáticamente.** Integrar
significa que el código ya está en la rama de desarrollo; no que el criterio de
aceptación esté demostrado.

Si el ticket requiere **validación funcional, visual o de integración**, queda
pendiente hasta que esa validación se realice y su resultado se registre:

| Naturaleza del criterio | Qué basta para cerrar |
| --- | --- |
| Estructural o determinista, demostrable en el código y sus pruebas | El merge, con la evidencia enlazada en el ticket |
| Formulado desde la experiencia del usuario —lo que ve, la interacción, el layout, los mensajes, el comportamiento en ejecución— | **Validación de QA sobre el ambiente DEV** |
| Que exige pruebas concretas en su criterio | Que esas pruebas existan y pasen |

Que el código parezca correcto es evidencia de implementación, no sustituto de
la validación funcional. Si QA encuentra que el defecto persiste, prevalece la
evidencia de ejecución y el ticket se reconcilia con ella.

## Ciclo completo

```text
rama → PR → develop → GitHub Actions → ambiente DEV → QA
```

Cada merge a `develop` dispara los flujos de GitHub Actions
(`.github/workflows/qa-package.yml` y `android-release.yml`), que compilan y
publican la versión que llega al ambiente DEV. QA valida sobre ese ambiente y el
ticket se cierra o se reconcilia según el resultado.

El proyecto mantiene **un único ambiente operativo (DEV)** asociado a `develop`.

## Promoción a `main`

`develop` pasa a `main` **solo cuando el equipo decide que el sprint o la
entrega están listos**. No es un paso automático ni individual.

## Reglas de seguridad

- **No versionar `.env`, tokens, contraseñas, credenciales ni configuraciones
  locales.** La configuración sensible se resuelve por variables de entorno;
  `.env.example` documenta cuáles hacen falta, sin valores reales.
- No subir direcciones IP privadas ni rutas de servidores.
- Resolver los conflictos en la rama de trabajo, nunca en `develop`.
- **No integrar código que no compile o que tenga pruebas fallidas** sin
  documentarlo explícitamente en el Pull Request y acordarlo con el equipo.
- Mantener `main` estable en todo momento.
