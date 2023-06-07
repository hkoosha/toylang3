package io.koosha.toylang.toylang3.regex.ui

import org.jgrapht.graph.DefaultEdge

class LabeledEdge(
    private val label: String?,
) : DefaultEdge() {

    override fun toString(): String =
        "${this.label}"
}
