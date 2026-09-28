import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

abstract class EnvFileSource : ValueSource<String, EnvFileSource.Params> {
    interface Params : ValueSourceParameters {
        val envFile: RegularFileProperty
    }

    override fun obtain(): String {
        val file = parameters.envFile.asFile.get()
        return if (file.isFile) file.readText() else ""
    }
}

val dotEnv = parseDotEnv(
    providers.of(EnvFileSource::class.java) {
        parameters.envFile.set(rootProject.layout.projectDirectory.file(".env"))
    }.get()
)

fun parseDotEnv(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()
    val values = linkedMapOf<String, String>()
    raw.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val separator = line.indexOf('=')
        if (separator <= 0) return@forEach
        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
        values.putIfAbsent(key, value)
    }
    return values
}

fun javaStringLiteral(value: String): String {
    val escaped = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }
    return "\"" + escaped + "\""
}

android {
    namespace = "com.example.fitcore"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.fitcore"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_API_KEY", javaStringLiteral(dotEnv["OPENAI_API_KEY"].orEmpty()))
        buildConfigField("String", "OPENAI_MODEL", javaStringLiteral(dotEnv["OPENAI_MODEL"].orEmpty()))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.fragment)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.glide)
    ksp(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
