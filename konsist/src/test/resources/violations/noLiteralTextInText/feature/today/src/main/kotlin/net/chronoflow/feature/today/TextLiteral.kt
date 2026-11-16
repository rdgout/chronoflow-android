package net.chronoflow.feature.today

fun positional() {
    Text("Hello")
}

fun named(name: String) {
    Text(text = "Hi $name")
}

fun raw() {
    BasicText("""raw""")
}

fun namedNotFirst() {
    Text(modifier = Modifier, text = "Hi")
}
