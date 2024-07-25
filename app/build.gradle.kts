import org.gradle.internal.impldep.bsh.commands.dir

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.prgguru.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.prgguru.example"
        minSdk = 24
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
    implementation(files("lib/gson-2.10.1.jar"))
    implementation(files("lib\\okhttp-3.0.0.jar"))
    implementation(files("lib\\okio-2.10.0.jar"))
    implementation(libs.mediarouter)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("androidx.appcompat:appcompat:1.4.1")
    implementation ("com.google.android.material:material:1.5.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation ("androidx.activity:activity-ktx:1.4.0")
    implementation ("androidx.core:core-ktx:1.7.0")

//    implementation ("com.google.code.gson:gson:2.10.1")
//    implementation ("com.squareup.okhttp3:okhttp:4.9.1")
//    implementation ("com.squareup.okio:okio:2.10.0")
//    implementation ("androidx.mediarouter:mediarouter:1.2.0")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}
