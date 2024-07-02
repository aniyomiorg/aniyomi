import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    dependencies {
        classpath(libs.android.shortcut.gradle)
        classpath(libs.aboutLibraries.gradle)
        classpath(libs.sqldelight.gradle)
        classpath(libs.moko.gradle)
    }
}

plugins {
    alias(kotlinx.plugins.serialization) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
}

subprojects {
    tasks.withType<KotlinJvmCompile> {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }

    plugins.withType<BasePlugin> {
        plugins.apply("detekt")
        configure<BaseExtension> {
            compileSdkVersion(AndroidConfig.compileSdk)
            defaultConfig {
                minSdk = AndroidConfig.minSdk
                targetSdk = AndroidConfig.targetSdk
                ndk {
                    version = AndroidConfig.ndk
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = true
            }

            dependencies {
                add("coreLibraryDesugaring", libs.desugar)
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
