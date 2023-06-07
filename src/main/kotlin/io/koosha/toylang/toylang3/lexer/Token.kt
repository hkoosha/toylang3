package io.koosha.toylang.toylang3.lexer

data class Token(
    val line: Int,
    val pos: Int,
    val kind: TokenKind,
    val text: String,
)
