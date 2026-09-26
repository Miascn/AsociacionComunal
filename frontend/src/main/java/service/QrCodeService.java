package service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public final class QrCodeService {

    private QrCodeService() {}

    /**
     * Construye el enlace de inicio de sesión directo para la aplicación móvil Android.
     * Esquema: asociacioncomunal://login?username={user}&password={pass}
     */
    public static String buildLoginUri(String username, String password) {
        String safeUser = username != null ? URLEncoder.encode(username.trim(), StandardCharsets.UTF_8) : "";
        String safePass = password != null ? URLEncoder.encode(password.trim(), StandardCharsets.UTF_8) : "";
        return "asociacioncomunal://login?username=" + safeUser + "&password=" + safePass;
    }

    /**
     * Construye un enlace alternativo HTTPS en caso de lectores que solo admitan URLs web.
     */
    public static String buildHttpsLoginUri(String username, String password) {
        String safeUser = username != null ? URLEncoder.encode(username.trim(), StandardCharsets.UTF_8) : "";
        String safePass = password != null ? URLEncoder.encode(password.trim(), StandardCharsets.UTF_8) : "";
        return "https://asociacioncomunal.sv/login?username=" + safeUser + "&password=" + safePass;
    }

    /**
     * Genera una imagen JavaFX del código QR a partir de un texto o URI.
     */
    public static Image generateQr(String text, int width, int height) {
        if (text == null || text.isBlank() || width <= 0 || height <= 0) {
            return null;
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            WritableImage image = new WritableImage(width, height);
            PixelWriter writer = image.getPixelWriter();

            // Colores modernos: negro profundo sobre fondo blanco nítido
            Color darkColor = Color.rgb(15, 23, 42); // slate-900
            Color lightColor = Color.WHITE;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    writer.setColor(x, y, bitMatrix.get(x, y) ? darkColor : lightColor);
                }
            }

            return image;
        } catch (Exception e) {
            System.err.println("Error al generar código QR: " + e.getMessage());
            return null;
        }
    }
}
