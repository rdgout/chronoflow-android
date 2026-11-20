package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.feature.wizard.questions.WizardQuestionUi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class QuestionKindsTest(
    private val kind: PromptKind,
) {
    @Test fun supportedQuestionKindsAreExactlyAgeBandAndShiftWork() {
        val field = if (kind == PromptKind.Screening) "shift_work" else "age_band"
        val options = WizardQuestionUi.options(questionPrompt(kind, field))
        assertEquals(kind == PromptKind.AgeBand || kind == PromptKind.Screening, options.isNotEmpty())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = PromptKind.known + PromptKind.Unknown("bipolar")
    }
}
