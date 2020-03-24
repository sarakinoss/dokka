import org.jetbrains.configureBintrayPublication

publishing {
    publications {
        register<MavenPublication>("javadocPlugin") {
            artifactId = "javadoc-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.soywiz.korlibs.korte:korte:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    implementation(project(":plugins:base"))

}

configureBintrayPublication("javadocPlugin")