package fixture

class RootContentCallTest {
    fun violatesBoundary() { RootContent(error("state"), {}) {} }
}
