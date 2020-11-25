import info.leadinglight.jdot.Edge

class Graph(val inputs: Set<Node>) {
    var condition: Condition? = null
    val nodes = hashSetOf<Node>()
    var outputs = hashSetOf<Node>()

    init {
        inputs.forEach {
            nodes.add(it)
            outputs.add(it)
        }
    }

    constructor(inp: Set<Node>, cond: Condition?): this(inp) {
        condition = cond
    }

    fun add(other: Graph) {
        other.nodes.forEach { nodes.add(it) }
        outputs = other.getActiveOutputs().toHashSet()
    }

    fun merge(other: Graph) {
        other.nodes.forEach { nodes.add(it) }
        other.getActiveOutputs().forEach { outputs.add(it) }
    }

    fun addNode(n: Node) {
        nodes.add(n)
        getActiveOutputs().forEach {
            if (condition != null) {
                it.addConditionalSuccessor(n, condition!!)
                condition = null
            } else {
                it.addSuccessor(n)
            }
            n.addPredecessor(it)
        }
        outputs = hashSetOf(n)
    }

    fun getActiveOutputs() = outputs.filter { !it.isReturn }.toSet()

    fun removeTerminateNodes() {
        val removableNodes = mutableListOf<Node>()
        nodes.forEach {
            if (it is TerminateNode) {
                it.selfDelete()
                removableNodes.add(it)
            }
        }
        removableNodes.forEach { nodes.remove(it) }
    }

    fun view() {
        val graph = info.leadinglight.jdot.Graph()
        nodes.forEach {
            val node = info.leadinglight.jdot.Node(it.content.order.toString() + ". " + it.content.text).setShape(
                when (it) {
                    is BeginNode -> info.leadinglight.jdot.enums.Shape.ellipse
                    is ActionNode -> info.leadinglight.jdot.enums.Shape.box
                    is ConditionNode -> info.leadinglight.jdot.enums.Shape.diamond
                    is TerminateNode -> info.leadinglight.jdot.enums.Shape.egg   /// this should not happen in normal workflow
                }
            )
            graph.addNode(node)
        }
        nodes.forEach {
            for (succ in it.successors) {
                graph.addEdge(
                    Edge(it.content.order.toString() + ". " + it.content.text)
                        .addNode(succ.key.content.order.toString() + ". " + succ.key.content.text).setColor(
                    when (succ.value) {
                        Condition.NONE -> "#000000"
                        Condition.TRUE -> "#00FF00"
                        Condition.FALSE -> "#FF0000"
                    }
                ))
            }
        }
        graph.viewSvg()
    }
}