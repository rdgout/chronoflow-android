package fixture

class QualifiedActivityCallTest {
    fun violatesBoundary() { org.robolectric.Robolectric.buildActivity(net.chronoflow.app.MainActivity::class.java).setup() }
}
