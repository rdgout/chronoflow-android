package net.chronoflow.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import net.chronoflow.feature.launch.LaunchController

internal fun newLaunchController(): LaunchController =
    LaunchController(moon = { resources ->
        ImageBitmap.imageResource(resources, R.drawable.launch_moon)
    })
