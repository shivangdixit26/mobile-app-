import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bugsmirror.riskchecker"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // Host apps that turn on minify (R8) pick these rules up automatically.
        consumerProguardFiles("consumer-rules.pro")
    }

    // A library has no targetSdk of its own (the host app's one is used), so these
    // keep lint and unit tests on the same target as samplehost.
    lint {
        targetSdk = 36
    }
    testOptions {
        targetSdk = 36
        // Lets code that calls android.util.Log run in plain JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Only the small annotations artifact (@MainThread, @WorkerThread). An SDK that goes
    // inside other companies' apps should bring as few dependencies as possible.
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    // Real org.json for unit tests: android.jar only contains empty stubs of it.
    testImplementation(libs.org.json)
}
