package fixture

class QualifiedScenarioCallTest {
    fun violatesBoundary() { androidx.test.core.app.ActivityScenario.launch<net.chronoflow.app.MainActivity>(error("intent")) }
}
