# Versiones de la aplicación QA

La versión visible en la esquina inferior del menú proviene directamente del
paquete generado con `jpackage`. Cuando se ejecuta desde el código muestra
`Versión DEV`.

Los commits que generan una entrega deben comenzar con la versión, seguida del
tipo y del ticket:

```text
v1.3.1 feat(SCRUM-156): mostrar versión de la aplicación
```

La misma versión debe utilizarse para construir el ZIP y publicar el manifiesto
de actualización del servidor.
