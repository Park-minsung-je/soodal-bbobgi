import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
}

// 서버 주소·OAuth 키는 소스에 박지 않는다 — git 미추적 local.properties(또는 환경변수)에서 읽는다.
// 키 목록과 형식은 local.properties.sample 참고.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * 빌드 설정값을 local.properties → 환경변수 순으로 찾는다.
 *
 * @param key 설정 키
 * @return 찾은 값. 없으면 빌드를 즉시 실패시킨다 (빈 값으로 잘못 빌드되는 것보다 낫다)
 */
fun buildSecret(key: String): String = localProps.getProperty(key)
    ?: System.getenv(key)
    ?: throw GradleException(
        "빌드 설정 '$key'를 찾을 수 없습니다. " +
            "local.properties.sample을 local.properties로 복사한 뒤 값을 채우세요.",
    )

// 릴리즈 서명 — keystore.properties가 있을 때만 활성 (파일·비밀번호는 git 미추적).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val soodalBaseUrl = buildSecret("SOODAL_BASE_URL")
val soodalAssetBaseUrl = buildSecret("SOODAL_ASSET_BASE_URL")
val soodalKakaoNativeAppKey = buildSecret("SOODAL_KAKAO_NATIVE_APP_KEY")
val soodalGoogleWebClientId = buildSecret("SOODAL_GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "kr.ilf.soodalbbobgi"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.ilf.soodalbbobgi"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        // 0.1.0에서 시작해 **dev에 머지할 때마다 패치 자리를 +1** 한다.
        // 마이너·메이저 자리는 사용자가 지시할 때만 올린다.
        versionName = "0.1.16"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$soodalBaseUrl\"")
        buildConfigField("String", "ASSET_BASE_URL", "\"$soodalAssetBaseUrl\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$soodalKakaoNativeAppKey\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = soodalKakaoNativeAppKey
        // Google Sign-In: idToken의 audience(aud) 클레임이 이 값으로 박힌다.
        // 서버 검증 시 audience 비교용 ID. Android client ID(SHA-1 매칭용)는 코드에 안 들어감.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$soodalGoogleWebClientId\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$soodalBaseUrl\"")
            buildConfigField("String", "ASSET_BASE_URL", "\"$soodalAssetBaseUrl\"")
            // 디버그도 릴리즈 키로 서명 — 디버그↔릴리즈를 지우지 않고 덮어쓸 수 있고,
            // OAuth 지문 등록도 하나로 충분해진다. 키가 없는 PC는 기본 디버그 키 폴백.
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // keystore.properties가 있으면 릴리즈 키 서명, 없으면(다른 PC 등) 디버그 키 폴백.
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
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
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
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
    implementation(libs.core.splashscreen)
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
    implementation(libs.haze)
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
