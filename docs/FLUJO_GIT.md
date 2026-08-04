# Flujo de trabajo con Git

Este proyecto utiliza `develop` como rama de integración y `main` como versión
estable. Ningún desarrollo debe realizarse directamente en esas ramas.

## Ramas de trabajo

Cada ticket de Jira se desarrolla en una rama creada desde `develop`:

- Funcionalidad: `feature/<modulo>-<nombre>`
- Corrección: `fix/<descripcion>-<nombre>`

Los nombres se escriben en minúsculas, sin espacios ni tildes, y se separan con
guiones. Ejemplos:

```text
feature/miembros-josue
feature/aportaciones-gerson
fix/validacion-dui-david
```

La incidencia de Jira debe incluirse en el Pull Request y, cuando sea útil, en
el cuerpo del commit. No se incluye en el nombre de la rama para conservar la
convención oficial del equipo.

## Procedimiento por ticket

1. Actualizar la rama de integración:

   ```powershell
   git switch develop
   git pull origin develop
   ```

2. Crear una rama para el ticket:

   ```powershell
   git switch -c feature/<modulo>-<nombre>
   ```

3. Implementar el cambio y ejecutar las pruebas de los módulos afectados.
4. Crear commits pequeños usando los prefijos `feat`, `fix`, `style`,
   `refactor`, `docs`, `test` o `chore`.
5. Publicar la rama:

   ```powershell
   git push -u origin feature/<modulo>-<nombre>
   ```

6. Crear un Pull Request hacia `develop` indicando:
   - ticket de Jira;
   - funcionalidad desarrollada;
   - archivos principales modificados;
   - pasos de prueba;
   - pendientes conocidos.
7. Solicitar revisión. La integración se realiza únicamente después de aprobar
   el Pull Request.

## Reglas de seguridad

- No subir contraseñas, tokens, direcciones IP privadas, archivos `.env` ni
  configuraciones locales.
- No usar `git add .` sin revisar antes `git status` y el diff.
- Resolver los conflictos en la rama de trabajo.
- No integrar cambios que no compilen o que tengan pruebas fallidas.
- Mantener `main` estable; el paso de `develop` a `main` se realiza al finalizar
  y validar el sprint.
