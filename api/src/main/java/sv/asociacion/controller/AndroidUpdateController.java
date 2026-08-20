package sv.asociacion.controller;

import io.javalin.http.*;
import java.io.InputStream;
import java.util.Map;
import sv.asociacion.service.AndroidUpdateService;

public class AndroidUpdateController {
    private final AndroidUpdateService service;
    public AndroidUpdateController(AndroidUpdateService service) { this.service = service; }

    public void manifest(Context context) throws Exception {
        var value = service.manifest();
        if (value == null) context.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hay actualización Android publicada."));
        else context.json(value);
    }
    public void packageFile(Context context) throws Exception {
        InputStream value = service.packageStream();
        if (value == null) { context.status(HttpStatus.NOT_FOUND); return; }
        context.header("Content-Disposition", "attachment; filename=AsociacionComunalAndroid.apk")
            .contentType("application/vnd.android.package-archive").result(value);
    }
    public void publish(Context context) throws Exception {
        try {
            context.status(HttpStatus.CREATED).json(service.publish(
                required(context, "apk"), required(context, "version"), required(context, "checksum"), context.uploadedFile("notes")
            ));
        } catch (IllegalArgumentException exception) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", exception.getMessage()));
        }
    }
    private static UploadedFile required(Context context, String name) {
        UploadedFile file = context.uploadedFile(name);
        if (file == null) throw new IllegalArgumentException("Falta el archivo " + name + ".");
        return file;
    }
}
