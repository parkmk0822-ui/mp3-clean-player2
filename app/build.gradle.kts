plugins {
    alias(libs.plugins.app)
}

dependencies {
    implementation(project(":data:auth"))
    implementation(project(":data:element"))
    implementation(project(":domain:auth"))
    implementation(project(":domain:common"))
    implementation(project(":domain:element"))
    implementation(project(":ui:auth"))
    implementation(project(":ui:common"))
    implementation(project(":ui:element-details"))
    implementation(project(":ui:element-list"))
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation(project(":ui:home"))
}
