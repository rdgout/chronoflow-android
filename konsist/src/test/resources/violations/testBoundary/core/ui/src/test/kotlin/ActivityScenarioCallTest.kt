package fixture

class ActivityScenarioCallTest {
    fun violatesBoundary() { ActivityScenario.launch<MainActivity>(error("intent")) }
}
