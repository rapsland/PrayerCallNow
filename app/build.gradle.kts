import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val vMajor = versionProps.getProperty("VERSION_MAJOR", "0").trim().toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR", "1").trim().toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH", "0").trim().toInt()
val appVersionName = "$vMajor.$vMinor.$vPatch"
val appVersionCode = vMajor * 10000 + vMinor * 100 + vPatch
val buildDateSuffix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

android {
    namespace = "com.salanow.ezannow"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.salanow.ezannow"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        val apkFileName = "EzanNow_v${appVersionName}_${buildDateSuffix}_${variant.buildType.name}.apk"
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = apkFileName
        }
        packageApplicationProvider.configure {
            doFirst {
                val outDir = outputDirectory.get().asFile
                if (outDir.exists()) {
                    outDir.listFiles { f ->
                        f.isFile && f.extension.equals("apk", ignoreCase = true)
                    }?.forEach { it.delete() }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
