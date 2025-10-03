plugins {
    kotlin("jvm") version "2.2.0"               // match the platform’s Kotlin line
    id("org.jetbrains.intellij.platform") version "2.9.0"
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

kotlin {
    jvmToolchain(21)                            // Kotlin -> Java 21
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))  // Java toolchain 21
}

dependencies {
    intellijPlatform {
        // Choose ONE of the two:

        // A) Use local installed IDE (requires IDEA_HOME pointing at 2025.2 / 252.*)
        // local(System.getenv("IDEA_HOME"))

        // B) Let Gradle fetch the 252 line for you:
        intellijIdeaCommunity("2025.2.3")       // any 252.x.y works
        // If you need Java plugin APIs:
        // bundledPlugin("com.intellij.java")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("252")                   // compatible with your IC-252.*
        // no untilBuild to avoid blocking newer minor updates
    }
}
