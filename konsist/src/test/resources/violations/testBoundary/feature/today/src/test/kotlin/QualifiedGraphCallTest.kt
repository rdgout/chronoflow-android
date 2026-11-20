package fixture

class QualifiedGraphCallTest {
    fun violatesBoundary() { net.chronoflow.app.AppGraph(error("context")) }
}
