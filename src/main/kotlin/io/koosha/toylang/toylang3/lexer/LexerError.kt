package io.koosha.toylang.toylang3.lexer

class LexerError(
    msg: String,
    val position: Int,
    val line: Int,
) : RuntimeException(msg)
