plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gsi.mrzandnfc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gsi.mrzandnfc"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
splits {
    abi {
        isEnable= true
        reset()
        include("armeabi-v7a")
        isUniversalApk = false
    }
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

    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runner)
    implementation(libs.firebase.ml.modeldownloader.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
   // api(project(":imageCropper"))


    implementation ("org.jmrtd:jmrtd:0.7.18")

    implementation ("net.sf.scuba:scuba-sc-android:0.0.18")
    implementation ("com.madgag.spongycastle:prov:1.54.0.0")
    implementation ("com.gemalto.jp2:jp2-android:1.0.3")
    implementation ("com.github.mhshams:jnbis:1.1.0")
    implementation ("org.bouncycastle:bcpkix-jdk15on:1.65")// do not update
    implementation("commons-io:commons-io:2.11.0")
    //oogleImplementation ('com.google.android.gms:play-services-ads:22.5.0')
  //  googleImplementation ('com.google.android.play:review-ktx:2.0.1')
    implementation ("androidx.concurrent:concurrent-futures:1.1.0")
    implementation ("com.google.guava:guava:31.0.1-android")
    implementation ("androidx.profileinstaller:profileinstaller:1.3.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation ("com.google.mlkit:text-recognition:16.0.1")

    /*
    implementation ("com.google.mlkit:language-id:17.0.4")
    implementation ("com.google.mlkit:common:16.0.0")
    */

    //implementation("com.google.firebase:firebase-ml-vision-text-model:16.0.0")

        // implementation ("com.rmtheis:tess-two:9.1.0")
   /* implementation ("cz.adaptech.tesseract4android:tesseract4android:4.8.0")
    implementation ("org.opencv:opencv-android:4.5.1")*/
    //implementation ("org.opencv:opencv-android:4.9.0")
  //  implementation ("com.google.android.gms:play-services-mlkit-text-recognition-japanese")
}

  //
