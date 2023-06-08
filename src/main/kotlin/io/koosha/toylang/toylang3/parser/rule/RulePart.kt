package io.koosha.toylang.toylang3.parser.rule

import io.koosha.toylang.toylang3.lexer.TokenKind

class RulePart(
    val rule: Rule?,
    val tokenKind: TokenKind?,
) {

    companion object {

        fun of(tokenKind: TokenKind): RulePart =
            RulePart(rule = null, tokenKind = tokenKind)

        fun of(rule: Rule): RulePart =
            RulePart(rule = rule, tokenKind = null)

    }

    init {
        if (rule != null && tokenKind != null)
            error("can not set rule and token at the same time")

        if (rule == null && tokenKind == null)
            error("rule and token can not be null at the same time")
    }

    fun isToken(): Boolean =
        this.tokenKind != null

    fun name(): String =
        this.rule?.name ?: this.tokenKind!!.name

    fun repr(): String =
        this.rule?.name ?: this.tokenKind!!.repr ?: this.tokenKind!!.name

    fun first(): Set<RulePart> =
        if (this.isToken())
            setOf(this)
        else
            this.rule!!.first

    override fun toString(): String {

        val type: String =
            if (isToken())
                "token"
            else
                "rule"

        return "RulePart(${type}=${this.repr()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (javaClass != other?.javaClass)
            return false

        other as RulePart

        return this.tokenKind == other.tokenKind &&
                this.rule == other.rule
    }

    override fun hashCode(): Int =
        if (this.isToken())
            this.tokenKind!!.hashCode()
        else
            this.rule!!.hashCode()

}
