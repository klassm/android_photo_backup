buildscript {
    dependencies {
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.8.0.0")
    }

    repositories {
        google()
        mavenCentral()
    }
}


plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("jacoco")
}

apply {
    plugin("de.mannodermaus.android-junit5")
    plugin("kotlin-android-extensions")
}

android {
    compileSdk = 31
    buildToolsVersion = "30.0.2"
    defaultConfig {
        applicationId = "li.klass.photo_copy"
        minSdk = 29
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility  = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    lint {
        lintConfig = file("android-lint.xml")
    }
}

val arrowVersion = "0.12.1"
val roomVersion = "2.3.0"

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(kotlin("stdlib-jdk7", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("joda-time:joda-time:2.10.13")
    implementation("androidx.preference:preference:1.1.1")
    implementation("br.com.simplepass:loading-button-android:2.2.0")
    implementation("com.drewnoakes:metadata-extractor:2.12.0")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.exifinterface:exifinterface:1.3.3")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")

    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.2")
    testImplementation("org.assertj:assertj-core:3.21.0")
}
