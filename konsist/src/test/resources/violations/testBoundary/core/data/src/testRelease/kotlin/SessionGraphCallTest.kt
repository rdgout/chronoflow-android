package fixture

class SessionGraphCallTest {
    fun violatesBoundary() { net.chronoflow.app.SessionGraph(error("args")) }
}
