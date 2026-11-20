package net.chronoflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class ViewModelExtrasTest {
    private class GraphViewModel(
        val graph: AppGraph,
    ) : ViewModel()

    @Test fun initializerReceivesTheApplicationGraph() {
        val app =
            androidx.test.core.app.ApplicationProvider
                .getApplicationContext<SessionTestApplication>()
        val intent =
            android.content
                .Intent(app, MainActivity::class.java)
                .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                .putExtra("CF_UI_TEST_PROFILE", "screen")
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).create()
        try {
            val activity = controller.get()
            val factory = viewModelFactory { initializer { GraphViewModel(checkNotNull(this[AppGraphKey])) } }
            val model = factory.create(GraphViewModel::class.java, activity.defaultViewModelCreationExtras)
            assertSame((activity.application as ChronoFlowApp).graph, model.graph)
        } finally {
            controller.destroy()
        }
    }
}
