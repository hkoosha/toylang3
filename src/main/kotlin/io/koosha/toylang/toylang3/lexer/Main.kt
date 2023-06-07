package io.koosha.toylang.toylang3.lexer

private fun printAll(
    lexer: Lexer,
    expectFailure: Boolean = false,
) {

    if (!lexer.hasNext())
        error("empty lexer: $lexer")

    if (expectFailure) {

        while (lexer.hasNext())
            try {
                lexer.next()
            }
            catch (e: LexerError) {
                println("expected error happened")
                println("lexer: $lexer")
                println("error: $e")
                return
            }

        error("expecting error in lexer but none happened: $lexer")

    }
    else {

        while (lexer.hasNext())
            println(lexer.next())

    }

}

fun main(args: Array<String>) {

    val hardCoded = """
        fn hello (int x): String {
            int y = 0;
            int z = x;
            int xyz = y + z / 2 * 3 - 1 + 34 * (x * 24);
            
            String fin = "hello damn \"thing\"!" + xyz;
            return fin;
        }
        
        hello(2)
    """.trimIndent()

    val badProgram0 = " fn 02hello( "

    val text =
        if (args.isNotEmpty())
            args[0]
        else
            hardCoded

    println("\n\n=================")
    printAll(Lexer(text))

    println("\n\n=================")
    printAll(Lexer(badProgram0), true)
}
