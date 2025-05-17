plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.siaga"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.siaga"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.hivemq:hivemq-mqtt-client:1.3.0")
    implementation("com.android.support:support-v4:28.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}