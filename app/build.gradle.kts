plugins {
    id("chronoflow.android.application.compose")
}

android {
    namespace = "net.chronoflow.app"

    defaultConfig {
        applicationId = "net.chronoflow.app.native"
    }
}

dependencies {
    implementation(project(":feature:launch"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:today"))
    implementation(project(":feature:trips"))
    implementation(project(":feature:trip"))
    implementation(project(":feature:wizard"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:reminders"))
    implementation(project(":core:model"))
    implementation(project(":core:plan"))
    implementation(project(":core:sync"))
    implementation(project(":core:reminders"))
    implementation(project(":core:wizard"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    debugImplementation(project(":core:data-mock"))
}
