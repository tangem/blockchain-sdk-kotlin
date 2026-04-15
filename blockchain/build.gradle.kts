plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.wire)
}

apply(from = "../upload-github.gradle.kts")

android {
    namespace = "com.tangem.demo.blockchain"

    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
            merges += "paymentrequest.proto"
            excludes += listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6pro33") {
                    device = "Pixel 6 Pro"
                    apiLevel = 33
                }
            }
        }
    }
}

tasks.register<Jar>("sourceJar") {
    dependsOn(tasks.withType<GenerateModuleMetadata>())
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

configurations {
    matching { !it.name.lowercase().contains("test") }.configureEach {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    matching { it.name.lowercase().contains("unittest") }.configureEach {
        exclude(group = "network.lightsail", module = "stellar-sdk-android-spi")
    }
}

dependencies {
    implementation(libs.tangem.sdk.core)
    implementation(libs.tangem.sdk.android)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.network.retrofit)
    implementation(libs.network.converter.moshi)
    implementation(libs.network.moshi)
    implementation(libs.network.moshi.kotlin)
    ksp(libs.network.moshi.kotlin.codegen)

    implementation(libs.coroutines.core)

    implementation(libs.bitcoin.bitcoinj)

    // Bitcoin PSBT support (BIP 174) - requires Kotlin 2.x
    implementation(libs.bitcoin.kmp.jvm)
    implementation(libs.bitcoin.secp256k1.kmp.jni.jvm)

    implementation(libs.stellar.sdk) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    implementation(libs.stellar.sdk.android.spi) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }

    implementation(libs.dogecoin.libdohj)

    implementation(libs.crypto.spongycastle.core)
    implementation(libs.crypto.spongycastle.prov)

    implementation(libs.kethereum.extensions.kotlin)
    implementation(libs.kethereum.extensions.transactions)
    implementation(libs.kethereum.erc55)
    implementation(libs.kethereum.keccak.shortcut)
    implementation(libs.kethereum.wallet)
    implementation(libs.kethereum.crypto.impl.spongycastle)
    implementation(libs.kethereum.crypto)
    implementation(libs.kethereum.crypto.api)
    implementation(libs.kethereum.model)
    implementation(libs.kethereum.rlp)
    implementation(libs.kethereum.abi)
    implementation(libs.kethereum.types)

    implementation(libs.komputing.khex.extensions)

    implementation(libs.crypto.cbor)

    implementation(files("libs/ripple-core-0.0.1.jar"))
    implementation(files("libs/solanaj-0.0.10.jar"))
    implementation(files("libs/polkaj-root-0.5.1-SNAPSHOT.jar"))

    implementation(files("libs/hedera-sdk-java-2.29.0.jar"))
    @Suppress("AARDependency")
    implementation(libs.tangem.blstlib)
    @Suppress("AARDependency")
    implementation(libs.tangem.wallet.core)
    implementation(libs.tangem.wallet.core.proto) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }

    implementation(libs.crypto.eddsa)

    // dependencies for binance
    implementation(libs.binance.grpc.protobuf)
    implementation(libs.binance.grpc.stub)
    implementation(libs.binance.commons.codec)
    implementation(libs.binance.commons.lang3)
    implementation(libs.binance.jackson.datatype.joda)
    implementation(libs.binance.joda.time)
    implementation(libs.network.converter.jackson)

    // dependencies for polkadot
    implementation(libs.polkadot.multibase)
    implementation(libs.polkadot.zero.allocation.hashing)

    // for hedera sdk
    implementation(libs.hedera.grpc.okhttp)
    implementation(libs.crypto.bouncycastle)
    implementation(libs.hedera.protobuf.java)

    // solana sdk with v0 transactions support
    implementation(libs.solana.metaplex)
    implementation(libs.solana.ditchoom.buffer)

    // dependencies for TON
    implementation(libs.ton.kotlin.tvm)
    implementation(libs.ton.kotlin.block.tlb)

    // region dependencies for ICP
    api(libs.icp.ic4j.agent) {
        exclude(group = "org.bouncycastle")
        exclude(group = "org.apache.httpcomponents.client5", module = "httpclient5")
        exclude(group = "jakarta.xml.bind", module = "jakarta.xml.bind-api")
    }
    implementation(libs.icp.ic4j.candid) {
        exclude(group = "org.bouncycastle")
        exclude(group = "jakarta.xml.bind", module = "jakarta.xml.bind-api")
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }
    implementation(libs.icp.jackson.databind)
    implementation(libs.icp.jackson.datatype.jdk8)
    implementation(libs.icp.jackson.dataformat.cbor)
    // end region

    implementation(libs.coroutines.okhttp)

    testImplementation(libs.test.junit4)
    testImplementation(libs.test.junit.jupiter.api)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.json.org)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.robolectric)
    androidTestImplementation(libs.test.truth)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.coroutines)
    androidTestImplementation(libs.test.mockk.android)

    implementation(libs.logging.slf4j)

    implementation(libs.serialization.json)
    implementation(libs.serialization.protobuf)
}

// Protobuf
wire {
    sourcePath {
        srcDir("src/main/java/com/tangem/blockchain/blockchains/tron/proto")
        srcDir("src/main/java/com/tangem/blockchain/blockchains/koinos/proto")
    }
    kotlin {}
}