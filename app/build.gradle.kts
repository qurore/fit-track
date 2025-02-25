plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.qurore.fittrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.qurore.fittrack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Auth0 configuration
        manifestPlaceholders["auth0Domain"] = "@string/com_auth0_domain"
        manifestPlaceholders["auth0Scheme"] = "@string/com_auth0_scheme"
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
        
        // デシュガリングを有効化
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Auth0 dependencies
    implementation("com.auth0.android:auth0:2.10.2")
    
    // Edge-to-Edge support
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.window:window:1.2.0")
    
    // MongoDB Java Driver
    implementation("org.mongodb:mongodb-driver-sync:4.9.1")
    
    // RxJava for async operations (optional but recommended)
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    
    // Core library desugaring (Java 8+ APIサポート)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}