package io.koosha.toylang.toylang3.lexer

enum class TokenKind(
    val repr: kotlin.String?,
) {

    Fn("fn"),
    Colon(":"),

    LParen("("),
    RParen(")"),
    LBracket("["),
    RBracket("]"),
    LCurly("{"),
    RCurly("}"),

    Int(null),
    Str(null),
    Id(null),

    Return("return"),
    Semicolon(";"),
    Comma(","),
    Equal("="),
    Slash("/"),
    Star("*"),
    Minus("-"),
    Plus("+"),

    Eof(null),

    Epsilon(null),
    Error(null),

    ;

    fun isKeyword(): Boolean =
        this.repr != null

    fun isEpsilon(): Boolean =
        this == Epsilon

    fun isEof(): Boolean =
        this == Eof

    companion object {

        fun fromReprElseNull(repr: kotlin.String): TokenKind? = when (repr) {
            "fn" -> Fn
            ":" -> Colon

            "(" -> LParen
            ")" -> RParen
            "[" -> LBracket
            "]" -> RBracket
            "{" -> LCurly
            "}" -> RCurly

            "return" -> Return
            ";" -> Semicolon
            "," -> Comma
            "=" -> Equal
            "/" -> Slash
            "*" -> Star
            "-" -> Minus
            "+" -> Plus

            else -> null
        }

        fun fromRepr(repr: kotlin.String): TokenKind =
            this.fromReprElseNull(repr) ?: error("unknown token representation: $repr")

        fun fromReprOrEpsilon(repr: kotlin.String): TokenKind =
            this.fromReprOrEpsilonElseNull(repr) ?: error("unknown token representation: $repr")

        fun fromReprOrEpsilonElseNull(repr: kotlin.String): TokenKind? {

            return if (repr.isEmpty())
                Epsilon
            else
                this.fromReprElseNull(repr)
        }

    }

}
