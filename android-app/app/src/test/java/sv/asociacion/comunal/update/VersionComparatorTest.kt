package sv.asociacion.comunal.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test fun detectsPatchUpdate() = assertTrue(VersionComparator.isNewer("0.2.1", "0.2.0"))
    @Test fun detectsMajorUpdate() = assertTrue(VersionComparator.isNewer("1.0.0", "0.9.99"))
    @Test fun rejectsSameVersion() = assertFalse(VersionComparator.isNewer("0.2.0", "0.2.0"))
    @Test fun rejectsOlderVersion() = assertFalse(VersionComparator.isNewer("0.1.9", "0.2.0"))
}
