plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.izak.demobankingapp20260118"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.izak.demobankingapp20260118"
        minSdk = 29
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Core AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")

    // Firebase (BoM)
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-analytics")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

}


//dependencies {
//    implementation(libs.appcompat)
//    implementation(libs.material)
//    implementation(libs.activity)
//    implementation(libs.constraintlayout)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.ext.junit)
//    androidTestImplementation(libs.espresso.core)
//
//
//
//
//    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
//
//    implementation("com.google.firebase:firebase-analytics")
//
//
//
//    // AndroidX Core
//    implementation(libs.appcompat)
//    implementation(libs.material)
//    implementation(libs.constraintlayout)
//    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
//
//    // Navigation Component
//    implementation("androidx.navigation:navigation-fragment:2.7.5")
//    implementation("androidx.navigation:navigation-ui:2.7.5")
//
//    // Firebase (using BoM)
//    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
//    implementation("com.google.firebase:firebase-auth")
//    implementation("com.google.firebase:firebase-database")
//    implementation("com.google.firebase:firebase-analytics")
//
//    // RecyclerView
//    implementation("androidx.recyclerview:recyclerview:1.3.2")
//    implementation("androidx.cardview:cardview:1.0.0")
//
//    // Lifecycle
//    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
//    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
//
//    // Testing
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.ext.junit)
//    androidTestImplementation(libs.espresso.core)
//
//}