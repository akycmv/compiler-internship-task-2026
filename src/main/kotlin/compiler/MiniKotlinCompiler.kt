package org.example.compiler

import MiniKotlinBaseVisitor
import MiniKotlinParser

class MiniKotlinCompiler : MiniKotlinBaseVisitor<String>() {
    private var argCounter = 0
    private var indentLevel = 0

    private fun indent() = "    ".repeat(indentLevel)

    private fun callArg() = "arg${argCounter++}"

    private fun <T> indentBlock(block: () -> T): T {
        indentLevel++
        var result = block()
        indentLevel--
        return result
    }

    fun compile(
        program: MiniKotlinParser.ProgramContext,
        className: String = "MiniProgram",
    ): String {
        val functions =
            program.functionDeclaration().joinToString("\n\n") { visitFunctionDeclaration(it) }

        return "public class $className {\n\n$functions\n\n}"
    }

    override fun visitFunctionDeclaration(
        ctx: MiniKotlinParser.FunctionDeclarationContext
    ): String {
        argCounter = 0
        indentLevel = 1
        val name = ctx.IDENTIFIER().text
        val returnType = visitType(ctx.type())

        val signature =
            if (name == "main") {
                "public static void main(String[] args)"
            } else {
                val params = buildString {
                    ctx.parameterList()?.parameter()?.forEach { p ->
                        append("${visitType(p.type())} ${p.IDENTIFIER().text}, ")
                    }
                    append("Continuation<$returnType> __continuation")
                }
                "public static void $name($params)"
            }

        val body = visitBlock(ctx.block())

        return "$signature {\n$body\n}"
    }

    override fun visitBlock(ctx: MiniKotlinParser.BlockContext): String {
        return compileStatements(ctx.statement())
    }

    // parse all possible statements based on grammar and proceed
    fun compileStatements(stms: List<MiniKotlinParser.StatementContext>): String {
        if (stms.isEmpty()) {
            return ""
        }

        val head = stms.first()
        val tail = stms.drop(1)

        return when {
            head.returnStatement() != null -> compileReturn(head.returnStatement())
            head.variableDeclaration() != null -> compileVar(head.variableDeclaration(), tail)
            head.variableAssignment() != null -> compileAsgt(head.variableAssignment(), tail)
            head.ifStatement() != null -> compileIf(head.ifStatement(), tail)
            head.whileStatement() != null -> compileWhile(head.whileStatement(), tail)
            head.expression() != null ->
                compileExpr(head.expression()) { _ -> compileStatements(tail) }
            else -> error("unknown statement at ${head.start.line}")
        }
    }

    // parse `return` statements
    private fun compileReturn(ctx: MiniKotlinParser.ReturnStatementContext): String {
        // if we have unit/implicit return
        return if (ctx.expression() == null) {
            "${indent()}__continuation.accept(null);\n${indent()}return;\n"
        } else {
            // for cases such as `return n * factorial(n - 1)` from the example
            compileExpr(ctx.expression()) { value ->
                "${indent()}__continuation.accept($value);\n${indent()}return;\n"
            }
        }
    }

    // parse `var` statements
    private fun compileVar(
        ctx: MiniKotlinParser.VariableDeclarationContext,
        tail: List<MiniKotlinParser.StatementContext>,
    ): String {
        val name = ctx.IDENTIFIER().text
        val type = visitType(ctx.type())

        return compileExpr(ctx.expression()) { value ->
            "${indent()}$type $name = $value;\n" + compileStatements(tail)
        }
    }

    // parse `=` as assignment statements
    private fun compileAsgt(
        ctx: MiniKotlinParser.VariableAssignmentContext,
        tail: List<MiniKotlinParser.StatementContext>,
    ): String {
        val name = ctx.IDENTIFIER().text
        return compileExpr(ctx.expression()) { value ->
            "${indent()}$name = $value;\n" + compileStatements(tail)
        }
    }

    // parse `if (...) else` statements
    // we could change if to be an expression for multileveled ifs
    private fun compileIf(
        ctx: MiniKotlinParser.IfStatementContext,
        tail: List<MiniKotlinParser.StatementContext>,
    ): String {
        return compileExpr(ctx.expression()) { cond ->
            // parse both branches if exist
            val ifBranch = indentBlock { compileStatements(ctx.block(0).statement() + tail) }
            val elseBranch =
                if (ctx.block().size > 1) {
                    "${indent()}} else {\n" +
                        indentBlock { compileStatements(ctx.block(1).statement() + tail) }
                } else ""

            "${indent()}if ($cond) {\n" + ifBranch + elseBranch + "${indent()}}\n"
        }
    }

    // parse `while` statements
    private fun compileWhile(
        ctx: MiniKotlinParser.WhileStatementContext,
        tail: List<MiniKotlinParser.StatementContext>,
    ): String {
        return compileExpr(ctx.expression()) { cond ->
            val block =
                when (ctx.block()) {
                    null -> ""
                    else -> indentBlock { compileStatements(ctx.block().statement()) }
                }

            "${indent()}while ($cond) {\n" + block + "${indent()}}\n" + compileStatements(tail)
        }
    }

    // parse all potential expressions
    private fun compileExpr(
        ctx: MiniKotlinParser.ExpressionContext,
        cont: (String) -> String,
    ): String =
        when (ctx) {
            is MiniKotlinParser.PrimaryExprContext -> compilePrimary(ctx.primary(), cont)
            is MiniKotlinParser.NotExprContext -> compileExpr(ctx.expression()) { v -> cont("!$v") }

            // *, +, -, =, >, <, &&, ||
            is MiniKotlinParser.MulDivExprContext,
            is MiniKotlinParser.AddSubExprContext,
            is MiniKotlinParser.ComparisonExprContext,
            is MiniKotlinParser.EqualityExprContext,
            is MiniKotlinParser.AndExprContext,
            is MiniKotlinParser.OrExprContext ->
                compileBinary(
                    ctx.getChild(0) as MiniKotlinParser.ExpressionContext,
                    ctx.getChild(1).text,
                    ctx.getChild(2) as MiniKotlinParser.ExpressionContext,
                    cont,
                )

            is MiniKotlinParser.FunctionCallExprContext -> compileFunc(ctx, cont)

            else -> error("unknown expression at ${ctx.start.line}")
        }

    // parse expression of form `a op b`
    private fun compileBinary(
        left: MiniKotlinParser.ExpressionContext,
        op: String,
        right: MiniKotlinParser.ExpressionContext,
        cont: (String) -> String,
    ): String = compileExpr(left) { l -> compileExpr(right) { r -> cont("($l $op $r)") } }

    // parse primary expression
    private fun compilePrimary(
        ctx: MiniKotlinParser.PrimaryContext,
        cont: (String) -> String,
    ): String =
        when (ctx) {
            is MiniKotlinParser.ParenExprContext -> compileExpr(ctx.expression(), cont)
            is MiniKotlinParser.IntLiteralContext -> cont(ctx.INTEGER_LITERAL().text)
            is MiniKotlinParser.StringLiteralContext -> cont(ctx.STRING_LITERAL().text)
            is MiniKotlinParser.BoolLiteralContext -> cont(ctx.BOOLEAN_LITERAL().text)
            is MiniKotlinParser.IdentifierExprContext -> cont(ctx.IDENTIFIER().text)
            else -> error("unknown primary at ${ctx.start.line}")
        }

    // parse function calls
    private fun compileFunc(
        ctx: MiniKotlinParser.FunctionCallExprContext,
        cont: (String) -> String,
    ): String {
        val name =
            when (ctx.IDENTIFIER().text) {
                "println" -> "Prelude.println"
                else -> ctx.IDENTIFIER().text
            }
        val args = ctx.argumentList()?.expression() ?: emptyList()

        // propagate chain of arguments into CPS calls
        fun liftArgs(idx: Int, acc: List<String>): String {
            if (idx == args.size) {
                val arg = callArg()
                val argList = acc.joinToString(", ")
                val body = indentBlock { cont(arg) }
                return "${indent()}$name($argList, ($arg) -> {\n$body${indent()}});\n"
            }
            return compileExpr(args[idx]) { value -> liftArgs(idx + 1, acc + value) }
        }

        return liftArgs(0, emptyList())
    }

    // override fun visitParameter(ctx: MiniKotlinParser.ParameterContext): String {
    //     return "empty!"
    // }

    override fun visitType(ctx: MiniKotlinParser.TypeContext): String {
        return when {
            // Future: add more types if language spec changes
            ctx.INT_TYPE() != null -> "Integer"
            ctx.STRING_TYPE() != null -> "String"
            ctx.BOOLEAN_TYPE() != null -> "Boolean"
            ctx.UNIT_TYPE() != null -> "Void"
            else -> error("unknown type: ${ctx.text}")
        }
    }
}
