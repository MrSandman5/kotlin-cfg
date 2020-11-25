import Node as AstNode
import kastree.ast.Node

data class CFGNode(val text: String, val order: Int)

class GraphBuilder(private val ast: Node.File) {

    private var order: Int = 0
    private val continues = mutableListOf<MutableList<AstNode>>()
    private val breaks = mutableListOf<MutableList<AstNode>>()

    fun build(): Graph {
        val function = getFirstFunction()
        val begin = BeginNode(order, toDisplayNode(function))
        order++
        val graph = Graph(hashSetOf(begin))
        processFunction(function, graph)
        return graph
    }

    private fun getFirstFunction(): Node.Decl.Func{
        if (ast.decls.size != 1)
            throw Exception("Only single function files are supported")
        return (ast.decls[0] as? Node.Decl.Func)!!
    }

    private fun processFunction(function: Node.Decl.Func, graph: Graph) {
        when (function.body) {
            is Node.Decl.Func.Body.Expr -> {
                val body = function.body as Node.Decl.Func.Body.Expr
                processExpression(body.expr, graph)
            }
            is Node.Decl.Func.Body.Block -> {
                val body = function.body as Node.Decl.Func.Body.Block
                processBlock(body.block, graph)
            }
        }
    }

    private fun processExpression(expr: Node.Expr, graph: Graph) {
        when (expr) {
            is Node.Expr.If -> processIf(expr, graph)
            is Node.Expr.For -> processFor(expr, graph)
            is Node.Expr.While -> processWhile(expr, graph)
            is Node.Expr.Brace -> processBrace(expr, graph)
            is Node.Expr.Return -> processReturn(expr, graph)
            is Node.Expr.Break -> processBreak(expr, graph)
            is Node.Expr.Continue -> processContinue(expr, graph)
            else -> {
                graph.addNode(ActionNode(order, toDisplayNode(expr)))
                order++
            }
        }
    }

    private fun processIf(ifExpr: Node.Expr.If, graph: Graph) {
        val condNode = ConditionNode(order, toDisplayNode(ifExpr.expr))
        order++

        graph.addNode(condNode)
        val thenGraph = Graph(graph.getActiveOutputs(), Condition.TRUE)
        val elseGraph = Graph(graph.getActiveOutputs(), Condition.FALSE)

        processExpression(ifExpr.body, thenGraph)
        if (ifExpr.elseBody != null) {
            processExpression(ifExpr.elseBody!!, elseGraph)
        }

        graph.add(thenGraph)
        graph.merge(elseGraph)
    }

    private fun processFor(forExpr: Node.Expr.For, graph: Graph) {
        continues.add(mutableListOf())
        breaks.add(mutableListOf())

        val initGraph = Graph(graph.getActiveOutputs())
        graph.add(initGraph)
        graph.addNode(ConditionNode(order, toDisplayNode(
            Node.Expr.BinaryOp(Node.Expr.Name(forExpr.vars[0]?.name!!),
                Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.IN),
                forExpr.inExpr))))
        order++

        val bodyGraph = Graph(graph.getActiveOutputs(), Condition.TRUE)
        processExpression(forExpr.body, bodyGraph)
        graph.add(bodyGraph)

        graph.getActiveOutputs().forEach {
            bodyGraph.inputs.forEach { ita ->
                it.addSuccessor(ita)
                ita.addPredecessor(it)
            }
        }

        graph.outputs = bodyGraph.inputs.toHashSet()
        continues.removeAt(continues.size - 1)
        breaks.last().forEach {
            graph.outputs.add(it)
        }
        breaks.removeAt(breaks.size - 1)
    }

    private fun processWhile(whileExpr: Node.Expr.While, graph: Graph) {
        continues.add(mutableListOf())
        breaks.add(mutableListOf())
        val condNode = ConditionNode(order, toDisplayNode(whileExpr.expr))
        order++
        graph.addNode(condNode)

        val bodyGraph = Graph(graph.getActiveOutputs(), Condition.TRUE)
        processExpression(whileExpr.body, bodyGraph)
        graph.add(bodyGraph)
        graph.getActiveOutputs().forEach {
            bodyGraph.inputs.forEach { ita ->
                it.addSuccessor(ita)
                ita.addPredecessor(it)
            }
        }
        graph.outputs = bodyGraph.inputs.toHashSet()

        continues.last().forEach {
            condNode.addPredecessor(it)
            it.addSuccessor(condNode)
        }
        continues.removeAt(continues.size - 1)
        breaks.last().forEach {
            graph.outputs.add(it)
        }
        breaks.removeAt(breaks.size - 1)
    }

    private fun processBrace(braceExpr: Node.Expr.Brace, graph: Graph) {
        if (braceExpr.block != null){
            val block = braceExpr.block as Node.Block
            processBlock(block, graph)
        }
    }

    private fun processReturn(expression: Node.Expr.Return, graph: Graph) {
        val node = ActionNode(order, toDisplayNode(expression))
        order++
        node.setReturn()
        graph.addNode(node)
    }

    private fun processBreak(expression: Node.Expr.Break, graph: Graph) {
        if (expression.label == null) {
            val node = TerminateNode(order, toDisplayNode(expression.copy(graph.toString())))
            order++
            graph.nodes.add(node)
            graph.getActiveOutputs().forEach {
                if (graph.condition != null) {
                    it.addConditionalSuccessor(node, graph.condition!!)
                    graph.condition = null
                } else {
                    it.addSuccessor(node)
                }
                node.addPredecessor(it)
            }
            breaks.last().add(node)
            graph.outputs = hashSetOf()
        } else {
            println("Labeled continues and breaks are not supported!!")
        }
    }

    private fun processContinue(expression: Node.Expr.Continue, graph: Graph) {
        if (expression.label == null) {
            val node = TerminateNode(order, toDisplayNode(expression.copy(graph.toString())))
            order++
            node.setReturn()
            graph.nodes.add(node)
            graph.getActiveOutputs().forEach {
                if (graph.condition != null) {
                    it.addConditionalSuccessor(node, graph.condition!!)
                    graph.condition = null
                } else {
                    it.addSuccessor(node)
                }
                node.addPredecessor(it)
            }
            graph.outputs = hashSetOf()
            continues.last().add(node)
        } else {
            println("Labeled continues and breaks are not supported!!")
        }
    }

    private fun processBlock(block: Node.Block, graph: Graph) {
        val blockGraph = Graph(graph.getActiveOutputs(), graph.condition)
        for (stmt: Node.Stmt in block.stmts) {
            processStatement(stmt, blockGraph)
        }
        graph.add(blockGraph)
    }

    private fun processDeclaration(decl: Node.Decl, graph: Graph) {
        when (decl) {
            is Node.Decl.Property -> {
                graph.addNode(ActionNode(order, toDisplayNode(decl)))
                order++
            }
            else -> throw Exception("Only propertyDeclarations are implemented")
        }
    }

    private fun processStatement(stmt: Node.Stmt, graph: Graph) {
        when (stmt) {
            is Node.Stmt.Decl -> processDeclaration(stmt.decl, graph)
            is Node.Stmt.Expr -> processExpression(stmt.expr, graph)
        }
    }

    private fun toDisplayNode(node: Node): CFGNode {
        val text = when (node) {
            is Node.Decl -> declToString(node)
            is Node.Expr -> exprToString(node)
            is Node.Type -> typeToString(node)
            is Node.Decl.Property.Var -> varToString(node)
            else -> "${node::class}"
        }
        return CFGNode(text, order)
    }

    private fun typeToString(node: Node.Type?): String {
        if (node == null)
            return ""
        return when (val typeRef = node.ref) {
            is Node.TypeRef.Simple -> typeRef.pieces.joinToString { it.name }
            is Node.TypeRef.Paren -> "Typeref - Paren"
            is Node.TypeRef.Func -> "Typeref - Func"
            is Node.TypeRef.Nullable -> "Typeref - Nullable"
            is Node.TypeRef.Dynamic -> "Typeref - Dynamic"
        }
    }

    private fun propToString(prop: Node.Decl.Property?): String {
        if (prop == null)
            return ""
        val name = if (prop.vars[0] != null) prop.vars[0]?.name else "var"
        val type = prop.vars.joinToString { if (it != null) typeToString(it.type) else "" }
        val value = if (prop.expr != null) exprToString(prop.expr) else ""
        return "$name${if (type != "") " :${type}" else ""}${if (value != "") " = $value" else ""}"
    }

    private fun varToString(variable: Node.Decl.Property.Var?): String {
        if (variable == null)
            return ""
        val name = variable.name
        val type = if (variable.type != null) ": ${variable.type}" else ""
        return "${name}${type}"
    }

    private fun condToString(cond: Node.Expr.When.Cond): String {
        return when (cond) {
            is Node.Expr.When.Cond.Expr -> "(==)${exprToString(cond.expr)}"
            is Node.Expr.When.Cond.In -> "${if (cond.not) "!in" else "in"}${cond.expr}"
            is Node.Expr.When.Cond.Is -> "${if (cond.not) "!is" else "is"}${typeToString(cond.type)}"
        }
    }

    private fun exprToString(expr: Node.Expr?): String {
        if (expr == null)
            return ""
        return when (expr) {
            is Node.Expr.StringTmpl -> expr.elems.joinToString {
                when (it) {
                    is Node.Expr.StringTmpl.Elem.Regular -> "\'${it.str}\'"
                    is Node.Expr.StringTmpl.Elem.ShortTmpl -> it.str
                    is Node.Expr.StringTmpl.Elem.UnicodeEsc -> it.digits
                    is Node.Expr.StringTmpl.Elem.RegularEsc -> it.char.toString()
                    is Node.Expr.StringTmpl.Elem.LongTmpl -> exprToString(it.expr)
                }
            }
            is Node.Expr.Const -> expr.value
            is Node.Expr.When.Cond -> condToString(expr)
            is Node.Expr.When -> "when ${exprToString(expr.expr)}"
            is Node.Expr.Return -> "return ${exprToString(expr.expr)}"
            is Node.Expr.While -> "while ${exprToString(expr.expr)}"
            is Node.Expr.BinaryOp -> {
                val oper = expr.oper
                return "${exprToString(expr.lhs)} ${when(oper) {
                    is Node.Expr.BinaryOp.Oper.Infix -> oper.str
                    is Node.Expr.BinaryOp.Oper.Token -> oper.token.str
                }} ${exprToString(expr.rhs)}"
            }
            is Node.Expr.UnaryOp -> if (expr.prefix) "${expr.oper.token.str}${exprToString(expr.expr)}"
            else "${exprToString(expr.expr)}${expr.oper.token.str}"
            is Node.Expr.If -> "if ${exprToString(expr.expr)}"
            is Node.Expr.For -> "for ${varToString(expr.vars[0])} in ${exprToString(expr.inExpr)}"
            is Node.Expr.Continue -> "continue ${expr.label ?: ""}"
            is Node.Expr.Break -> "break ${expr.label ?: ""}"
            is Node.Expr.Name -> expr.name
            is Node.Expr.Call -> "${exprToString(expr.expr)}(${expr.args.joinToString { exprToString(it.expr)}})"
            is Node.Expr.ArrayAccess -> "${exprToString(expr.expr)}[${expr.indices.joinToString { exprToString(it) }}]"
            else -> TODO()
        }
    }

    private fun declToString (decl: Node.Decl): String {
        return when (decl) {
            is Node.Decl.Property -> propToString(decl)
            is Node.Decl.Func -> "function ${decl.name}(${decl.params.joinToString(",") { it.name }})"
            else -> TODO()
        }
    }

}