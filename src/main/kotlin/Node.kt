import CFGNode as AstNode

enum class Condition {
    NONE,
    TRUE,
    FALSE
}

sealed class Node(val order: Int?, val content: AstNode) {
    val successors: HashMap<Node, Condition> = hashMapOf()
    val predecessors: MutableList<Node> = mutableListOf()
    var isReturn = false

    fun addPredecessor(pred: Node) {
        predecessors.add(pred)
    }

    fun addSuccessor(succ: Node) {
        successors[succ] = Condition.NONE
    }

    fun addConditionalSuccessor(succ: Node, c: Condition) {
        successors[succ] = c
    }

    fun setReturn() {
        isReturn = true
    }

    fun removeSuccessor(succ: Node) {
        successors.remove(succ)
    }

    fun removePredecessor(pred: Node) {
        predecessors.remove(pred)
    }

    override fun toString() = content.toString()

    override fun hashCode(): Int {
        return content.hashCode() + order.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Node) return false
        return content == other.content && order == other.order
    }
}

class ActionNode(order: Int, init: AstNode): Node(order, init)
class BeginNode(order: Int, init: AstNode): Node(order, init)
class ConditionNode(order: Int, init: AstNode): Node(order, init)
class TerminateNode(order: Int, init: AstNode): Node(null, init) {

    fun selfDelete() {
        successors.forEach {
            it.key.removePredecessor(this)
        }
        predecessors.forEach {
            val oldCond = it.successors[this]
            it.removeSuccessor(this)
            successors.forEach { its ->
                its.key.addPredecessor(it)
                it.addConditionalSuccessor(its.key, oldCond!!)
            }
        }
    }
}