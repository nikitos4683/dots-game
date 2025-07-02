import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDouble(value: Double): String {
    return String.format(Locale.ENGLISH, "%.4f", value)
}

val nanosInSec: Long = TimeUnit.SECONDS.toNanos(1)