plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
}


android {
    namespace = "com.example.LockerApp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.LockerApp"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.play.services.drive)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // ViewModel for Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.8.6")

    // LiveData (หากต้องการใช้งาน LiveData)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Navigation for Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // MQTT
    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation ("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")



    implementation ("androidx.fragment:fragment-ktx:1.8.4")
    implementation ("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.compose.material:material:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    //qrcode
    implementation("com.google.zxing:core:3.4.1")


    //Google Drive
    implementation("com.google.api-client:google-api-client-android:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")
    implementation("com.google.android.gms:play-services-auth:20.5.0")
    implementation("com.google.http-client:google-http-client-gson:1.41.1")
    implementation("com.google.code.gson:gson:2.8.9")




    //db
    implementation ("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.room:room-ktx:2.6.1")


    implementation ("io.coil-kt:coil-compose:2.3.0")




    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.7.3")



}