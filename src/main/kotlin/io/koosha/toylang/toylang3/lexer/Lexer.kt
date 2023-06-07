package io.koosha.toylang.toylang3.lexer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Lexer(
    private val text: String,
) : Iterator<Token> {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    private var error: String? = null
    private var bufferStart: Int = 0
    private var bufferEnd: Int = 0
    private var line: Int = 1
    private var pos: Int = 0
    private var linePos = 1
    private var inEscape: Boolean = false
    private var tokenKind: TokenKind = TokenKind.Error

    private fun skipWhiteSpace() {

        var count = 0
        while (this.hasNext() && this.text[this.pos] == ' ') {
            this.linePos++
            this.pos++
            count++
        }

        log.trace("skipped whitespaces, count={}", count)
    }

    private fun advanceBuffer() {

        this.bufferEnd++
        this.pos++
        this.linePos++
    }

    private fun startBuffer() {

        this.bufferStart = this.pos
        this.bufferEnd = this.pos
    }

    private fun bufferedText(): String {

        if (this.bufferStart == this.bufferEnd)
            throw LexerError(
                msg = "buffer empty",
                position = this.linePos,
                line = this.line,
            )

        return this.text.substring(this.bufferStart, this.bufferEnd)
    }


    private fun scanNumber() {

        while (this.hasNext())
            when (this.text[this.pos]) {
                in '0'..'9' -> this.advanceBuffer()

                '_',
                in 'a'..'z',
                in 'A'..'Z',
                -> {
                    this.error =
                        "unexpected character, line=${this.line}, pos=${this.pos}, character=${this.text[this.pos]}"
                    throw LexerError(
                        msg = this.error!!,
                        position = this.linePos,
                        line = this.line,
                    )
                }

                else -> return
            }
    }

    private fun scan() {
        this.advanceBuffer()

        while (this.hasNext())
            when (this.text[this.pos]) {
                in 'a'..'z',
                in 'A'..'Z',
                in '0'..'9',
                '_',
                -> this.advanceBuffer()

                else -> break
            }

        this.tokenKind = TokenKind.fromReprElseNull(bufferedText()) ?: TokenKind.Id
    }

    private fun scanString() {
        this.inEscape = false

        val start = this.linePos
        this.advanceBuffer()

        // Skip initial double quotation character.
        this.startBuffer()

        while (this.hasNext())
            when (this.text[this.pos]) {
                '"' -> {
                    if (this.inEscape) {
                        this.advanceBuffer()
                        this.inEscape = false
                    }
                    else {
                        this.pos++
                        return
                    }
                }

                '\\' -> {
                    this.inEscape = !this.inEscape
                    this.advanceBuffer()
                }

                '\n' -> {
                    this.line++
                    this.inEscape = false
                    this.advanceBuffer()
                    this.linePos = 1
                }

                else -> {
                    this.inEscape = false
                    this.advanceBuffer()
                }
            }

        this.error =
            "unterminated string, line=${this.line}, pos=${this.pos}"
        throw LexerError(
            msg = this.error!!,
            position = start,
            line = this.line,
        )
    }


    private fun readNext(): Boolean? {

        this.startBuffer()

        if (this.hasNext())
            when (this.text[this.pos]) {
                ' ' -> {
                    this.skipWhiteSpace()
                    return false
                }

                '\n' -> {
                    this.line++
                    this.pos++
                    this.linePos = 1
                    return false
                }

                '_',
                in 'a'..'z',
                in 'A'..'Z',
                -> {
                    this.scan()
                    return true
                }

                in '0'..'9' -> {
                    this.scanNumber()
                    this.tokenKind = TokenKind.Int
                    return true
                }

                '"' -> {
                    this.scanString()
                    this.tokenKind = TokenKind.Str
                    return true
                }

                ',' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Comma
                    return true
                }

                ';' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Semicolon
                    return true
                }

                ':' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Colon
                    return true
                }

                '(' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.LParen
                    return true
                }

                ')' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.RParen
                    return true
                }

                '[' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.LBracket
                    return true
                }

                ']' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.RBracket
                    return true
                }

                '{' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.LCurly
                    return true
                }

                '}' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.RCurly
                    return true
                }

                '/' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Slash
                    return true
                }

                '+' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Plus
                    return true
                }

                '-' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Minus
                    return true
                }

                '*' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Star
                    return true
                }

                '=' -> {
                    this.advanceBuffer()
                    this.tokenKind = TokenKind.Equal
                    return true
                }

                else -> {
                    this.error =
                        "unexpected character, line=${this.line}, pos=${this.pos}, character=${this.text[this.pos]}"
                    throw LexerError(
                        msg = this.error!!,
                        position = this.linePos,
                        line = this.line,
                    )
                }
            }

        if (this.bufferStart != this.bufferEnd) {
            this.error =
                "left over in buffer, line=${this.line}, pos=${this.pos}"
            throw LexerError(
                msg = this.error!!,
                position = this.linePos,
                line = this.line,
            )
        }

        return null
    }


    override fun hasNext(): Boolean =
        this.error == null && this.pos < this.text.length

    override fun next(): Token {

        if (this.error != null)
            throw LexerError(
                msg = this.error!!,
                position = this.linePos,
                line = this.line,
            )

        while (true)
            when (this.readNext()) {
                false ->
                    log.trace("got skipper at pos={} line={}", this.linePos, this.line)

                true -> {
                    log.trace(
                        "got token at pos={} line={}, kind={} text={}",
                        this.linePos,
                        this.line,
                        this.tokenKind,
                        this.bufferedText(),
                    )

                    return Token(
                        line = this.line,
                        pos = this.linePos,
                        text = if (this.tokenKind == TokenKind.Str)
                            this.bufferedText().replace("\\\"", "\"")
                        else
                            this.bufferedText(),
                        kind = this.tokenKind,
                    )
                }

                null -> {
                    log.trace("fin at pos={} line={}", this.linePos, this.line)

                    return Token(
                        line = this.line,
                        pos = this.linePos,
                        text = "",
                        kind = TokenKind.Eof,
                    )
                }
            }
    }

    override fun toString(): String =
        "Lexer(" +
                "text='$text', " +
                "error=$error, " +
                "bufferStart=$bufferStart, " +
                "bufferEnd=$bufferEnd, " +
                "line=$line, " +
                "pos=$pos, " +
                "linePos=$linePos, " +
                "inEscape=$inEscape, " +
                "tokenKind=$tokenKind" +
                ")"

}
