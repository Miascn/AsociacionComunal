# Versiones de la aplicaciÃ³n QA

La versiÃ³n visible en la esquina inferior del menÃº proviene directamente del
paquete generado con `jpackage`. Cuando se ejecuta desde el cÃ³digo muestra
`VersiÃ³n DEV`.

Los commits que generan una entrega deben comenzar con la versiÃ³n, seguida del
tipo y del ticket:

```text
v1.4.0 fix(SCRUM-156): instalar y actualizar incrementalmente QA
```

La misma versiÃ³n debe utilizarse para construir el instalador y publicar el
manifiesto de actualizaciÃ³n del servidor.

Desde la versiÃ³n 1.3.2, QA recibe
`AsociacionComunalQA-<version>-win64.zip`, como en la versiÃ³n 1.3.0. Se extrae la
carpeta y se abre `AsociacionComunalQA.exe`; la primera ejecuciÃ³n crea un acceso
directo en el escritorio. Las entregas posteriores usan un ZIP incremental que
contiene Ãºnicamente los archivos de `app/` cuyo hash cambiÃ³ respecto al
manifiesto anterior; el runtime de Java no vuelve a descargarse.

Para conservar la reputaciÃ³n del ejecutable 1.3.1 en equipos con Smart App
Control, el paquete puente reutiliza exactamente su lanzador y runtime mediante
`-LegacyImage`; solo se reemplazan los archivos Java de `app/`.

Para construir una entrega incremental se conserva el manifiesto publicado y
se ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-qa-package.ps1 `
  -Version 1.4.1 -PreviousManifest ruta/al/update-manifest.json
```
