package net.chronoflow.feature.today

fun fromResource() {
    Text(stringResource(R.string.x))
}

fun fromVariable(label: String) {
    Text(text = label)
}

fun field() {
    TextField(value = "", onValueChange = {})
}

fun customText() {
    CfText("custom")
    MyText("custom")
}

@Preview
fun previewed() {
    Text("Preview")
}

@PreviewLightDark
fun previewedDark() {
    Text(text = "Preview")
}
