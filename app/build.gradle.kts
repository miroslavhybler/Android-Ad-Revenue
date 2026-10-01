import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val admobWebClientId = readLocalProperty(name = "ADMOB_WEB_CLIENT_ID").escapedBuildConfigString()

fun readLocalProperty(name: String): String {
    val localPropertiesFile = rootProject.file("local.properties")
    if (!localPropertiesFile.isFile) return ""

    return Properties().apply {
        localPropertiesFile.inputStream().use(block = ::load)
    }.getProperty(name).orEmpty()
}

fun String.escapedBuildConfigString(): String {
    return this.replace(oldValue = "\\", newValue = "\\\\")
        .replace(oldValue = "\"", newValue = "\\\"")
}

android {
    flavorDimensions += "dataSource"
    productFlavors {
        create("real") { dimension = "dataSource" }
        create("mock") {
            dimension = "dataSource"
            applicationIdSuffix = ".mock"
        }
    }
    namespace = "mir.oslav.adrevenue"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mir.oslav.adrevenue"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "ADMOB_WEB_CLIENT_ID",
            "\"$admobWebClientId\"",
        )
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isShrinkResources = true
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(files("libs/jet-article-core-debug.aar"))
    implementation(files("libs/jet-article-ui-debug.aar"))
    implementation (libs.compose.charts)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.jet.utils)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.google.identity.googleid)
    implementation(libs.google.play.services.auth)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.hilt.android)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
