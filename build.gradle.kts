// Top-level build file where you can add configuration options common to all sub-projects/modules.

// ---------------------------------------------------------------------------
// Kotlin compiler override (see docs/references/DEVELOPMENT_CONTEXT.md §6-1)
//
// AGP 9.x ships a built-in Kotlin compiler (2.2.10 for AGP 9.4.x). The ML Kit
// GenAI artifacts are compiled with Kotlin 2.4.x, whose metadata a 2.2.x
// compiler refuses to read. Promoting kotlin-gradle-plugin on the buildscript
// classpath makes AGP's built-in compilation (and the Compose compiler plugin)
// run on a compiler that understands that metadata.
// ---------------------------------------------------------------------------
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
