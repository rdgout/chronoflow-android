package net.chronoflow.feature.auth.model

enum class AuthPolicy(
    val url: String,
) {
    Terms("https://chronoflow.net/terms"),
    Privacy("https://chronoflow.net/privacy"),
}

object AuthConsentText {
    sealed interface Segment {
        data class Plain(
            val text: String,
        ) : Segment

        data class Link(
            val policy: AuthPolicy,
            val text: String,
        ) : Segment
    }

    fun parse(text: String): List<Segment> {
        val result = mutableListOf<Segment>()
        var remaining = text
        while (true) {
            val opening = remaining.indexOf('<').takeIf { it >= 0 } ?: break
            val end = remaining.indexOf('>', opening).takeIf { it >= 0 } ?: break
            val tag = remaining.substring(opening + 1, end)
            val policy =
                when (tag) {
                    "terms" -> AuthPolicy.Terms
                    "privacy" -> AuthPolicy.Privacy
                    else -> break
                }
            val closingTag = "</$tag>"
            val closing = remaining.indexOf(closingTag, end + 1).takeIf { it >= 0 } ?: break
            result += Segment.Plain(remaining.substring(0, opening))
            result += Segment.Link(policy, remaining.substring(end + 1, closing))
            remaining = remaining.substring(closing + closingTag.length)
        }
        result += Segment.Plain(remaining)
        return result
    }
}
