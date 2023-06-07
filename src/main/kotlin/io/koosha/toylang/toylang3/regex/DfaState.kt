package io.koosha.toylang.toylang3.regex

import io.koosha.toylang.toylang3.regex.ui.GraphJFrame
import io.koosha.toylang.toylang3.regex.ui.LabeledEdge
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import javax.swing.JFrame

data class DfaState(
    val num: Int,
    val fromStates: Set<NfaState>,
    val transition: MutableMap<Char, DfaState> = mutableMapOf(),
    var finalState: Boolean = false,
    var startState: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (other !is DfaState)
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

        fun graphToString(
            s: DfaState,
        ): String {
            val sb = StringBuilder()
            graphToString(s, mutableSetOf(), sb)
            return sb.toString()
        }

        private fun stateToString(
            s: DfaState,
            sb: StringBuilder,
        ) {

            sb.append("===[")
                .append(s.num)
                .append("]\n")

            for ((c, t) in s.transition)
                sb.append(c)
                    .append(" => ")
                    .append(t.num)
                    .append('\n')
        }

        private fun graphToString(
            s: DfaState,
            seen: MutableSet<DfaState>,
            sb: StringBuilder,
        ) {

            if (!seen.add(s))
                return

            stateToString(s, sb)

            sb.append("\n")

            for ((_, t) in s.transition)
                graphToString(t, seen, sb)
        }

        fun fromNfa(
            nfa: NfaState,
        ): DfaState {

            val alphabet: Set<Char> = this.alphabetOf(nfa).filterNotNull().toSet()

            var num = 0

            val starting = DfaState(
                num = num++,
                fromStates = this.eClosureOf(nfa),
            )

            val worked: MutableMap<Set<NfaState>, DfaState> = mutableMapOf()
            val workList: MutableMap<Set<NfaState>, DfaState> = mutableMapOf(starting.fromStates to starting)

            while (workList.isNotEmpty()) {
                val work: DfaState = workList.firstNotNullOf { it }.value
                workList.remove(work.fromStates)
                work.startState = work.fromStates.any { it.startState }

                for (c: Char in alphabet) {
                    val reachable = mutableSetOf<NfaState>()

                    for (state: NfaState in work.fromStates)
                        for (transition: NfaState in state.transition[c] ?: emptySet())
                            reachable.addAll(this.eClosureOf(transition))

                    if (reachable.isNotEmpty()) {
                        if (worked.containsKey(reachable))
                            work.transition[c] = worked[reachable]!!
                        else
                            work.transition[c] = workList.computeIfAbsent(reachable) {
                                DfaState(
                                    num = num++,
                                    fromStates = reachable,
                                )
                            }
                    }
                }

                worked[work.fromStates] = work
            }

            for (state in worked.values)
                this.markFinalStates(state)

            return starting
        }

        private fun markFinalStates(
            state: DfaState,
            seen: MutableSet<DfaState> = mutableSetOf(),
        ) {

            if (!seen.add(state))
                return

            state.finalState = state.fromStates.any { it.finalState }

            for (next in state.transition.values)
                this.markFinalStates(next, seen)
        }

        private fun eClosureOf(
            state: NfaState,
            seen: MutableSet<NfaState> = mutableSetOf(),
        ): MutableSet<NfaState> {

            val closure = mutableSetOf<NfaState>()

            if (!seen.add(state))
                return closure

            closure.add(state)
            closure.addAll(state.transition[null] ?: emptySet())

            for (eTransition in state.transition[null] ?: emptySet())
                closure.addAll(this.eClosureOf(eTransition, seen))

            return closure
        }

        private fun alphabetOf(
            state: NfaState,
            seen: MutableSet<NfaState> = mutableSetOf(),
        ): Set<Char?> {

            if (!seen.add(state))
                return emptySet()

            val alphabet = mutableSetOf<Char?>()
            alphabet.addAll(state.transition.keys)

            for ((_, transitions) in state.transition)
                for (transition in transitions)
                    alphabet.addAll(this.alphabetOf(transition, seen))

            return alphabet
        }

        fun draw(dfa: DfaState): JFrame {

            val graph = DefaultDirectedGraph<Int, LabeledEdge>(LabeledEdge::class.java)

            val end = mutableSetOf<DfaState>()
            val start = this.addVertexes(s = dfa, graph = graph, end = end)
            this.addEdges(dfa, graph)

            return GraphJFrame(
                graph = graph,
                end = end.map { it.num },
                start = listOf(start!!),
            )
        }

        private fun addVertexes(
            s: DfaState,
            graph: Graph<Int, LabeledEdge>,
            end: MutableSet<DfaState>,
            seen: MutableSet<DfaState> = mutableSetOf(),
        ): Int? {

            if (!seen.add(s))
                return null

            if (s.finalState)
                end.add(s)

            graph.addVertex(s.num)

            var start: Int? = null

            for ((_, transition) in s.transition) {
                val newStart: Int? = this.addVertexes(s = transition, graph = graph, seen = seen, end = end)
                if (newStart != null)
                    start = newStart
            }

            return if (s.startState)
                s.num
            else
                start
        }

        private fun addEdges(
            s: DfaState,
            graph: Graph<Int, LabeledEdge>,
            seen: MutableSet<DfaState> = mutableSetOf(),
        ) {

            if (!seen.add(s))
                return

            for ((char, transition) in s.transition) {
                graph.addEdge(s.num, transition.num, LabeledEdge(char.toString()))
                this.addEdges(transition, graph, seen)
            }
        }

        fun matches(
            value: String,
            dfa: DfaState,
        ): Boolean {

            var state = dfa

            for (char in value)
                if (state.transition.containsKey(char))
                    state = state.transition[char]!!
                else
                    return false

            return state.finalState
        }

    }

}
