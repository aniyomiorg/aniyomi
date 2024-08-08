buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.android.shortcut.gradle)
        classpath(libs.google.services.gradle)
        classpath(libs.aboutLibraries.gradle)
        classpath(libs.sqldelight.gradle)
        classpath(libs.moko.gradle)
    }
}

plugins {
    alias(kotlinx.plugins.serialization) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
