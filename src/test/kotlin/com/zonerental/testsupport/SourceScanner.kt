package com.zonerental.testsupport

/**
 * Minimal source scanner for consistency tests: finds calls like `name(...)` across lines
 * and splits their top-level arguments, respecting string literals and nested parentheses.
 */
object SourceScanner {

    data class Call(val file: String, val args: List<String>)

    /** Finds every call to [functionName] in [sources] (path -> text). */
    fun findCalls(sources: Map<String, String>, functionName: String): List<Call> {
        val calls = mutableListOf<Call>()
        val needle = "$functionName("
        for ((path, text) in sources) {
            var idx = text.indexOf(needle)
            while (idx >= 0) {
                val before = if (idx > 0) text[idx - 1] else ' '
                // Skip declarations ("fun getMessage(") and longer identifiers ("xgetMessage(")
                val isIdentifierPart = before.isLetterOrDigit() || before == '_'
                val isDeclaration = text.substring(maxOf(0, idx - 4), idx) == "fun "
                if (!isIdentifierPart && !isDeclaration) {
                    extractArgs(text, idx + needle.length)?.let { calls += Call(path, it) }
                }
                idx = text.indexOf(needle, idx + needle.length)
            }
        }
        return calls
    }

    /** Returns the top-level arguments of the call whose '(' ends just before [start]. */
    private fun extractArgs(text: String, start: Int): List<String>? {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var inString = false
        var i = start
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                current.append(c)
                if (c == '\\' && i + 1 < text.length) {
                    current.append(text[i + 1]); i += 2; continue
                }
                if (c == '"') inString = false
            } else when (c) {
                '"' -> { inString = true; current.append(c) }
                '(', '[', '{' -> { depth++; current.append(c) }
                ')', ']', '}' -> {
                    if (depth == 0) {
                        if (current.isNotBlank()) args += current.toString().trim()
                        return args
                    }
                    depth--; current.append(c)
                }
                ',' -> if (depth == 0) { args += current.toString().trim(); current.clear() } else current.append(c)
                else -> current.append(c)
            }
            i++
        }
        return null
    }

    private val STRING_LITERAL = Regex("^\"((?:[^\"\\\\]|\\\\.)*)\"$")

    /** The value of a plain string literal argument, or null if the argument is an expression. */
    fun stringLiteral(arg: String): String? = STRING_LITERAL.find(arg.trim())?.groupValues?.get(1)
}
