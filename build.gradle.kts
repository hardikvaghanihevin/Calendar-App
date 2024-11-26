// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
//    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
//    id ("com.google.dagger.hilt.android") version "2.48.1" apply false

//    id("com.android.application") version "8.1.2" apply false
//    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("androidx.navigation.safeargs") version "2.7.3" apply false // Add Safe Args plugin here
}