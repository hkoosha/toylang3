package io.koosha.toylang.toylang3.regex

val ALL_OPS: Set<Char> = setOf('|', '*')
val BIN_OPS: Set<Char> = setOf('|')

val PRECEDENCE: Map<Char, Int> = mapOf(
    '(' to 1,
    '|' to 2,
    '.' to 3,
    '*' to 4,
)

val LOWEST_PRECEDENCE: Int = PRECEDENCE.values.asSequence().max()

fun toDotted(regex: String): List<Char> {

    if (regex.contains('.'))
        throw IllegalArgumentException("regex already contains dots: $regex")

    val list = mutableListOf<Char>()

    for (i in 0 until regex.length - 1) {
        val current: Char = regex[i]
        val next: Char = regex[i + 1]

        list.add(current)

        if (current != '(' && next != ')' && next !in ALL_OPS && current !in BIN_OPS)
            list.add('.')
    }

    if (regex.isNotEmpty())
        list.add(regex.last())

    return list
}

fun infixToPostFix(regex: List<Char>): List<Char> {

    val postfix = mutableListOf<Char>()
    val stack = ArrayDeque<Char>()

    for (c in regex)
        when (c) {
            '(' -> stack.addLast(c)
            ')' -> {
                while (stack.last() != '(')
                    postfix.add(stack.removeLast())
                stack.removeLast()
            }

            else -> {
                while (stack.isNotEmpty()) {

                    val lastPrecedence: Int = PRECEDENCE.getOrDefault(stack.last(), LOWEST_PRECEDENCE + 1)
                    val currentPrecedence: Int = PRECEDENCE.getOrDefault(c, LOWEST_PRECEDENCE + 1)

                    if (currentPrecedence <= lastPrecedence)
                        postfix.add(stack.removeLast())
                    else
                        break
                }

                stack.addLast(c)
            }
        }

    while (stack.isNotEmpty())
        postfix.add(stack.removeLast())

    return postfix
}
