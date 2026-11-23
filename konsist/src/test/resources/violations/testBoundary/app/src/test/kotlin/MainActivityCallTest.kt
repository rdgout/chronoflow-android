package fixture

class MainActivityCallTest {
    fun violatesBoundary() { Robolectric.buildActivity(MainActivity::class.java).setup() }
}
