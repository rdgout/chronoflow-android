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
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            val factory = viewModelFactory { initializer { GraphViewModel(checkNotNull(this[AppGraphKey])) } }
            val model = factory.create(GraphViewModel::class.java, activity.defaultViewModelCreationExtras)
            assertSame((activity.application as ChronoFlowApp).graph, model.graph)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
