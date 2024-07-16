plugins {
    id("mihon.library")
    kotlin("multiplatform")
}

kotlin {
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.sourceApi)
                api(projects.i18n)

                implementation(libs.unifile)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.core.common)
                implementation(projects.coreMetadata)

                // Move ChapterRecognition to separate module?
                implementation(projects.domain)

                implementation(kotlinx.bundles.serialization)
            }
        }
    }
}

android {
    namespace = "tachiyomi.source.local"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    dependencies {
        // FFmpeg-kit
        implementation(libs.ffmpeg.kit)
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            ),
        )
    }
}
