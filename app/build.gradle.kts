plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.soodalbbobgi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soodalbbobgi.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"https://bbobgi.soodal.ilf.kr/v1/\"")
        buildConfigField("String", "ASSET_BASE_URL", "\"https://bbobgi.soodal.ilf.kr\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"ff9ecdb8cae1ebf2c9541f3aee571cca\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = "ff9ecdb8cae1ebf2c9541f3aee571cca"
        // Google Sign-In: idToken의 audience(aud) 클레임이 이 값으로 박힌다.
        // 서버 검증 시 audience 비교용 ID. Android client ID(SHA-1 매칭용)는 코드에 안 들어감.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"338800630296-qrm3niallrtf804gi2n12qbcu2e0gg40.apps.googleusercontent.com\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://bbobgi.soodal.ilf.kr/v1/\"")
            buildConfigField("String", "ASSET_BASE_URL", "\"https://bbobgi.soodal.ilf.kr\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 테스트 설치용 디버그 서명 — OAuth(SHA-1/키해시)도 디버그 키 기준 등록이라 그대로 동작.
            // Play 출시 시 릴리즈 키스토어로 교체.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.health.connect)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.kakao.user)
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)
    implementation(libs.coroutines.android)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.aboutlibraries.core)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.coroutines.test)
}
