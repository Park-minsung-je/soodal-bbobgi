package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kr.ilf.soodalbbobgi.BuildConfig
import kr.ilf.soodalbbobgi.data.asset.AssetStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 컴포저블에서 [AssetStore]를 ApplicationComponent로부터 꺼내기 위한 Hilt EntryPoint.
 *
 * Compose 트리는 ViewModel이 아니므로 `@Inject` 주입 경로가 없다.
 * 임의 컴포저블 어디서나 같은 Singleton 인스턴스를 얻기 위해 EntryPointAccessors를 사용한다.
 * MainActivity에 CompositionLocal을 두는 방식 대비 진입점 변경이 없어 덜 침습적이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AssetStoreEntryPoint {
    fun assetStore(): AssetStore
}

/**
 * Compose 트리에서 사용할 Singleton [AssetStore]를 EntryPoint로부터 얻어 기억한다.
 * Application context 기준으로 lookup하므로 어떤 Activity/Composition에서 호출해도 동일 인스턴스.
 */
@Composable
private fun rememberAssetStore(): AssetStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context, AssetStoreEntryPoint::class.java)
            .assetStore()
    }
}

/**
 * Coil이 사용할 model 객체를 결정한다 — 로컬 에셋이 있으면 [java.io.File], 없으면 fallback URL.
 *
 * 서버 아이템의 imageAsset은 URL 기준 절대 경로("/assets/bg/bg_0.png")지만 로컬 매니페스트의
 * path는 assets 루트 기준 상대 경로("bg/bg_0.png")다 — 접두사를 벗겨 로컬 캐시를 먼저 찾고,
 * 없으면 네트워크로 폴백한다 (첫 sync 전/오프라인에서도 화면이 깨지지 않도록).
 *
 * @param assetStore 로컬 에셋 저장소 (Singleton)
 * @param imageAsset 서버 절대 경로("/assets/…") 또는 매니페스트 상대 경로. null/공백이면 null 반환.
 * @return 로컬 파일이 존재하면 [java.io.File], 아니면 `${ASSET_BASE_URL}${imageAsset}` 문자열,
 *         imageAsset이 null/blank면 null
 */
fun resolveAssetModel(assetStore: AssetStore, imageAsset: String?): Any? {
    if (imageAsset.isNullOrBlank()) return null
    val relativePath = imageAsset.removePrefix("/assets/").removePrefix("/")
    val local = assetStore.fileFor(relativePath)
    return if (local.exists()) local else "${BuildConfig.ASSET_BASE_URL}$imageAsset"
}

/**
 * 매니페스트 상대 경로로부터 이미지를 표시한다 — 로컬 우선, 네트워크 폴백.
 *
 * 동작:
 * 1. [imageAsset]이 null/공백이면 빈 [AsyncImage]를 그린다 (placeholder/error만 노출).
 * 2. [AssetStore.fileFor]로 로컬 파일을 조회하고 존재하면 File을 model로 사용.
 * 3. 로컬에 없으면 `${ASSET_BASE_URL}${imageAsset}` URL을 model로 사용 → Coil이 네트워크로 받음.
 *
 * 에셋 sync 전(콜드 스타트) 또는 sync 실패 상황에서도 화면이 깨지지 않도록 설계됨.
 *
 * @param imageAsset 매니페스트 기준 상대 경로 (예: "items/n_01.png")
 */
@Composable
fun AssetImage(
    imageAsset: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter? = null,
    error: Painter? = null,
) {
    val assetStore = rememberAssetStore()
    val model = remember(assetStore, imageAsset) { resolveAssetModel(assetStore, imageAsset) }
    // placeholder/error를 명시적으로 안 넘기면 Coil 기본 placeholder(작은 회색 동그라미)가
    // 로딩/실패 사이에 깜빡 보인다. 투명 ColorPainter로 깔아 시각적 잔상을 제거한다.
    // 호출자가 명시적으로 Painter를 넘기면 그 값이 그대로 적용된다.
    val effectivePlaceholder = placeholder ?: ColorPainter(Color.Transparent)
    val effectiveError = error ?: ColorPainter(Color.Transparent)
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = effectivePlaceholder,
        error = effectiveError,
    )
}
