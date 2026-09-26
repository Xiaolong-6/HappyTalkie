plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.xiaolong.happytalky.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    api("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("androidx.activity:activity:1.13.0")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")

    testImplementation("junit:junit:4.13.2")
}
