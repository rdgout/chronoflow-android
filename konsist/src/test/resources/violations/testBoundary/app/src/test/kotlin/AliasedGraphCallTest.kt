package fixture

import net.chronoflow.app.AppGraph as Graph

class AliasedGraphCallTest {
    fun violatesBoundary() { Graph(error("context")) }
}
