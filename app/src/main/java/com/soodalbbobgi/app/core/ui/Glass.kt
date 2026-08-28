package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalColors
import com.soodalbbobgi.app.core.theme.SoodalDesign

// ─── 공용 글래스 기준값 (한 곳에서 관리 — 바꾸면 전 컴포넌트에 반영) ───
// App Canvas 곡률: 콘텐츠 글래스 카드(도감·오늘·주간·월간·달력)=24, 프로필 프레임=32(내부 24),
// 칩/아이콘 버튼=14, 탭바=22(SoodalTabBar 자체 shape).
/** 카드·달력·도감 등 콘텐츠 글래스 컨테이너의 표준 모서리 (App Canvas = 24). */
val GlassCorner: Dp = 24.dp
/** 프로필 카드 프레임의 모서리 (App Canvas 개정 = 24, 내부 아트 18 = 프레임 − 패딩 6). */
val ProfileFrameCorner: Dp = 24.dp
/** 칩·아이콘 버튼 등 작은 글래스 요소의 모서리 (App Canvas = 14). */
val GlassCornerSmall: Dp = 14.dp

/**
 * 팝업/시트 뒤 스크림(딤)의 공통 알파 — 앱 전체 통일.
 * 높으면 유리 시트 경계에서 안(밝은 블러)과 밖(어두운 원본)의 연속성이 끊겨 유리감이 죽는다.
 */
const val SoodalDimAlpha = 0.2f

/**
 * 앱 전역 통일 배경 — 물빛 파스텔 165° 그라데이션 위에 teal/purple/gold radial glow를
 * 겹친 **디자인 원본(.app-root + ::before) 값 그대로**. 루트에서 한 번만 그리고,
 * 각 화면은 배경을 칠하지 않아(투명) 고정 헤더 경계에 이음매가 생기지 않는다.
 */
fun Modifier.soodalBackground(colors: SoodalColors): Modifier = this.drawBehind {
    // 1) 베이스 그라데이션 (cyan→mint→lavender 3스톱, 165° ≈ 살짝 기운 세로)
    drawRect(
        Brush.linearGradient(
            colors = colors.screenGradientStops,
            start = Offset(size.width * 0.16f, 0f),
            end = Offset(size.width * 0.84f, size.height),
        ),
    )
    // 2) radial glow — 디자인 원본 알파 그대로 (teal .10 / purple .08 / gold .07)
    val big = maxOf(size.width, size.height)
    fun glow(c: Color, cx: Float, cy: Float, r: Float) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(c, Color.Transparent),
                center = Offset(size.width * cx, size.height * cy),
                radius = big * r,
            ),
        )
    }
    if (!colors.isDark) {
        glow(Color(0x1A00A8B8), 0.20f, 0.00f, 0.60f) // teal .10
        glow(Color(0x14883DDB), 0.90f, 0.30f, 0.60f) // purple .08
        glow(Color(0x12D99500), 0.50f, 1.00f, 0.60f) // gold .07
    } else {
        glow(Color(0x1200F5FF), 0.20f, 0.00f, 0.60f)
        glow(Color(0x0FBF5AF2), 0.90f, 0.30f, 0.60f)
        glow(Color(0x0DFFD60A), 0.50f, 1.00f, 0.60f)
    }
}

/**
 * 화면 자체 배경 — 푸시로 열리는 서브 화면(설정·라이선스 등)이 전환 중 뒤 화면을 가리도록
 * **자기 배경을 직접 칠한다**. 루트 통일 배경과 동일한 이미지/그라데이션이라 이음매 없음.
 * (탭 화면은 투명 유지 — 루트 배경 공유.)
 */
@Composable
fun Modifier.soodalScreenBackdrop(): Modifier {
    val colors = SoodalDesign.colors
    return if (!colors.isDark) {
        this.paint(painterResource(id = R.drawable.soodal_bg), contentScale = ContentScale.FillBounds)
    } else {
        this.soodalBackground(colors)
    }
}

/** 화면 배경 그라데이션 색 스톱 (라이트/다크 공통 진입점). */
private val SoodalColors.screenGradientStops: List<Color>
    get() = if (!isDark) {
        listOf(Color(0xFFCFE9FA), Color(0xFFD6F0E6), Color(0xFFE4DCF8))
    } else {
        listOf(Color(0xFF0E1A30), Color(0xFF0A1220), Color(0xFF06080F))
    }

/**
 * 글래스 서피스 그림자 — 네이티브 `shadowLayer`로 그리는 **부드럽고 균일한** 블루틴트 그림자.
 * (Compose `Modifier.shadow`는 반투명 카드에서 흰 사각형 아티팩트가 있었어 되돌림.
 *  오프셋을 작게 두어 예전 "사각으로 잘린 듯한" 하드 그림자도 피한다.)
 */
fun Modifier.glassShadow(cornerDp: Dp, colors: SoodalColors): Modifier = this.drawBehind {
    val argb = if (!colors.isDark) {
        android.graphics.Color.argb(38, 45, 90, 140)
    } else {
        android.graphics.Color.argb(140, 0, 0, 0)
    }
    val paint = Paint().asFrameworkPaint().apply {
        color = android.graphics.Color.TRANSPARENT
        // 큰 블러 + 작은 y오프셋 = 카드 아래로 은은하게 퍼지는 글로우 (하드 엣지 없음)
        setShadowLayer(26.dp.toPx(), 0f, 7.dp.toPx(), argb)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerDp.toPx(), cornerDp.toPx(), paint,
        )
    }
}

/**
 * 공용 haze 상태 — zIndex 레이어링: 배경=소스(z0), 카드=소스(z1)+z0 블러,
 * 탭바/팝업(이펙트 전용)=모든 소스 블러. AppNavHost가 제공.
 */
val LocalHazeContent = staticCompositionLocalOf<HazeState?> { null }

/** 팝업/시트 공통 프로스트 흰 틴트 — 유리감(뒤 비침)을 남기는 표준값. */
const val GlassFrostTint = 0.66f

/** 뽑기 결과처럼 화려한 씬 위에 뜨는 프로스트 — 가독 우선의 진한 틴트. */
const val GlassFrostTintHeavy = 0.90f

/**
 * 오버레이 글래스 프로스트 채움 — 뒤 콘텐츠를 샘플링·블러해 진짜 젖빛 유리를 만든다.
 * haze가 없으면(프리뷰 등) 반투명 채움 폴백.
 *
 * @param tintAlpha 흰 틴트 강도 — 표준 [GlassFrostTint], 뽑기 결과는 [GlassFrostTintHeavy]
 */
fun Modifier.glassFrost(
    colors: SoodalColors,
    shape: Shape,
    haze: HazeState?,
    tintAlpha: Float = GlassFrostTint,
): Modifier = this
    .clip(shape)
    .then(
        if (haze != null) {
            Modifier.hazeEffect(state = haze) {
                backgroundColor = if (colors.isDark) Color(0xFF0E1426) else Color.White
                tints = listOf(HazeTint(if (colors.isDark) colors.glassBg else Color.White.copy(alpha = tintAlpha)))
                blurRadius = 20.dp
                noiseFactor = 0f
                inputScale = HazeInputScale.Auto
            }
        } else {
            Modifier.background(colors.glassBg)
        },
    )

/**
 * 카드 글래스 프로스트 — Haze 문서의 zIndex 레이어링 패턴.
 * 카드는 **자기 자신을 z=1 소스로 등록**하면서, 자기보다 낮은 z(배경, z=0)만 샘플링·블러한다.
 * 콘텐츠를 감싸는 통소스가 없으므로 어떤 이펙트도 다른 소스의 녹화 안에 갇히지 않고,
 * 탭바/팝업(이펙트 전용)은 배경(z0)+카드(z1) 소스를 모두 샘플링한다.
 */
@Composable
fun Modifier.cardFrost(colors: SoodalColors, shape: Shape): Modifier {
    val haze = LocalHazeContent.current
    return if (!colors.isDark && haze != null) {
        this
            // 자기 콘텐츠를 z=1 소스로 등록 — 탭바/팝업 블러에 카드가 비치도록.
            .hazeSource(haze, zIndex = 1f)
            .clip(shape)
            // 소스(z=1)가 붙은 노드의 이펙트는 z<1 소스(배경)만 샘플링 → 재귀 없음.
            .hazeEffect(state = haze) {
                backgroundColor = Color.White
                // 카드 흰끼 조정 지점 — 낮을수록 유리 속 배경색이 진하게 비친다.
                tints = listOf(HazeTint(Color.White.copy(alpha = 0.55f)))
                blurRadius = 45.dp
                noiseFactor = 0f
                // 백드롭을 축소해 샘플링 — 45dp 블러가 축소 아티팩트를 덮어 화질 차이는
                // 없고 GPU 비용만 준다. 탭 전환처럼 두 화면이 동시에 그려질 때의 잭 대책.
                inputScale = HazeInputScale.Auto
            }
    } else {
        this.clip(shape).background(colors.glassBg)
    }
}

/**
 * 공용 글래스 표면 모디파이어 — 그림자 + 반투명 채움 + 1px 흰 하이라이트 보더.
 * **상단 sheen은 [GlassSheen]으로 별도로 얹는다** (모디파이어는 자식 컴포저블을 못 그리므로).
 * 자식이 없는 표면(탭바 등)은 이 모디파이어 + [GlassSheen] 오버레이를 함께 쓴다.
 */
fun Modifier.glass(
    colors: SoodalColors,
    cornerDp: Dp = GlassCorner,
    shape: Shape = RoundedCornerShape(cornerDp),
    shadow: Boolean = true,
): Modifier {
    var m = this
    if (shadow) m = m.glassShadow(cornerDp, colors)
    return m
        .clip(shape)
        .background(colors.glassBg)
        .border(1.dp, colors.glassBorder, shape)
}

/**
 * 글래스 상단 sheen 오버레이 — 유리 윗면에 빛이 번지는 3단 하이라이트.
 * 어떤 글래스 [Box] 안에서든 호출하면 동일한 sheen이 얹힌다(모서리 [shape]로 클립).
 * 이 하나로 **모든 글래스 컴포넌트의 상단 효과를 통일**한다.
 */
@Composable
fun BoxScope.GlassSheen(shape: Shape) {
    val colors = SoodalDesign.colors
    Box(Modifier.matchParentSize().clip(shape)) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // 유리 윗면 하이라이트 — 영역(높이)은 원래대로 얇게, 진하기만 올린 값.
                // 카드·시트·팝업·탭바가 모두 이 값을 공유한다.
                .height(if (colors.isDark) 5.dp else 7.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (colors.isDark) 0.24f else 0.62f),
                            Color.White.copy(alpha = if (colors.isDark) 0.08f else 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/**
 * 상단바 정보 패널 — 조개·진주·연속일처럼 **읽기만 하는 값**을 하나의 유리 바에 묶는다.
 * 홈/인양소/상점 헤더 공용이며, 그림자·보더·sheen이 패널 단위로 한 번만 적용돼
 * 작은 유리 조각이 여럿 흩어질 때보다 상단이 덜 산만하다.
 *
 * 반대로 **액션 버튼은 묶지 않는다** — 서로 다른 동작이라 각자 눌리는 낱개 유리로 둔다.
 * 하나의 바에 담으면 단일 컨트롤처럼 보인다.
 *
 * 안쪽 여백 5dp는 세그먼트 자체 여백 8dp와 합쳐 낱개 칩 시절의 가장자리 13dp를 그대로 맞춘다
 * — 좁힌 건 세그먼트 사이(32dp → 16dp)뿐이다.
 */
@Composable
fun GlassInfoGroup(content: @Composable RowScope.() -> Unit) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(GlassCornerSmall)
    Box(
        modifier = Modifier.height(38.dp).glass(colors, GlassCornerSmall, shape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        GlassSheen(shape)
    }
}

/**
 * [GlassInfoGroup] 안의 값 세그먼트 — 아이콘 + 값. 세그먼트 경계는 여백으로만 구분한다.
 *
 * @param icon 값 아이콘 (조개/진주/연속일)
 * @param value 표시 값
 * @param tint 아이콘·값 색
 */
@Composable
fun GlassInfoSegment(icon: SoodalIcons, value: String, tint: Color) {
    Row(
        // 세그먼트 자체 여백 4 + 패널 여백 9 = 가장자리 13dp(낱개 칩 시절 값), 값 사이 8dp.
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        // 아이콘과 숫자는 한 덩어리로 읽혀야 해서 세그먼트 사이보다 훨씬 좁게 붙인다.
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SoodalIcon(icon = icon, tint = tint, size = 16.dp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}

/**
 * 재사용 글래스 박스 — 디자인 표준 글래스(.glass/.card) 공용 컨테이너.
 * 반투명 프로스트 + 흰 하이라이트 보더 + 소프트 블루틴트 그림자 + **상단 sheen**.
 * 앱의 모든 카드/패널이 이걸 쓰므로, 기준값(코너·sheen·그림자)을 여기서 바꾸면 전부 반영된다.
 *
 * @param cornerDp 모서리 반경 (기본 [GlassCorner]).
 * @param contentPadding 내부 패딩 (카드 16, 밀집 14, 프레임 6).
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerDp: Dp = GlassCorner,
    contentPadding: Dp = 16.dp,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(cornerDp)
    var m = modifier
    if (shadow) m = m.glassShadow(cornerDp, colors)
    Box(
        modifier = m
            // 프로스트(블러 배경) 채움 — 반투명 채움 대신 진짜 젖빛 유리 질감.
            .cardFrost(colors, shape)
            .border(1.dp, colors.glassBorder, shape)
            // 탭은 **프로스트가 클립된 뒤** 붙인다 — 누름 스크림이 카드 모서리를 따라 잘린다.
            // 호출부가 modifier로 clickable을 넘기면 클립 밖이라 사각형으로 덮인다.
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier),
    ) {
        Box(Modifier.padding(contentPadding), content = content)
        GlassSheen(shape)
    }
}
