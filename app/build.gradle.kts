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
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}