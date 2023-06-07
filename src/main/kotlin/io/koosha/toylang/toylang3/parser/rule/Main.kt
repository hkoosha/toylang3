package io.koosha.toylang.toylang3.parser.rule

fun main(args: Array<String>) {

    val hardCoded = """
            start           -> fn_declaration start | fn_declaration |
            fn_call         -> ID ( args ) ;
            args            -> args , arg | arg | # Left recursive on purpose to test the left recursion elimination
            arg             -> STR | INT | ID
            fn_declaration  -> FN ID ( params ) { statements }
            params          -> param , params | param |
            param           -> ID ID
            statements      -> statement , statements | statement |
            statement       -> declaration | assignment | fn_call | ret
            declaration     -> ID ID ;
            assignment      -> ID = expressions ;
            expressions     -> terms + expressions | terms - expressions | terms
            terms           -> factor * terms | factor / terms | factor
            factor          -> ( expressions ) | INT | ID
            ret             -> RETURN expressions ;
    """.trimIndent()

    val rulesDef: String =
        if (args.isEmpty())
            hardCoded
        else
            args[0]

    val rules: List<Rule> = Rule.parse(rulesDef)
    val maxLen = rules.maxBy { it.name.length }.name.length
    rules.forEach {
        println("${it.name.padEnd(maxLen)}  ===>  ${it.altsToString()}")
    }
    rules.forEach(::println)
}
