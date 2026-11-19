package fixture

class QualifiedRootCallTest {
    fun violatesBoundary() { net.chronoflow.app.CfRoot(error("graph")) }
}
