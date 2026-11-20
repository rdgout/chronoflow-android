package fixture

class AppGraphCallTest {
    fun violatesBoundary() { AppGraph(error("context")) }
}
