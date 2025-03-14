plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.StoneCode.rain_alert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.StoneCode.rain_alert"
        minSdk = 34
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"  // Using semantic versioning for clearer version tracking

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Enable code optimization for release builds
            isDebuggable = false
        }
        
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            // Allow debugging for development builds
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation (libs.google.maps.compose.v2110)
    implementation (libs.play.services.maps.v1810)

    // Ground Overlay implementation for Google Maps
    implementation(libs.android.maps.utils)

    // Pager for horizontal carousel
    implementation(libs.androidx.foundation)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    implementation(libs.androidx.material.icons.extended)

    // Retrofit for API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Location services
    implementation(libs.play.services.location)
    
    // Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    
    // Coil for image loading
    implementation(libs.coil.compose)
    
    // Firebase    
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.analytics.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.activity.compose.v182)
    implementation(platform(libs.androidx.compose.bom.v20230800))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.appcompat)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20241201))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // LiveData
    implementation(libs.androidx.runtime.livedata.v154)
    implementation(libs.okhttp)
}