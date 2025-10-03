import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.gravity"
version = "0.2.1"

dependencies {
    intellijPlatform {
        // Download IntelliJ 2025.2.3 (build line 252) automatically
//        intellijIdeaCommunity("2025.2.3")
        local(System.getenv("IDEA_HOME"))
        // If you need Java PSI APIs:
        bundledPlugin("com.intellij.java")
    }
}

// Include generated sources
sourceSets {
    main {
        java.srcDirs("src/main/gen")
    }
}

intellijPlatform {
    // Disable building searchable options to keep builds faster/quieter
    buildSearchableOptions.set(false)
}

// Grammar-Kit tasks (new property names)
tasks.register<GenerateLexerTask>("generateDroolsLexer") {
    sourceFile.set(file("src/main/grammars/Drools.flex"))
    // NEW: targetOutputDir instead of targetDir; pass a File, not String
    targetOutputDir.set(file("src/main/gen/com/gravity/drools/lexer"))
    purgeOldFiles.set(true)
    // DO NOT set targetClass here — it's defined in the .flex via %class DroolsLexer
}

tasks.register<GenerateParserTask>("generateDroolsParser") {
    sourceFile.set(file("src/main/grammars/Drools.bnf"))
    // NEW: targetRootOutputDir instead of targetRoot
    targetRootOutputDir.set(file("src/main/gen"))
    // Use paths relative to targetRootOutputDir, NO leading slash
    pathToParser.set("com/gravity/drools/parser/DroolsParser.java")
    pathToPsiRoot.set("com/gravity/drools/psi")
    purgeOldFiles.set(true)
}

// Ensure parser/lexer run before compilation
tasks.named("compileKotlin") { dependsOn("generateDroolsLexer", "generateDroolsParser") }
tasks.named("compileJava")   { dependsOn("generateDroolsLexer", "generateDroolsParser") }

tasks {
    patchPluginXml {
        sinceBuild.set("252")
        // No untilBuild, so it works with all 252.x updates
    }
}
