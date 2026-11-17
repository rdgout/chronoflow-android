package net.chronoflow.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Debug-only entry for `adb shell am start`: applies the seed on IO, then restarts MainActivity. */
class SeedReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val seed = TestSeed.fromIntent(intent)
        lifecycleScope.launch {
            TestSeeds.apply(applicationContext, seed, Dispatchers.IO)
            startActivity(TestSeeds.forwardIntent(this@SeedReceiverActivity, seed))
            finish()
        }
    }
}
