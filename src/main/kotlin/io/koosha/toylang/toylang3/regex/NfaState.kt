package io.koosha.toylang.toylang3.regex

import io.koosha.toylang.toylang3.regex.ui.GraphJFrame
import io.koosha.toylang.toylang3.regex.ui.LabeledEdge
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import javax.swing.JFrame


data class NfaState(
    val num: Int,
    val transition: MutableMap<Char?, MutableSet<NfaState>> = mutableMapOf(),
    var finalState: Boolean = false,
    var startState: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (other !is NfaState)
            return false

        return this.num == other.num
    }

    override fun hashCode(): Int =
        this.num.hashCode()

    override fun toString(): String {
        val sb = StringBuilder()
        stateToString(this, sb)
        return sb.toString()
    }

    companion object {

        private fun stateToString(
            s: NfaState,
            sb: StringBuilder,
        ) {

            sb.append("[")
                .append(s.num)
                .append("]======\n")

            for ((char, transitions) in s.transition)
                for (transition in transitions)
                    sb.append(char ?: "ε")
                        .append(" => ")
                        .append(transition.num)
                        .append('\n')
        }

        private fun graphToString(
            s: NfaState,
            seen: MutableSet<NfaState>,
            sb: StringBuilder,
        ) {

            if (!seen.add(s))
                return

            stateToString(s, sb)

            sb.append("\n")

            for ((_, transitions) in s.transition)
                for (t in transitions)
                    graphToString(t, seen, sb)
        }

        fun graphToString(
            s: NfaState,
        ): String {
            val sb = StringBuilder()
            graphToString(s, mutableSetOf(), sb)
            return sb.toString()
        }

        fun fromPostfix(postfix: List<Char>): NfaState {

            var n = 0

            val stack = mutableListOf<Pair<NfaState, NfaState>>()
            val debug = mutableListOf<String>()

            for (char in postfix) {

                when (char) {
                    '.' -> {

                        val (rightS, rightE) = stack.removeLast()
                        val (leftS, leftE) = stack.removeLast()

                        if (!leftE.transition.containsKey(null))
                            leftE.transition[null] = mutableSetOf()
                        leftE.transition[null]!!.add(rightS)

                        stack.add(leftS to rightE)

                        val b = debug.removeLast()
                        val a = debug.removeLast()
                        debug.add("($a.$b)")
                    }

                    '*' -> {

                        val s = NfaState(num = n++)
                        val e = NfaState(num = n++)

                        val (leftS, leftE) = stack.removeLast()

                        if (!leftE.transition.containsKey(null))
                            leftE.transition[null] = mutableSetOf()
                        if (!s.transition.containsKey(null))
                            s.transition[null] = mutableSetOf()

                        leftE.transition[null]!!.add(leftS)
                        leftE.transition[null]!!.add(e)
                        s.transition[null]!!.add(leftS)
                        s.transition[null]!!.add(e)

                        stack.add(s to e)

                        debug.add("(" + debug.removeLast() + "*)")
                    }

                    '|' -> {

                        val s = NfaState(num = n++)
                        val e = NfaState(num = n++)

                        val (rightS, rightE) = stack.removeLast()
                        val (leftS, leftE) = stack.removeLast()

                        if (!rightE.transition.containsKey(null))
                            rightE.transition[null] = mutableSetOf()
                        if (!leftE.transition.containsKey(null))
                            leftE.transition[null] = mutableSetOf()
                        if (!s.transition.containsKey(null))
                            s.transition[null] = mutableSetOf()

                        s.transition[null]!!.add(leftS)
                        s.transition[null]!!.add(rightS)
                        leftE.transition[null]!!.add(e)
                        rightE.transition[null]!!.add(e)

                        stack.add(s to e)

                        val b = debug.removeLast()
                        val a = debug.removeLast()
                        debug.add("($a|$b)")
                    }

                    else -> {

                        val s = NfaState(num = n++)
                        val e = NfaState(num = n++)

                        if (!s.transition.containsKey(char))
                            s.transition[char] = mutableSetOf()

                        s.transition[char]!!.add(e)

                        stack.add(s to e)
                        debug.add(char.toString())
                    }
                }
            }

            if (stack.size != 1)
                throw IllegalArgumentException("bad stack => $stack")

            val (s, e) = stack.removeLast()

            s.startState = true
            e.finalState = true

            return s
        }

        fun draw(nfa: NfaState): JFrame {

            val graph = DefaultDirectedGraph<Int, LabeledEdge>(LabeledEdge::class.java)

            val end: Int? = this.addVertexes(nfa, graph)
            this.addEdges(nfa, graph)

            return GraphJFrame(graph = graph, end = listOf(end!!), start = listOf(nfa.num))
        }

        private fun addVertexes(
            s: NfaState,
            graph: Graph<Int, LabeledEdge>,
            seen: MutableSet<NfaState> = mutableSetOf(),
        ): Int? {

            if (!seen.add(s))
                return null

            graph.addVertex(s.num)

            var end: Int? = null

            for ((_, transitions) in s.transition)
                for (t in transitions) {
                    val newEnd: Int? = this.addVertexes(t, graph, seen)
                    if (newEnd != null)
                        end = newEnd
                }

            return if (s.finalState)
                s.num
            else
                end
        }

        private fun addEdges(
            s: NfaState,
            graph: Graph<Int, LabeledEdge>,
            seen: MutableSet<NfaState> = mutableSetOf(),
        ) {

            if (!seen.add(s))
                return

            for ((char, transitions) in s.transition)
                for (t in transitions) {
                    graph.addEdge(s.num, t.num, LabeledEdge(char.toString()))
                    this.addEdges(t, graph, seen)
                }
        }
    }

}
