package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language

class CopyCatalog(
    data: ByteArray,
) {
    @Serializable
    private class Table(
        val entries: List<EntryDto>,
    )

    @Serializable
    private class EntryDto(
        val key: String,
        val arguments: List<String>,
        val languages: Map<String, TranslationDto>,
    )

    @Serializable
    private class TranslationDto(
        val kind: String,
        val formats: List<String>? = null,
        val cases: List<CaseDto>? = null,
    )

    @Serializable
    private class CaseDto(
        val bound: Double,
        val exact: Boolean,
        val format: String,
    )

    private sealed interface Token {
        class Literal(
            val text: String,
        ) : Token

        class Argument(
            val index: Int,
            val raw: String,
        ) : Token
    }

    private class Template(
        val tokens: List<Token>,
    ) {
        fun render(values: List<String>): String {
            val out = StringBuilder()
            for (token in tokens) {
                when (token) {
                    is Token.Literal -> out.append(token.text)
                    is Token.Argument -> out.append(values.getOrNull(token.index) ?: token.raw)
                }
            }
            return out.toString()
        }
    }

    private class Case(
        val bound: Double,
        val exact: Boolean,
        val template: Template,
    )

    private class Translation(
        val formats: List<Template>,
        val cases: List<Case>?,
    ) {
        fun format(count: Double): Template? {
            if (cases !=
                null
            ) {
                return cases.firstOrNull { if (it.exact) count == it.bound else count >= it.bound }?.template
            }
            if (formats.isEmpty()) return null
            return formats[if (count == 1.0) 0 else minOf(1, formats.size - 1)]
        }
    }

    private class Entry(
        val arguments: List<String>,
        val languages: Map<String, Translation>,
    )

    private val entries: Map<String, Entry>
    private val placeholder = Regex(":([A-Za-z][A-Za-z0-9_]*)")

    init {
        val table = ApiJson.response.decodeFromString(Table.serializer(), data.decodeToString())
        val map = HashMap<String, Entry>(table.entries.size * 2)
        for (dto in table.entries) {
            require(dto.key !in map) { "duplicate copy key: ${dto.key}" }
            require("en" in dto.languages) { "copy key without en: ${dto.key}" }
            map[dto.key] =
                Entry(
                    dto.arguments,
                    dto.languages.mapValues { (_, value) ->
                        Translation(
                            value.formats.orEmpty().map(::compile),
                            value.cases?.map { Case(it.bound, it.exact, compile(it.format)) },
                        )
                    },
                )
        }
        entries = map
    }

    fun contains(key: String): Boolean = key in entries

    fun text(
        key: String,
        language: Language,
        arguments: Map<String, String> = emptyMap(),
        count: Double = 1.0,
    ): String {
        val entry = entries[key] ?: return interpolate(key, arguments)
        val translation = entry.languages[language.raw] ?: entry.languages.getValue("en")
        val values =
            entry.arguments.map { name ->
                arguments[name] ?: if (name == "count") count.toString() else ":$name"
            }
        return translation.format(count)?.render(values) ?: ""
    }

    private fun interpolate(
        key: String,
        arguments: Map<String, String>,
    ): String = placeholder.replace(key) { match -> arguments[match.groupValues[1]] ?: match.value }

    companion object {
        suspend fun load(io: CoroutineDispatcher): CopyCatalog =
            withContext(io) {
                val bytes =
                    checkNotNull(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json")) {
                        "plan_copy.json is not on the classpath"
                    }.use { it.readBytes() }
                CopyCatalog(bytes)
            }

        private fun compile(format: String): Template {
            val tokens = ArrayList<Token>()
            val literal = StringBuilder()
            var sequential = 0
            var i = 0

            fun flush() {
                if (literal.isNotEmpty()) {
                    tokens.add(Token.Literal(literal.toString()))
                    literal.clear()
                }
            }
            while (i < format.length) {
                val c = format[i]
                if (c != '%' || i + 1 >= format.length) {
                    literal.append(c)
                    i++
                    continue
                }
                val next = format[i + 1]
                if (next == '%') {
                    literal.append('%')
                    i += 2
                } else if (next == '@') {
                    flush()
                    tokens.add(Token.Argument(sequential++, "%@"))
                    i += 2
                } else if (next in '0'..'9') {
                    var j = i + 1
                    while (j < format.length && format[j] in '0'..'9') j++
                    if (format.startsWith("$@", j)) {
                        flush()
                        tokens.add(Token.Argument(format.substring(i + 1, j).toInt() - 1, format.substring(i, j + 2)))
                        i = j + 2
                    } else {
                        literal.append(c)
                        i++
                    }
                } else {
                    literal.append(c)
                    i++
                }
            }
            flush()
            return Template(tokens)
        }
    }
}
