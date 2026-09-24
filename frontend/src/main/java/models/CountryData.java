package models;

import java.io.InputStream;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Representa la información de un país para formularios internacionales:
 * código ISO2, nombre en español, código de marcación telefónica (+XXX)
 * e icono de bandera local en formato PNG.
 */
public record CountryData(String iso2, String name, String dialCode) {

    private static List<CountryData> ALL;
    private static final Map<String, Image> FLAG_CACHE = new HashMap<>();

    public static synchronized List<CountryData> getAll() {
        if (ALL == null) {
            try (InputStream is = CountryData.class.getResourceAsStream("/countries.json")) {
                if (is != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    CountryData[] array = mapper.readValue(is, CountryData[].class);
                    ALL = Collections.unmodifiableList(Arrays.asList(array));
                } else {
                    ALL = List.of();
                }
            } catch (Exception e) {
                System.err.println("Error al cargar /countries.json: " + e.getMessage());
                ALL = List.of();
            }
        }
        return ALL;
    }

    public static CountryData findByIso2(String iso2) {
        if (iso2 == null || iso2.isBlank()) return null;
        String search = iso2.trim().toLowerCase();
        for (CountryData c : getAll()) {
            if (c.iso2().equalsIgnoreCase(search)) return c;
        }
        return null;
    }

    public static CountryData findByName(String name) {
        if (name == null || name.isBlank()) return null;
        String search = name.trim().toLowerCase();
        for (CountryData c : getAll()) {
            if (c.name().equalsIgnoreCase(search)) return c;
        }
        return null;
    }

    public static CountryData findByDialCode(String dialCode) {
        if (dialCode == null || dialCode.isBlank()) return null;
        String search = dialCode.trim();
        for (CountryData c : getAll()) {
            if (c.dialCode().equals(search)) return c;
        }
        return null;
    }

    public Image getFlagImage() {
        return getFlagImage(this.iso2);
    }

    public static Image getFlagImage(String iso2) {
        if (iso2 == null || iso2.isBlank()) return null;
        String key = iso2.trim().toLowerCase();
        synchronized (FLAG_CACHE) {
            if (!FLAG_CACHE.containsKey(key)) {
                try {
                    InputStream stream = CountryData.class.getResourceAsStream("/flags/" + key + ".png");
                    if (stream != null) {
                        FLAG_CACHE.put(key, new Image(stream, 24, 18, true, true));
                    } else {
                        FLAG_CACHE.put(key, null);
                    }
                } catch (Exception e) {
                    FLAG_CACHE.put(key, null);
                }
            }
            return FLAG_CACHE.get(key);
        }
    }

    public ImageView createFlagView() {
        Image img = getFlagImage();
        if (img == null) return null;
        ImageView view = new ImageView(img);
        view.setFitWidth(20);
        view.setFitHeight(15);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    @Override
    public String toString() {
        return name;
    }
}
