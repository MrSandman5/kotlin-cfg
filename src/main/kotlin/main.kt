import info.leadinglight.jdot.Graph
import kastree.ast.psi.Parser
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("No input file specified")
        exitProcess(-1)
    }
    val config = ConfigReader.instance
    Graph.DEFAULT_CMD = config.getStringProperty("Dot")
    Graph.DEFAULT_BROWSER_CMD = arrayOf(config.getStringProperty("Browser"))
    val codeStr = File(args[0]).readText().trimIndent()
    val fileAst = Parser().parseFile(codeStr)
    val graph = GraphBuilder(fileAst).build()
    graph.removeTerminateNodes()
    graph.view()
}