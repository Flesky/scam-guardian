import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

android {
    namespace = "ph.scamguardian"
    compileSdk = 37
    defaultConfig {
        applicationId = "ph.scamguardian"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // data/brands.json is the only copy of the brand catalog; it is packaged as an asset from there.
    // models/ holds the model, which downloadModel puts there: it is too big for git.
    sourceSets {
        getByName("main") {
            assets.directories.add(rootProject.file("data").path)
            assets.directories.add(rootProject.file("models").path)
        }
    }

    // The model is stored as it is, so the app can ask for its size and the build does not compress 485 MB.
    androidResources {
        noCompress.add("litertlm")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Every build that packages the app also packages the model. It is downloaded once, the first time.
val downloadModel by tasks.registering(DownloadModel::class) {
    url =
        "https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm/resolve/main/embeddinggemma-2-740m.litertlm"
    sha256 = "e7a8a2204b91e0f96e92960e84a09a89212e1633dcb7575a9bf3378b4df77f4c"
    target = rootProject.layout.projectDirectory.file("models/embeddinggemma-2-740m.litertlm")
}

tasks.matching { it.name.matches(Regex("merge\\w+Assets")) }.configureEach { dependsOn(downloadModel) }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Static analysis
    detektPlugins(libs.compose.rules.detekt)

    // Scam-detection pipeline (core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.text)
    implementation(libs.icu4j)
    implementation(libs.url.detector)
    implementation(libs.guava)

    // On-device AI
    implementation(libs.litertlm.android)
}

/** Downloads the model into models/ when it is not there yet, and checks that it is the expected file. */
@DisableCachingByDefault(because = "Downloads a file")
abstract class DownloadModel : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:Internal
    abstract val target: RegularFileProperty

    @TaskAction
    fun download() {
        val file = target.get().asFile
        if (file.isFile) return
        logger.lifecycle("Downloading the model (485 MB) to $file")
        file.parentFile.mkdirs()
        val partial = File(file.path + ".part")
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(URI(url.get()).toURL().openStream(), digest).use { input ->
            partial.outputStream().use { input.copyTo(it) }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != sha256.get()) {
            partial.delete()
            throw GradleException("The downloaded model is not the expected file: SHA-256 is $actual")
        }
        if (!partial.renameTo(file)) throw GradleException("Could not move the model to $file")
    }
}
