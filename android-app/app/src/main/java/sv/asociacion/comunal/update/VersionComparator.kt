package sv.asociacion.comunal.update

object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { index ->
            val difference = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
            difference.takeIf { it != 0 }
        }?.let { it > 0 } ?: false
    }
}
