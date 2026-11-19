package net.chronoflow.core.plan.step

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Priority

val legendGroups: List<Pair<String, List<BlockType>>> =
    listOf(
        "Sleep" to listOf(BlockType.Sleep, BlockType.Nap, BlockType.Rest),
        "Light" to listOf(BlockType.SeekLight, BlockType.LightOK, BlockType.LightNeutral, BlockType.AvoidLight),
        "Caffeine" to
            listOf(BlockType.CaffeineServing, BlockType.CaffeineRoutine, BlockType.CaffeineOK, BlockType.NoCaffeine),
        "Travel" to
            listOf(
                BlockType.Flight,
                BlockType.Layover,
                BlockType.Transfer,
                BlockType.MovementBreak,
                BlockType.ZoneChange,
                BlockType.Note,
            ),
    )

fun legendDescription(type: BlockType): String =
    when (type) {
        BlockType.Sleep -> "Your main sleep window."
        BlockType.Nap -> "A planned nap."
        BlockType.Rest -> "Time set aside for rest."
        BlockType.SeekLight -> "Follow the light source shown in your plan."
        BlockType.LightOK -> "No special action needed."
        BlockType.LightNeutral -> "Ordinary indoor or cabin light is fine."
        BlockType.AvoidLight -> "Follow your plan's guidance to keep light low."
        BlockType.CaffeineServing -> "A timed serving in your plan."
        BlockType.CaffeineRoutine -> "Your daily caffeine guidance."
        BlockType.CaffeineOK -> "A time window for caffeine in your plan."
        BlockType.NoCaffeine -> "A time to avoid caffeine in your plan."
        BlockType.Flight -> "Your flight."
        BlockType.Layover -> "Time between flights."
        BlockType.Transfer -> "The trip to or from the airport around your flight."
        BlockType.MovementBreak -> "Time to get up and move."
        BlockType.ZoneChange -> "A change in local time during your trip."
        else -> "Extra guidance in your plan."
    }

fun priorityDescription(priority: Priority): String =
    when (priority) {
        Priority.Core -> "Your plan depends on this step."
        Priority.Recommended -> "Helps your clock shift as planned."
        Priority.Optional -> "Nice if you can."
        else -> "Good to know, nothing to do."
    }
