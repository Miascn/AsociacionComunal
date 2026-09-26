package controller;

import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import models.MiembroModel;

public class DetalleMiembroController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label lblIniciales;
    @FXML private Label lblNombreCompleto;
    @FXML private Label lblDocumento;
    @FXML private Label lblTipoDocumento;
    @FXML private Label lblPais;
    @FXML private Label lblTelefono;
    @FXML private Label lblCorreo;
    @FXML private Label lblDireccion;
    @FXML private Label lblFechaIngreso;
    @FXML private Label lblEstado;
    @FXML private Label lblTotalAportado;
    @FXML private Label lblTotalCuotas;
    @FXML private Label lblUltimoPago;
    @FXML private Label lblSolvencia;

    // Campos de credenciales móviles y código QR
    @FXML private Label lblEstadoCredencialBadge;
    @FXML private Label lblUsuarioMovil;
    @FXML private Label lblClaveMovil;
    @FXML private Label lblExplicacionClave;
    @FXML private Button btnCopiarClave;
    @FXML private Button btnGenerarAcceso;
    @FXML private Button btnCopiarEnlaceQr;
    @FXML private Button btnAbrirModalQr;
    @FXML private VBox contenedorQr;
    @FXML private ImageView imgCodigoQr;

    private MiembroModel miembro;

    public void setMiembro(MiembroModel miembro) {
        this.miembro = miembro;
        String nombres = value(miembro.getNombres());
        String apellidos = value(miembro.getApellidos());
        lblNombreCompleto.setText((nombres + " " + apellidos).trim());
        lblIniciales.setText(inicial(nombres) + inicial(apellidos));
        lblDocumento.setText(formatearDocumento(miembro));
        lblTipoDocumento.setText(nombreTipo(miembro.getTipoDocumento()));
        lblPais.setText(valueOrDefault(miembro.getPaisOrigen(), "El Salvador"));
        lblTelefono.setText(valueOrDefault(miembro.getTelefono(), "No registrado"));
        lblCorreo.setText(valueOrDefault(miembro.getCorreo(), "No registrado"));
        lblDireccion.setText(valueOrDefault(miembro.getDireccion(), "No registrada"));
        lblFechaIngreso.setText(formatDate(miembro.getFechaIngreso()));
        lblEstado.setText(miembro.getEstado() == null ? "Sin estado" : miembro.getEstado());

        cargarAportacionesMiembro(miembro);
        cargarCredencialesMiembro(miembro);
    }

    private void cargarAportacionesMiembro(MiembroModel miembro) {
        if (lblTotalAportado != null && miembro.getIdMiembro() != null) {
            javafx.concurrent.Task<models.AportacionModel.Page> task = new javafx.concurrent.Task<>() {
                @Override
                protected models.AportacionModel.Page call() throws Exception {
                    return new service.AportacionApiClient().findPage(
                        miembro.getIdMiembro(), null, null, null, null, null, "REGISTRADA", null, 1, 10
                    );
                }
            };
            task.setOnSucceeded(e -> {
                models.AportacionModel.Page page = task.getValue();
                if (page != null) {
                    java.math.BigDecimal total = page.getTotalRecaudado() != null ? page.getTotalRecaudado() : java.math.BigDecimal.ZERO;
                    lblTotalAportado.setText(String.format("$%.2f USD", total));
                    lblTotalCuotas.setText(page.getTotal() + (page.getTotal() == 1 ? " aportación" : " aportaciones"));
                    if (page.getItems() != null && !page.getItems().isEmpty()) {
                        models.AportacionModel ultima = page.getItems().get(0);
                        lblUltimoPago.setText(ultima.getPeriodoMes() + " (" + ultima.getMontoFormateado() + ")");
                        lblSolvencia.setText("Al día");
                        lblSolvencia.setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        lblUltimoPago.setText("Sin registros");
                        lblSolvencia.setText("Sin aportaciones");
                        lblSolvencia.setStyle("-fx-text-fill: -color-fg-muted;");
                    }
                }
            });
            task.setOnFailed(e -> {
                lblTotalAportado.setText("$0.00 USD");
                lblTotalCuotas.setText("No disponible");
                lblUltimoPago.setText("—");
                lblSolvencia.setText("—");
            });
            Thread thread = new Thread(task, "detalle-miembro-aportaciones");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private String formatearDocumento(MiembroModel miembro) {
        String value = value(miembro.getDui());
        if ((miembro.getTipoDocumento() == null || "DUI".equals(miembro.getTipoDocumento()))
            && value.matches("\\d{9}")) {
            return value.substring(0, 8) + "-" + value.substring(8);
        }
        return value;
    }

    private String nombreTipo(String tipo) {
        if ("PASAPORTE".equals(tipo)) return "Pasaporte";
        if ("CARNET_RESIDENTE".equals(tipo)) return "Carnet de residente";
        return "DUI";
    }

    private String formatDate(String value) {
        if (value == null || value.isBlank()) return "No registrada";
        try { return java.time.LocalDate.parse(value).format(DATE_FORMAT); }
        catch (Exception ignored) { return value; }
    }

    private String inicial(String value) { return value.isBlank() ? "" : value.substring(0, 1).toUpperCase(); }
    private String value(String value) { return value == null ? "" : value.trim(); }
    private String valueOrDefault(String value, String fallback) { return value(value).isBlank() ? fallback : value.trim(); }

    private void cargarCredencialesMiembro(MiembroModel miembro) {
        if (lblUsuarioMovil == null || miembro.getIdMiembro() == null) return;

        javafx.concurrent.Task<service.MiembroApiClient.CredencialesMiembro> task = new javafx.concurrent.Task<>() {
            @Override
            protected service.MiembroApiClient.CredencialesMiembro call() throws Exception {
                return new service.MiembroApiClient().getCredenciales(miembro.getIdMiembro());
            }
        };

        task.setOnSucceeded(e -> {
            service.MiembroApiClient.CredencialesMiembro creds = task.getValue();
            if (creds == null) {
                mostrarSinAccesoMovil();
                return;
            }

            if (btnGenerarAcceso != null) {
                btnGenerarAcceso.setVisible(false);
                btnGenerarAcceso.setManaged(false);
            }

            String user = creds.nombreUsuario() != null ? creds.nombreUsuario() : miembro.getDocumento();
            lblUsuarioMovil.setText(user);

            boolean requiereCambio = creds.requiereCambioClave();
            String claveTemporal = creds.claveTemporal();

            if (requiereCambio) {
                // Contraseña provisional activa: permanece visible hasta que se cambie en el celular
                if (lblEstadoCredencialBadge != null) {
                    lblEstadoCredencialBadge.setText("⚠️ CLAVE PROVISIONAL");
                    lblEstadoCredencialBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 3 8; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
                }

                String claveMostrar = (claveTemporal != null && !claveTemporal.isBlank()) ? claveTemporal : "(Generada previamente)";
                lblClaveMovil.setText(claveMostrar);
                lblClaveMovil.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #b45309;");
                if (btnCopiarClave != null) {
                    boolean visible = claveTemporal != null && !claveTemporal.isBlank();
                    btnCopiarClave.setVisible(visible);
                    btnCopiarClave.setManaged(visible);
                    btnCopiarClave.setOnAction(ev -> copiarTexto(claveMostrar, btnCopiarClave, "¡Copiada!"));
                }

                if (lblExplicacionClave != null) {
                    lblExplicacionClave.setText("Esta contraseña y código QR permanecerán disponibles hasta que el miembro inicie sesión en la app móvil y cambie su contraseña.");
                }

                String uri = service.QrCodeService.buildLoginUri(user, claveTemporal != null ? claveTemporal : "");
                Image qrImage = service.QrCodeService.generateQr(uri, 90, 90);
                if (imgCodigoQr != null) imgCodigoQr.setImage(qrImage);
                if (contenedorQr != null) {
                    contenedorQr.setVisible(true);
                    contenedorQr.setManaged(true);
                }

                if (btnCopiarEnlaceQr != null) {
                    btnCopiarEnlaceQr.setVisible(true);
                    btnCopiarEnlaceQr.setManaged(true);
                    btnCopiarEnlaceQr.setDisable(false);
                    btnCopiarEnlaceQr.setOnAction(ev -> copiarTexto(uri, btnCopiarEnlaceQr, "¡Enlace copiado!"));
                }

                if (btnAbrirModalQr != null) {
                    btnAbrirModalQr.setVisible(true);
                    btnAbrirModalQr.setManaged(true);
                    btnAbrirModalQr.setOnAction(ev -> {
                        Window owner = lblUsuarioMovil.getScene() != null ? lblUsuarioMovil.getScene().getWindow() : null;
                        service.MaterialAlertService.revisarCredencialesProvisionales(
                            owner,
                            miembro.getNombreCompleto(),
                            user,
                            claveTemporal
                        );
                    });
                }
            } else {
                // Ya fue cambiada por el miembro en el celular
                if (lblEstadoCredencialBadge != null) {
                    lblEstadoCredencialBadge.setText("✓ PERSONALIZADA");
                    lblEstadoCredencialBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 3 8; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
                }

                lblClaveMovil.setText("••••••••");
                lblClaveMovil.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #15803d;");
                if (btnCopiarClave != null) {
                    btnCopiarClave.setVisible(false);
                    btnCopiarClave.setManaged(false);
                }

                if (lblExplicacionClave != null) {
                    lblExplicacionClave.setText("El miembro ya actualizó su contraseña personal desde su teléfono celular. La contraseña provisional inicial fue dada de baja por seguridad.");
                }

                // Generar QR para autocompletar solo el usuario
                String uri = service.QrCodeService.buildLoginUri(user, "");
                Image qrImage = service.QrCodeService.generateQr(uri, 90, 90);
                if (imgCodigoQr != null) imgCodigoQr.setImage(qrImage);
                if (contenedorQr != null) {
                    contenedorQr.setVisible(true);
                    contenedorQr.setManaged(true);
                }

                if (btnCopiarEnlaceQr != null) {
                    btnCopiarEnlaceQr.setVisible(true);
                    btnCopiarEnlaceQr.setManaged(true);
                    btnCopiarEnlaceQr.setDisable(false);
                    btnCopiarEnlaceQr.setOnAction(ev -> copiarTexto(uri, btnCopiarEnlaceQr, "¡Enlace copiado!"));
                }

                if (btnAbrirModalQr != null) {
                    btnAbrirModalQr.setVisible(true);
                    btnAbrirModalQr.setManaged(true);
                    btnAbrirModalQr.setOnAction(ev -> {
                        Window owner = lblUsuarioMovil.getScene() != null ? lblUsuarioMovil.getScene().getWindow() : null;
                        service.MaterialAlertService.mostrarQrAccesoMovil(
                            owner,
                            miembro.getNombreCompleto(),
                            user
                        );
                    });
                }
            }
        });

        task.setOnFailed(e -> mostrarSinAccesoMovil());

        Thread thread = new Thread(task, "detalle-miembro-credenciales");
        thread.setDaemon(true);
        thread.start();
    }

    private void mostrarSinAccesoMovil() {
        if (lblEstadoCredencialBadge != null) {
            lblEstadoCredencialBadge.setText("SIN USUARIO");
            lblEstadoCredencialBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 3 8; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
        }
        if (lblUsuarioMovil != null) lblUsuarioMovil.setText("No asignado");
        if (lblClaveMovil != null) lblClaveMovil.setText("—");
        if (btnCopiarClave != null) {
            btnCopiarClave.setVisible(false);
            btnCopiarClave.setManaged(false);
        }
        if (lblExplicacionClave != null) {
            lblExplicacionClave.setText("Este miembro no tiene un usuario vinculado actualmente. Puede generar su acceso móvil y contraseña provisional presionando el botón a continuación.");
        }
        if (contenedorQr != null) {
            contenedorQr.setVisible(false);
            contenedorQr.setManaged(false);
        }
        if (btnCopiarEnlaceQr != null) {
            btnCopiarEnlaceQr.setVisible(false);
            btnCopiarEnlaceQr.setManaged(false);
        }
        if (btnAbrirModalQr != null) {
            btnAbrirModalQr.setVisible(false);
            btnAbrirModalQr.setManaged(false);
        }
        if (btnGenerarAcceso != null) {
            btnGenerarAcceso.setVisible(true);
            btnGenerarAcceso.setManaged(true);
            btnGenerarAcceso.setDisable(false);
            btnGenerarAcceso.setText("⚡ Generar acceso móvil");
            btnGenerarAcceso.setOnAction(ev -> generarAccesoMovil());
        }
    }

    private void generarAccesoMovil() {
        if (miembro == null || miembro.getIdMiembro() == null) return;
        if (btnGenerarAcceso != null) {
            btnGenerarAcceso.setDisable(true);
            btnGenerarAcceso.setText("Generando acceso...");
        }

        javafx.concurrent.Task<service.MiembroApiClient.CredencialesMiembro> task = new javafx.concurrent.Task<>() {
            @Override
            protected service.MiembroApiClient.CredencialesMiembro call() throws Exception {
                return new service.MiembroApiClient().generarCredenciales(miembro.getIdMiembro(), miembro.getDui());
            }
        };

        task.setOnSucceeded(e -> {
            service.MiembroApiClient.CredencialesMiembro creds = task.getValue();
            if (creds != null) {
                miembro.setNombreUsuario(creds.nombreUsuario());
                miembro.setClaveTemporal(creds.claveTemporal());
                miembro.setRequiereCambioClave(creds.requiereCambioClave());

                Window owner = lblUsuarioMovil != null && lblUsuarioMovil.getScene() != null
                    ? lblUsuarioMovil.getScene().getWindow() : null;
                service.MaterialAlertService.credenciales(
                    owner,
                    creds.nombreUsuario(),
                    creds.claveTemporal()
                );

                cargarCredencialesMiembro(miembro);
            } else {
                if (btnGenerarAcceso != null) {
                    btnGenerarAcceso.setDisable(false);
                    btnGenerarAcceso.setText("⚡ Generar acceso móvil");
                }
            }
        });

        task.setOnFailed(e -> {
            if (btnGenerarAcceso != null) {
                btnGenerarAcceso.setDisable(false);
                btnGenerarAcceso.setText("⚡ Generar acceso móvil");
            }
            Throwable ex = task.getException();
            String errorMsg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Error inesperado al generar acceso.";
            Window owner = lblUsuarioMovil != null && lblUsuarioMovil.getScene() != null
                ? lblUsuarioMovil.getScene().getWindow() : null;
            service.MaterialAlertService.error(owner, "No fue posible generar el acceso", errorMsg);
        });

        Thread thread = new Thread(task, "generar-acceso-movil");
        thread.setDaemon(true);
        thread.start();
    }

    private void copiarTexto(String texto, Button btn, String confirmacion) {
        try {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(texto);
            clipboard.setContent(cc);
            String original = btn.getText();
            btn.setText(confirmacion);
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(ev -> btn.setText(original));
            pause.play();
        } catch (Exception ignored) {}
    }
}
