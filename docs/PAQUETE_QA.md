# Paquete ejecutable para QA

El paquete de Windows incluye la aplicación, Java 21 y sus dependencias. El
equipo QA no necesita instalar Java, Maven ni abrir el código fuente.

## Construcción local

Desde la raíz del repositorio:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\build-qa-package.ps1 -Version "1.0.0"
```

El resultado se guarda en `dist/AsociacionComunalQA-1.0.0-win64.zip`. QA debe
extraer el ZIP y ejecutar:

```text
AsociacionComunalQA\AsociacionComunalQA.exe
```

## Construcción automática

El flujo `Paquete QA para Windows` se ejecuta cuando se integra código en
`develop` y también puede iniciarse manualmente en GitHub Actions. Si las
pruebas fallan no se publica ningún paquete.

El ZIP generado se conserva como artefacto privado durante 14 días. Solo los
integrantes con acceso al repositorio pueden descargarlo; no deben compartirse
tokens de GitHub dentro de la aplicación.

## Actualizaciones

La fase siguiente publicará el paquete aprobado desde la laptop-servidor a
través de HTTPS. La aplicación consultará un manifiesto de versión y ofrecerá
instalar la actualización. Hasta completar esa fase, QA debe descargar el nuevo
artefacto después de cada integración aprobada en `develop`.
