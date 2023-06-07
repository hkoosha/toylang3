package io.koosha.toylang.toylang3.regex

import javax.swing.SwingUtilities

fun nfaOf(
    source: String,
    display: Boolean = true,
): NfaState {

    val dotted: List<Char> = toDotted(source)
    if (display)
        println(dotted)

    val postfix: List<Char> = infixToPostFix(dotted)
    if (display)
        println(postfix)

    val nfa: NfaState = NfaState.fromPostfix(postfix)
    if (display)
        println(NfaState.graphToString(nfa))

    if (display) {
        val nfaFrame = NfaState.draw(nfa)
        SwingUtilities.invokeLater {
            nfaFrame.pack()
            nfaFrame.isVisible = true
        }
    }

    return nfa
}

fun dfaOf(
    nfa: NfaState,
    display: Boolean = true,
): DfaState {

    val dfa = DfaState.fromNfa(nfa)
    if (display)
        println(DfaState.graphToString(dfa))

    if (display) {
        val dfaFrame = DfaState.draw(dfa)
        SwingUtilities.invokeLater {
            dfaFrame.pack()
            dfaFrame.isVisible = true
        }
    }

    return dfa
}

fun nfaAndDfaOf(
    source: String,
    display: Boolean = true,
): Pair<NfaState, DfaState> {

    val nfa = nfaOf(source, display)

    if (display) {
        println("=========================================================")
        println()
        println()
    }

    val dfa = dfaOf(nfa, display)

    return nfa to dfa
}

@Suppress("SpellCheckingInspection")
fun main(args: Array<String>) {

    val valueHardcode = "aeeeed"

    // val regexHardcode = "abc(ef|klm)*ab*s|qq(op*q)|yy"
    // val regexHardcode = "abc"
    // val regexHardcode = "(ab(c|dd*)x)|a"
    val regexHardcode = "a(c|ee*)|d"

    val source =
        if (args.isNotEmpty())
            args[0]
        else
            regexHardcode

    val value =
        if (args.size > 1)
            args[1]
        else
            valueHardcode

    val dfa = nfaAndDfaOf(source)

    println(DfaState.matches(value, dfa.second))
}
