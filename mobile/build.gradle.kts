plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val devKeyFile = rootProject.layout.buildDirectory
    .file("dev-signing/happytalkie-dev.p12")
    .get().asFile

if (!devKeyFile.exists()) {
    devKeyFile.parentFile.mkdirs()
    val encoded = rootProject.file("dev-keystore/happytalkie-dev.p12.b64")
        .readText()
        .trim()
    devKeyFile.writeBytes(java.util.Base64.getDecoder().decode(encoded))
}

android {
    namespace = "com.xiaolong.happytalkie.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xiaolong.happytalkie"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("repoDev") {
            storeFile = devKeyFile
            storePassword = "happytalkie-dev"
            keyAlias = "happytalkie"
            keyPassword = "happytalkie-dev"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("repoDev")
        }
        release {
            signingConfig = signingConfigs.getByName("repoDev")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
}
