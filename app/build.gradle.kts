plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Убираем явное подключение kapt, так как он уже включен в kotlin-android
    // Вместо этого используем kotlin("kapt") в блоке зависимостей для Glide
}

android {
    namespace = "com.example.topmemes"
    compileSdk = 35  // Рекомендую использовать 34 вместо 35 для стабильности

    defaultConfig {
        applicationId = "com.example.topmemes"
        minSdk = 24
        targetSdk = 35  // Синхронизируем с compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true  // Добавляем поддержку векторных изображений
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
        viewBinding = true
    }
}

dependencies {
    // Базовые зависимости Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Nearby Connections
    implementation("com.google.android.gms:play-services-nearby:18.7.0")

    // Зависимости для работы с мемами
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)  // Используем annotationProcessor вместо kapt

    // Тестовые зависимости (убраны дубликаты)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}