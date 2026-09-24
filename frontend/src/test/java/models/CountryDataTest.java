package models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CountryDataTest {

    @Test
    void testAllCountriesLoaded() {
        List<CountryData> list = CountryData.getAll();
        assertNotNull(list);
        assertTrue(list.size() >= 240, "Debe incluir todos los países del mundo");
    }

    @Test
    void testElSalvadorData() {
        CountryData sv = CountryData.findByIso2("sv");
        assertNotNull(sv);
        assertEquals("El Salvador", sv.name());
        assertEquals("+503", sv.dialCode());
    }

    @Test
    void testDialCodeLookup() {
        CountryData gt = CountryData.findByDialCode("+502");
        assertNotNull(gt);
        assertEquals("Guatemala", gt.name());
    }

    @Test
    void testNameLookup() {
        CountryData us = CountryData.findByName("Estados Unidos");
        assertNotNull(us);
        assertEquals("us", us.iso2());
        assertEquals("+1", us.dialCode());
    }
}
