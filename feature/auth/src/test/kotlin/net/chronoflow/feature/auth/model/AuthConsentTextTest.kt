package net.chronoflow.feature.auth.model

import net.chronoflow.feature.auth.model.AuthConsentText.Segment.Link
import net.chronoflow.feature.auth.model.AuthConsentText.Segment.Plain
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthConsentTextTest {
    @Test fun englishConsentHasTwoPolicyLinks() {
        assertEquals(
            listOf(
                Plain("By creating an account you agree to the "),
                Link(AuthPolicy.Terms, "terms"),
                Plain(" and "),
                Link(AuthPolicy.Privacy, "privacy policy"),
                Plain("."),
            ),
            AuthConsentText.parse(
                "By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>.",
            ),
        )
    }

    @Test fun dutchConsentHasTwoPolicyLinks() {
        assertEquals(
            listOf(
                Plain("Door een account aan te maken ga je akkoord met de "),
                Link(AuthPolicy.Terms, "voorwaarden"),
                Plain(" en het "),
                Link(AuthPolicy.Privacy, "privacybeleid"),
                Plain("."),
            ),
            AuthConsentText.parse(
                "Door een account aan te maken ga je akkoord met de <terms>voorwaarden</terms> en het <privacy>privacybeleid</privacy>.",
            ),
        )
    }

    @Test fun unknownTagKeepsTheRestWithoutDuplicatingThePrefix() {
        assertEquals(listOf(Plain("a <foo>x</foo>")), AuthConsentText.parse("a <foo>x</foo>"))
        assertEquals(
            listOf(Plain("a "), Link(AuthPolicy.Terms, "t"), Plain(" b <foo>x</foo>")),
            AuthConsentText.parse("a <terms>t</terms> b <foo>x</foo>"),
        )
    }

    @Test fun missingClosingTagKeepsTheRest() {
        assertEquals(listOf(Plain("a <terms>t")), AuthConsentText.parse("a <terms>t"))
    }

    @Test fun plainTextStaysPlain() {
        assertEquals(listOf(Plain("plain")), AuthConsentText.parse("plain"))
    }

    @Test fun policyUrlsMatchTheShippedPolicies() {
        assertEquals("https://chronoflow.net/terms", AuthPolicy.Terms.url)
        assertEquals("https://chronoflow.net/privacy", AuthPolicy.Privacy.url)
    }
}
