plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
//    id ("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin") // Apply Safe Args here
    id("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
}

android {
    namespace = "com.hardik.calendarapp"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("/Users/dreamworld/Desktop/Projects/Hardik/CalendarApp/app/com_hardik_calendarapp.jks")
            storePassword = "com_hardik_calendarapp"
            keyAlias = "com_hardik_calendarapp"
            keyPassword = "com_hardik_calendarapp"
        }
    }

    defaultConfig {
        applicationId = "com.hardik.calendarapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }



    buildTypes {
        /*debug {
            isDebuggable = true
            isShrinkResources = true
            isMinifyEnabled = true//false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }*/
        release {
            isDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true//false
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.ui.graphics.android)
    implementation(libs.legacy.support.v4)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt DI
//    implementation ("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03") // Todo:hilt-lifecycle-viewmodel is outdated. Hilt’s lifecycle integration is now part of the core Hilt library, so you can remove the androidx.hilt:hilt-lifecycle-viewmodel dependency entirely.
    implementation ("com.google.dagger:hilt-android:2.48")
    ksp ("com.google.dagger:hilt-compiler:2.48")
//    kapt ("com.google.dagger:hilt-compiler:2.48.1")

    // Coroutine dependencies support
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle components
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Navigation components
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.3")

    // Retrofit for API calls
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:5.0.0-alpha.7")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.7")

    // Room Database | Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-common:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

//    implementation("androidx.room:room-runtime:2.6.1")
//    implementation ("androidx.room:room-ktx:2.6.1")
//    kapt ("androidx.room:room-compiler:2.6.1")


    // Glide for image loading
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Palette API works best with colorful images
    implementation(libs.androidx.palette.ktx)

    // Lottie: Load the JSON animation in your LottieAnimationView
    implementation ("com.airbnb.android:lottie:6.1.0")

    // UI and layout dependencies
    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.intuit.ssp:ssp-android:1.1.0")

    // Date and Time handling
    implementation ("joda-time:joda-time:2.12.7")

    // Google APIs and Calendar
    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.23.0")
    implementation("com.google.api-client:google-api-client-gson:1.31.5")
    implementation("com.google.http-client:google-http-client-jackson2:1.39.2")
    implementation("com.google.http-client:google-http-client-gson:1.39.2")
    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    // Activity extensions for ViewModel
    implementation ("androidx.activity:activity-ktx:1.7.2")
    // Fragment extensions for ViewModel
    implementation ("androidx.fragment:fragment-ktx:1.6.1")

//    implementation("com.kizitonwose.calendar:view:2.0.0")

    // Material Design Components
    implementation ("com.google.android.material:material:1.9.0")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics:19.2.1")

    implementation ("androidx.core:core-splashscreen:1.0.0-beta02")
}