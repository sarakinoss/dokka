import org.jetbrains.configureBintrayPublication

dependencies {
    implementation("org.jsoup:jsoup:1.12.1")
}

publishing {
    publications {
        register<MavenPublication>("basePlugin") {
            artifactId = "dokka-base"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
}

configureBintrayPublication("basePlugin")
