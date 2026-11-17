package net.chronoflow.app

interface WelcomeDestinations {
    fun planMyTrip()

    fun signIn()

    val seam: String? get() = null

    fun closeSeam() {}
}

object InertWelcomeDestinations : WelcomeDestinations {
    override fun planMyTrip() {}

    override fun signIn() {}
}
