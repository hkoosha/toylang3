package io.koosha.toylang.toylang3.util


fun <T> MutableList<T>.removeFirstByOrNull(predicate: (T) -> Boolean): T? {

    val index: Int = this.indexOfFirst(predicate)

    return if (index < 0)
        null
    else
        removeAt(index)
}

fun <T> MutableList<T>.removeFirstBy(predicate: (T) -> Boolean): T =
    this.removeFirstByOrNull(predicate) ?: throw NoSuchElementException("no element with given predicate")
