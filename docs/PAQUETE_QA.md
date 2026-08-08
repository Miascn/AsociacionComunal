# Paquete ejecutable y actualizaciones para QA

El ZIP completo incluye la aplicación, Java 21 y todas sus dependencias. QA solo
debe extraerlo y abrir `AsociacionComunalQA.exe`. Las versiones posteriores se
descargan automáticamente como paquetes incrementales.

## Archivos locales que no se publican en Git

- `qa-local.properties`: URL pública de la API y token de QA.
- `dist/update-manifest-1.3.3.json`: manifiesto base de la versión más antigua
  que todavía puede actualizarse automáticamente.
- `dist/AsociacionComunalQA-1.3.4-base/AsociacionComunalQA`: imagen cuyo
  lanzador y runtime se reutilizan para no cambiar innecesariamente el EXE.

Nunca copies el token de `qa-local.properties` en commits, capturas o mensajes.

## 1. Preparar una versión

Trabaja en una rama `feature/SCRUM-N-descripcion`, ejecuta las pruebas y elige
una versión mayor que la publicada. Ejemplo: si está publicada `1.3.6`, usa
`1.3.7`.

El commit de entrega debe incluir la versión y el ticket:

```text
v1.3.7 feat(SCRUM-67): ajustar menú principal
```

## 2. Construir el ZIP completo y el incremental

Desde la raíz del repositorio ejecuta:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\build-qa-package.ps1 `
  -Version 1.3.7 `
  -PreviousManifest .\dist\update-manifest-1.3.3.json `
  -LegacyImage .\dist\AsociacionComunalQA-1.3.4-base\AsociacionComunalQA
```

El script compila, ejecuta las pruebas y genera:

- `dist/AsociacionComunalQA-1.3.7-win64.zip`: entrega completa para un QA nuevo.
- `dist/AsociacionComunalQA-1.3.7-delta.zip`: actualización automática.
- `dist/update-manifest.json`: versión, hash y tamaño del delta.
- `dist/version.txt` y `dist/sha256.txt`: compatibilidad con clientes antiguos.

No publiques nada si Maven informa `BUILD FAILURE`.

## 3. Revisar la estructura del delta

El ZIP incremental debe contener rutas como estas:

```text
app/app/AsociacionComunalQA.cfg
app/app/AsociacionComunal-1.0-SNAPSHOT.jar
```

La doble carpeta `app/app` es intencional: la primera es el contenedor del
actualizador y la segunda es la carpeta real dentro de la instalación.

## 4. Subir los archivos al servidor

Adapta la versión en estos comandos:

```powershell
scp -i "$env:USERPROFILE\.ssh\asociacion_comunal_ed25519" `
  .\dist\AsociacionComunalQA-1.3.7-win64.zip `
  .\dist\AsociacionComunalQA-1.3.7-delta.zip `
  .\dist\update-manifest.json `
  .\dist\version.txt `
  .\dist\sha256.txt `
  miascn@100.64.210.47:/home/miascn/apps/asociacion-api/updates/
```

Activa la nueva versión usando nombres estables:

```powershell
ssh -i "$env:USERPROFILE\.ssh\asociacion_comunal_ed25519" miascn@100.64.210.47
```

Ya dentro del servidor:

```bash
cd /home/miascn/apps/asociacion-api/updates
cp AsociacionComunalQA-1.3.7-win64.zip AsociacionComunalQA-win64.zip
cp AsociacionComunalQA-1.3.7-delta.zip AsociacionComunalQA-delta.zip
chmod 600 *.zip update-manifest.json version.txt sha256.txt
cat version.txt
exit
```

`cat version.txt` debe mostrar exactamente la versión publicada.

## 5. Probar como QA

1. Conserva una copia de una versión anterior.
2. Ábrela y confirma que aparece el aviso de actualización.
3. Acepta y observa la animación y el porcentaje.
4. Espera el reinicio automático.
5. Comprueba en la esquina inferior que aparece la nueva versión.
6. Cierra y vuelve a abrir la aplicación: no debe ofrecer la misma actualización.

Después de esta prueba ya puedes compartir el ZIP completo con QA.

## Publicación automática con GitHub

El workflow `Compilar y publicar QA` funciona de dos maneras:

- Un `push` a `develop` compila, prueba y conserva el paquete durante 30 días.
- Desde **Actions > Compilar y publicar QA > Run workflow**, marca
  **Publicar esta compilación para los QA** para compilar y activarla en el
  servidor automáticamente.

La numeración automática usa `1.4.<número de ejecución>`.

GitHub necesita estos secretos del repositorio:

- `QA_API_URL`
- `QA_API_TOKEN`

La publicación usa el endpoint autenticado
`POST /api/updates/windows/publish`. GitHub envía el paquete completo, el delta
y sus manifiestos directamente a la API. No se instala un runner de GitHub en
la laptop-servidor, por lo que el workflow no obtiene acceso para ejecutar
comandos generales en ella.
