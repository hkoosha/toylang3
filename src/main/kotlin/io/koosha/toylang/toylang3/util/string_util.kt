package io.koosha.toylang.toylang3.util

fun String.isInt(): Boolean = try {
    Integer.parseInt(this)
    true
}
catch (e: NumberFormatException) {
    false
}
