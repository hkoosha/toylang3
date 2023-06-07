package io.koosha.toylang.toylang3.regex.ui

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class GraphJFrame(
    graph: Graph<Int, LabeledEdge>,
    end: List<Int>,
    start: List<Int>,
) : JFrame() {

    private val contentPane: JPanel

    init {
        val adapter = JGraphXAdapter(graph)
        adapter.model.beginUpdate()
        adapter.clearSelection()
        adapter.selectAll()
        for (c in adapter.selectionCells) {
            val cell = c as mxCell
            if (cell.isVertex) {
                cell.geometry.width = 40.0
                cell.geometry.height = 40.0
                if (end.contains(cell.value))
                    adapter.setCellStyle(mxConstants.STYLE_FILLCOLOR + "=yellow", arrayOf(cell))
                if (start.contains(cell.value))
                    adapter.setCellStyle(mxConstants.STYLE_FILLCOLOR + "=red", arrayOf(cell))
            }
        }
        adapter.model.endUpdate()
        val layout = mxCircleLayout(adapter)
        layout.execute(adapter.defaultParent)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.setBounds(100, 100, 550, 450)
        this.contentPane = JPanel()
        this.contentPane.border = EmptyBorder(5, 5, 5, 5)
        this.setContentPane(this.contentPane)
        this.contentPane.layout = null
        val graphComponent = mxGraphComponent(adapter)
        graphComponent.isPageVisible = true
        graphComponent.setBounds(30, 30, 1600, 1600)
        this.contentPane.add(graphComponent)
        graphComponent.viewport.background = Color.white
        this.preferredSize = Dimension(1600, 1600)
    }
}
