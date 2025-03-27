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
    
    // Add packaging options to handle duplicate files and other issues
    packaging {
        resources {
            // Exclude other potential conflict files
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            
            // Add these to resolve the specific conflict
            excludes += "META-INF/mimetypes.default"
            excludes += "META-INF/mailcap.default"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        
        // Enable desugaring for Java 8+ API support
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
    
    // Original MongoDB Java Driver with packaging exclusions and excluding bson-record-codec
    // implementation("org.mongodb:mongodb-driver-sync:4.9.1") {
    //     exclude(group = "org.mongodb", module = "bson-record-codec")
    //     // Exclude the conflicting activation dependency
    //     exclude(group = "javax.activation", module = "activation")
    // }
    
    // RxJava for async operations
    // implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    // implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    
    // Core library desugaring (Java 8+ APIサポート)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // Add JNDI support for MongoDB SRV connections - use only one activation library
    // implementation("com.sun.activation:javax.activation:1.2.0")
    // implementation("com.sun.mail:javax.mail:1.6.2") {
    //     // Exclude the older activation dependency that might be pulled in by javax.mail
    //     exclude(group = "javax.activation", module = "activation")
    // }
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.9.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}