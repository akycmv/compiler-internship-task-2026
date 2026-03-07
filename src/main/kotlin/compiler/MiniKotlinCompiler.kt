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

    fun compileStatements(stms: List<MiniKotlinParser.StatementContext>): String {

        return ""
    }

    override fun visitParameter(ctx: MiniKotlinParser.ParameterContext): String {
        return "empty!"
    }

    override fun visitType(ctx: MiniKotlinParser.TypeContext): String {
        return when {
            ctx.INT_TYPE() != null -> "Integer"
            ctx.STRING_TYPE() != null -> "String"
            ctx.BOOLEAN_TYPE() != null -> "Boolean"
            ctx.UNIT_TYPE() != null -> "Void"
            else -> error("unknown type: ${ctx.text}")
        }
    }
}
