plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.ksp)
}

val formalRelease = when (
    val value = providers.environmentVariable("GLYPH_FORMAL_RELEASE").orNull
        ?.trim()
        ?.lowercase()
) {
    null, "", "false" -> false
    "true" -> true
    else -> error("GLYPH_FORMAL_RELEASE must be true or false, but was '$value'")
}

android {
    namespace = gropify.project.app.packageName
    compileSdk = gropify.project.android.compileSdk

    signingConfigs {
        create("snapshot") {
            keyAlias = gropify.project.app.signing.keyAlias
            keyPassword = gropify.project.app.signing.keyPassword
            storeFile = rootProject.file(gropify.project.app.signing.storeFilePath)
            storePassword = gropify.project.app.signing.storePassword
        }
    }
    defaultConfig {
        applicationId = gropify.project.app.packageName
        minSdk = gropify.project.android.minSdk
        targetSdk = gropify.project.android.targetSdk
        versionName = gropify.project.app.versionName
        versionCode = gropify.project.app.versionCode
    }
    buildTypes {
        release {
            // Formal builds stay unsigned until the isolated workflow signing step.
            // Gradle and its plugins never receive the production key or passwords.
            signingConfig = signingConfigs.getByName("snapshot").takeUnless { formalRelease }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.versionName.map { versionName ->
                    "Glyph-$versionName.apk"
                }
            )
        }
    }
}

dependencies {
    val yukiHookBom = platform(libs.yukihook.bom)
    implementation(yukiHookBom)
    ksp(yukiHookBom)
    implementation(libs.yukihook.core)
    implementation(libs.yukihook.runtime.libxposed)
    ksp(libs.yukihook.compiler)
    implementation(platform(libs.kavaref.bom))
    implementation(libs.kavaref.core)

    // YukiHook's modern runtime needs the framework API at compile time only.
    compileOnly(libs.libxposed.api)
    // Keep the existing transactional remote-config transport and preference listeners.
    implementation(libs.libxposed.service)
    implementation(libs.okhttp)
    implementation(libs.androidx.core.ktx)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.androidx.navigationevent)
    implementation(libs.hiddenapibypass)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.miuix.ui.android)
    implementation(libs.miuix.icons.android)
    implementation(libs.miuix.preference.android)
    implementation(libs.miuix.blur.android)
    implementation(libs.miuix.nav.android)
    testImplementation(libs.junit)
}
