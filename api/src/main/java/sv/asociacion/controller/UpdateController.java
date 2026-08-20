package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import sv.asociacion.service.UpdateService;
import java.io.InputStream;
import java.util.Map;

public class UpdateController {
    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    public void manifestV1(Context context) throws Exception {
        Map<String, Object> manifest = updateService.getManifestV1();
        if (manifest == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay actualización publicada."));
            return;
        }
        context.json(manifest);
    }

    public void manifestV2(Context context) throws Exception {
        Map<String, Object> manifest = updateService.getManifestV2();
        if (manifest == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay actualización publicada."));
            return;
        }
        context.json(manifest);
    }

    public void buildManifest(Context context) throws Exception {
        InputStream is = updateService.getBuildManifest();
        if (is == null) {
            context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay manifiesto publicado."));
            return;
        }
        context.contentType("application/json").result(is);
    }

    public void legacyPackage(Context context) throws Exception {
        InputStream is = updateService.getLegacyPackage();
        if (is == null) {
            context.status(HttpStatus.NOT_FOUND);
            return;
        }
        context.header("Content-Disposition", "attachment; filename=AsociacionComunalQA-win64.zip");
        context.contentType("application/zip").result(is);
    }

    public void deltaPackage(Context context) throws Exception {
        InputStream is = updateService.getDeltaPackage();
        if (is == null) {
            context.status(HttpStatus.NOT_FOUND);
            return;
        }
        context.header("Content-Disposition", "attachment; filename=AsociacionComunalQA-delta.zip");
        context.contentType("application/zip").result(is);
    }

    public void publish(Context context) throws Exception {
        UploadedFile full = requiredUpload(context, "full");
        UploadedFile delta = requiredUpload(context, "delta");
        UploadedFile manifest = requiredUpload(context, "manifest");
        UploadedFile version = requiredUpload(context, "version");
        UploadedFile checksum = requiredUpload(context, "checksum");

        try {
            Map<String, Object> result = updateService.publish(full, delta, manifest, version, checksum);
            context.status(HttpStatus.CREATED).json(result);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    private static UploadedFile requiredUpload(Context context, String name) {
        UploadedFile file = context.uploadedFile(name);
        if (file == null) throw new IllegalArgumentException("Falta el archivo " + name + ".");
        return file;
    }
}