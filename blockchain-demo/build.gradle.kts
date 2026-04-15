plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tangem.blockchain"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.blockchain_demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf(
                "lib/x86_64/darwin/libscrypt.dylib",
                "lib/x86_64/freebsd/libscrypt.so",
                "lib/x86_64/linux/libscrypt.so",
            )
            merges += "paymentrequest.proto"
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":blockchain"))

    implementation(libs.tangem.sdk.core.demo)
    implementation(libs.tangem.sdk.android.demo)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.jdk8)
    implementation(libs.coroutines.android)

    testImplementation(libs.test.junit4)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.espresso.core)

    debugImplementation(libs.test.chucker)
    releaseImplementation(libs.test.chucker.no.op)
}