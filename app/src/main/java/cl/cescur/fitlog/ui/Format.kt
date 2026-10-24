package cl.cescur.fitlog.ui

import java.util.Locale

// Números con el separador decimal del idioma del móvil (coma en español, punto en inglés).

fun formatKcal(v: Double?): String = v?.let { String.format(Locale.getDefault(), "%.1f kcal", it) } ?: "–"

fun formatKm(m: Int?): String = m?.let { String.format(Locale.getDefault(), "%.2f km", it / 1000.0) } ?: "–"

/** Valor inicial de un campo editable: 74.25 → "74,25" en español. */
fun formatDecimalInput(v: Double, digits: Int? = null): String {
    val plain = if (digits != null) String.format(Locale.ROOT, "%.${digits}f", v) else v.toBigDecimal().stripTrailingZeros().toPlainString()
    val separator = java.text.DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    return plain.replace('.', separator)
}
