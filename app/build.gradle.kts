plugins {
  id("com.android.application")
  kotlin("android")
}

android {
  namespace = "com.example.audeonbrowser"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.example.audeonbrowser"
    minSdk = 28
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.activity:activity-ktx:1.9.2")
  implementation("androidx.webkit:webkit:1.11.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
