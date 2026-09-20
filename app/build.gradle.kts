import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.makerandreas.papirusoffice"
    minSdk = 24
    targetSdk = 36
    
    // Dynamic versioning with environment variable support (CI/CD) and local fallbacks
    val envVersionName = System.getenv("APP_VERSION_NAME")
    val envVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull()

    val semVerMajor = 1
    val semVerMinor = 0
    val semVerPatch = 0
    val preRelease = "nightly"
    val buildMetadata = "20260724"

    versionCode = envVersionCode ?: (semVerMajor * 1000000 + semVerMinor * 10000 + semVerPatch * 100 + 1)
    versionName = envVersionName ?: (if (preRelease.isNotEmpty()) {
      "$semVerMajor.$semVerMinor.$semVerPatch-$preRelease+$buildMetadata"
    } else {
      "$semVerMajor.$semVerMinor.$semVerPatch"
    })

    // Compute dynamic Papirus Engine version based on engine source changes.
    // NOTE: uses only file sizes + relative paths (never lastModified) so the
    // version is deterministic across checkouts and reproducible builds.
    val engineDir = file("src/main/java/com/makerandreas/papirusoffice")
    val engineVersion = if (engineDir.exists()) {
      var hashSum = 0L
      var fileCount = 0
      engineDir.walkTopDown().filter { it.isFile }.forEach { f ->
        hashSum += f.length() + f.relativeTo(engineDir).path.hashCode()
        fileCount++
      }
      val engineMajor = 1
      val engineMinor = 3
      // NOTE: kotlin.math is not available on the build-script classpath,
      // so the non-negative remainder is computed manually.
      val hashMod = ((hashSum % 100) + 100) % 100
      val enginePatch = fileCount + hashMod.toInt()
      "$engineMajor.$engineMinor.$enginePatch-engine"
    } else {
      "1.3.0-engine"
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Feature toggles for modular C++ / JNI OOXML compatibility layers
    buildConfigField("boolean", "ENABLE_OOXML_SUPPORT", "true")
    buildConfigField("boolean", "ENABLE_OMML_PARSER", "true")
    buildConfigField("String", "APP_VERSION_NAME", "\"$versionName\"")
    buildConfigField("String", "PAPIRUS_ENGINE_VERSION", "\"$engineVersion\"")

    // Membaca dari environment variable lokal atau CI/CD GitHub
    val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
    val cseCx = System.getenv("GOOGLE_CSE_CX") ?: ""
    val cseApiKey = System.getenv("GOOGLE_CSE_API_KEY") ?: ""
    val fontsApiKey = System.getenv("GOOGLE_FONTS_REST_API") ?: System.getenv("GOOGLE_FONTS_API_KEY") ?: ""

    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    buildConfigField("String", "GOOGLE_CSE_CX", "\"$cseCx\"")
    buildConfigField("String", "GOOGLE_CSE_API_KEY", "\"$cseApiKey\"")
    buildConfigField("String", "GOOGLE_FONTS_REST_API", "\"$fontsApiKey\"")
  }

  // Release keystore is optional at configuration time: fresh clones and CI jobs
  // without secrets must still be able to assemble debug builds. Release builds
  // are only signed when a keystore file is actually present (see buildTypes).
  val releaseKeyFile = if (file("papirus-release.jks").exists()) file("papirus-release.jks") else file("release.jks")
  val hasReleaseKeystore = releaseKeyFile.exists()

  signingConfigs {
    create("release") {
      if (hasReleaseKeystore) {
        storeFile = releaseKeyFile
        // Credentials come from environment or Gradle properties only; there are
        // intentionally no hardcoded fallback passwords in this script.
        storePassword = System.getenv("KEYSTORE_PASSWORD")
          ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
        keyAlias = System.getenv("KEY_ALIAS")
          ?: project.findProperty("KEY_ALIAS")?.toString()
          ?: "papirus_key"
        keyPassword = System.getenv("KEY_PASSWORD")
          ?: project.findProperty("KEY_PASSWORD")?.toString()
      }
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
      // Only sign when a keystore exists; otherwise produce an unsigned APK
      // instead of failing with "Keystore file not found".
      if (hasReleaseKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      isMinifyEnabled = false
      // Deliberately uses the standard auto-generated debug key so that
      // `./gradlew assembleDebug` works on a fresh clone with no secrets.
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  sourceSets {
    getByName("main") {
      assets.directories.addAll(listOf("src/main/assets", "src/main/share"))
      jniLibs.srcDir("src/main/libs")
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

}

// Selalu sertakan source stub Java/Kotlin di src/compileOnly/java untuk memastikan
// kompilasi sukses baik secara lokal maupun di GitHub Actions tanpa ketergantungan LibreOffice eksternal.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  source(file("src/compileOnly/java"))
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  // Jalur langsung yang jauh lebih stabil untuk memuat folder libs
  implementation(fileTree("libs"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
