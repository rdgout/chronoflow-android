package net.chronoflow.core.testing.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.DeviceProfiles

/** A fixed probe for the harness: an outlined box with two short texts. Not a screen. */
@Composable
fun HarnessProbe(modifier: Modifier = Modifier) {
    ProfileHost(DeviceProfiles.pixel8) {
        Box(modifier.fillMaxSize().padding(16.dp)) {
            Column(Modifier.size(width = 120.dp, height = 48.dp).border(1.dp, Color.Black).padding(4.dp)) {
                Text("Probe")
                Text("12:00")
            }
        }
    }
}
