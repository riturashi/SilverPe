plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.akaalpay"
    compileSdk = 35



    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.akaalpay"
        minSdk = 24
        targetSdk = 35
        versionCode = 27
        versionName = "2.7"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            // 🔹 2. Release build mein key ko link kiya
//            signingConfig = signingConfigs.getByName("release")
//
//            proguardFiles(
//                    getDefaultProguardFile("proguard-android-optimize.txt"),
//                    "proguard-rules.pro"
//            )
//        }
//
//        // Debug mein bhi same key use karne ke liye (Optional but helpful)
//        getByName("debug") {
//            signingConfig = signingConfigs.getByName("release")
//        }
//    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
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
    implementation(libs.activity.ktx)
    // ================= CORE & UI =================
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    // ================= NETWORK & JSON =================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // ================= GOOGLE SERVICES =================
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // ================= IMAGE & UTILS =================
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.print)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("io.github.youth5201314:banner:2.2.2")
    implementation("com.karumi:dexter:4.2.0")
    implementation("com.github.yalantis:ucrop:2.2.8")

    // ================= MICRO ATM SDK FILES (LIBS) =================
    implementation(files("libs/paysprint_matm_live-release.aar"))
    implementation(files("libs/finosdk-release19Nov.aar"))
    implementation(files("libs/onboardinglib-release.aar"))

    // ================= SDK SUPPORT LIBRARIES =================
    implementation("de.greenrobot:greendao:2.1.0")
    implementation("org.greenrobot:eventbus:3.3.1")

    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.google.zxing:core:3.5.1")

    // ================= TEST =================
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.viewpager2:viewpager2:1.0.0")

}


configurations.all {
    resolutionStrategy {
        force("androidx.media3:media3-common:1.1.1")
        force("org.greenrobot:eventbus:3.3.1")
    }
}