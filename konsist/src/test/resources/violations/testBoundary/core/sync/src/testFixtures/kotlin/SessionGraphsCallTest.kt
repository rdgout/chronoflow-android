package fixture

class SessionGraphsCallTest {
    fun violatesBoundary() { SessionGraphs(error("args")) }
}
