@file:Suppress(
    "ktlint:standard:max-line-length",
    "ktlint:standard:max-line-length",
    "ktlint:standard:enum-entry-name-case",
    "ktlint:standard:class-naming",
)

package net.chronoflow.core.wizard.model

sealed interface MemberWizardDestination {
    data object outbound : MemberWizardDestination

    data object returnChoice : MemberWizardDestination

    data object returnRoute : MemberWizardDestination

    data object returnFlight : MemberWizardDestination

    data object sleep : MemberWizardDestination

    data object checking : MemberWizardDestination

    data class question(
        val index: Int,
    ) : MemberWizardDestination

    data object excluded : MemberWizardDestination

    data object continuation : MemberWizardDestination

    data object fineTune : MemberWizardDestination

    data object ready : MemberWizardDestination

    data object calculating : MemberWizardDestination

    val step: MemberWizardStep get() =
        when (this) {
            outbound -> MemberWizardStep.outbound
            returnChoice -> MemberWizardStep.returnChoice
            returnRoute -> MemberWizardStep.returnRoute
            returnFlight -> MemberWizardStep.returnFlight
            sleep -> MemberWizardStep.sleep
            checking -> MemberWizardStep.checking
            is question -> MemberWizardStep.question
            excluded -> MemberWizardStep.excluded
            continuation -> MemberWizardStep.continuation
            fineTune -> MemberWizardStep.fineTune
            ready -> MemberWizardStep.ready
            calculating -> MemberWizardStep.calculating
        }
}

val MemberWizardModel.navigationPath: List<MemberWizardDestination> get() {
    val step = state.step
    if (step == MemberWizardStep.whereTo) return emptyList()
    if (step == MemberWizardStep.outbound) return listOf(MemberWizardDestination.outbound)
    val result = mutableListOf<MemberWizardDestination>(MemberWizardDestination.outbound)
    if (mode == WizardMode.member) result.add(MemberWizardDestination.returnChoice)
    if (step == MemberWizardStep.returnChoice) return result
    if (mode == WizardMode.member && form.end.kind == "return_legs") {
        result.add(MemberWizardDestination.returnRoute)
        if (step == MemberWizardStep.returnRoute) return result
        result.add(MemberWizardDestination.returnFlight)
    }
    if (step == MemberWizardStep.returnFlight) return result
    if (mode == WizardMode.guest || state.sleepNeeded || state.lastInputStep == MemberWizardStep.sleep ||
        step == MemberWizardStep.sleep
    ) {
        result.add(MemberWizardDestination.sleep)
    }
    when (step) {
        MemberWizardStep.checking -> {
            result.add(MemberWizardDestination.checking)
        }

        MemberWizardStep.question -> {
            (0..(state.questionIndex ?: 0)).forEach { result.add(MemberWizardDestination.question(it)) }
        }

        MemberWizardStep.excluded -> {
            result.add(MemberWizardDestination.excluded)
        }

        MemberWizardStep.continuation, MemberWizardStep.fineTune -> {
            result.add(MemberWizardDestination.fineTune)
        }

        MemberWizardStep.ready, MemberWizardStep.calculating -> {
            if (optionalPrompts.isNotEmpty()) result.add(MemberWizardDestination.fineTune)
            result.add(MemberWizardDestination.ready)
            if (step == MemberWizardStep.calculating) result.add(MemberWizardDestination.calculating)
        }

        else -> {}
    }
    return result
}

fun MemberWizardModel.popNavigation(
    to: List<MemberWizardDestination>,
    from: List<MemberWizardDestination>,
) {
    if (firstScreen || state.step == MemberWizardStep.calculating || from != navigationPath || to.size >= from.size ||
        from.take(to.size) != to
    ) {
        return
    }
    val last = to.lastOrNull()
    if (last is MemberWizardDestination.question) {
        navigateToQuestion(last.index)
    } else {
        go(last?.step ?: MemberWizardStep.whereTo)
    }
}

val MemberWizardModel.firstScreen: Boolean get() =
    state.step == MemberWizardStep.whereTo ||
        (
            state.copiedRoute != null && state.edit == null && state.step == MemberWizardStep.outbound &&
                state.revisit == null
        ) ||
        (state.edit != null && state.step == MemberWizardStep.ready) ||
        (state.edit != null && state.step == MemberWizardStep.checking && checking.failure != null)

val MemberWizardModel.showsBack: Boolean get() =
    !firstScreen && state.step != MemberWizardStep.calculating &&
        !(state.step == MemberWizardStep.checking && checking.failure == null)
