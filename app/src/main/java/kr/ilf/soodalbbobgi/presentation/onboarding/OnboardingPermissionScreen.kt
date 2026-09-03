package kr.ilf.soodalbbobgi.presentation.onboarding

import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import kr.ilf.soodalbbobgi.core.ui.pressable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.ButtonStyle
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.ChipColor
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.SoodalChip
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import timber.log.Timber

/** 지난 기록 가져오기 기간 선택지 — (개월 수, 칩 라벨). 12개월은 '1년'으로 읽힌다. */
internal val HISTORY_MONTH_OPTIONS: List<Pair<Int, String>> =
    listOf(1 to "1개월", 3 to "3개월", 6 to "6개월", 12 to "1년")

/**
 * 온보딩 2단계 — Health Connect 권한 요청 화면.
 *
 * "Health Connect 연결하기" 버튼을 누르면 Health Connect SDK가 제공하는
 * 권한 요청 다이얼로그를 띄운다. 권한 부여 완료 또는 "나중에 하기"로
 * 다음 화면(알림 설정)으로 이동한다.
 *
 * 권한이 이미 전부 허용된 채 들어와도(재가입·다른 계정 전환 등) 자동으로 넘어가지 않는다 —
 * 지난 기록 가져오기 토글·기간을 고를 기회가 있어야 하므로 버튼을 "연결됨 · 계속하기"로 바꿔 보여 준다.
 *
 * @param onConnect 권한 부여 완료(또는 요청 후) 콜백
 * @param onSkip "나중에 하기" 콜백
 */
@Composable
fun OnboardingPermissionScreen(
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val colors = SoodalDesign.colors

    // Health Connect SDK 가용 여부
    val isHealthConnectAvailable = remember {
        HealthConnectManager.isAvailable(context)
    }

    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    // 진입 시점에 필수 권한이 이미 전부 허용돼 있는지 — 버튼 라벨과 탭 동작(런처 생략)을 가른다.
    var alreadyGranted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 지난 기록 가져오기 토글 + 기간(개월, 최대 12) — 켰을 때만 과거 데이터 권한을 요청한다.
    var importHistory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var selectedMonths by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(1) }

    // Health Connect 권한 요청 런처 — 권한 셋은 설정 화면과 공용
    // 지난 기록을 가져오기로 했을 때만 과거 데이터 권한을 함께 요청한다
    val healthPermissions = HealthConnectManager.requestPermissionsFor(includeHistory = importHistory)

    val scope = rememberCoroutineScope()

    // 결과 셋(grantedPermissions)으로 판단하지 않는다 — HC는 이미 전부 허용된 상태에서
    // 재요청하면 빈 결과를 돌려주므로, 허용해 놓고도 화면이 멈추는 버그가 있었다.
    // 런처가 돌아오면 실제 권한 상태를 다시 조회해서 판단한다.
    // 권한만 확인하고 바로 넘어간다 — 동기화는 백그라운드로 시작하고 홈이 진행을 표시한다.
    suspend fun proceedIfGranted() {
        val granted = viewModel.hasAllPermissions()
        permissionGranted = granted
        // 진입 후 권한이 회수된 경우 "연결됨" 표시를 거두고 다음 탭은 런처로 보낸다.
        alreadyGranted = granted
        if (!granted) {
            errorMessage = OnboardingCopy.PERMISSION_DENIED
            return
        }
        if (importHistory && !viewModel.hasHistoryPermission()) {
            // 과거 데이터 권한만 거부한 경우 — 필수 권한이 아니라 진행은 막지 않되, HC가 첫 허용 30일 이전
            // 기록을 주지 않아 6개월·1년을 골라도 조용히 짧아지므로 미리 알린다.
            Toast.makeText(context, OnboardingCopy.HISTORY_PERMISSION_MISSING, Toast.LENGTH_LONG).show()
        }
        viewModel.startInitialSync(if (importHistory) selectedMonths else 0)
        onConnect()
    }

    val onPermissionFlowReturned: () -> Unit = {
        permissionRequested = true
        scope.launch { proceedIfGranted() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        Timber.d("Health Connect 권한 결과 셋: ${grantedPermissions.size}/${healthPermissions.size} (판단은 재조회로)")
        onPermissionFlowReturned()
    }

    // 이미 전부 허용된 채 들어와도 자동으로 넘어가지 않는다 — 지난 기록 토글·기간을 고를 기회가 사라진다(R18·R19).
    // 상태만 보관해 버튼을 "연결됨 · 계속하기"로 바꾼다.
    LaunchedEffect(Unit) {
        alreadyGranted = isHealthConnectAvailable && viewModel.hasAllPermissions()
        if (alreadyGranted) Timber.d("Health Connect 권한 이미 허용됨 — 기간 선택을 위해 화면 유지")
    }

    // 자체 배경 필수 — 투명이면 슬라이드 전환 중 이전 화면과 겹쳐 보인다 (설정 화면과 동일 패턴).
    Column(Modifier.fillMaxSize().soodalScreenBackdrop().statusBarsPadding().padding(24.dp)) {
        Text("STEP 2 / 3", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = colors.accentBlue, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Text("수영 기록을\n자동으로 가져올게요", style = SoodalDesign.typography.xl, color = colors.textPrimary)
        Text("Health Connect와 연동하면 수영 후 자동으로 기록이 등록되고 조개를 받을 수 있어요.",
            fontSize = 14.sp, color = colors.textSecondary, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(36.dp))
        SoodalIcon(SoodalIcons.Swimmer, tint = colors.accentBlue, size = 64.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(30.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Health Connect — 필수
            SoodalCard(Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoodalIcon(icon = SoodalIcons.HealthConnect, tint = colors.accentBlue, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Health Connect", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            SoodalChip("필수", color = ChipColor.Blue)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (!isHealthConnectAvailable) OnboardingCopy.HC_NOT_INSTALLED else OnboardingCopy.HC_REQUIRED,
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp,
                        )
                    }
                }
            }

            // 지난 기록 가져오기 — 위 필수 카드와 같은 레이아웃. 토글을 켜면 기간 선택이 나온다.
            SoodalCard(Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoodalIcon(icon = SoodalIcons.Clock, tint = colors.accentBlue, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("지난 기록 가져오기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                SoodalChip("선택", color = ChipColor.Blue)
                            }
                            HistoryToggle(checked = importHistory, onCheckedChange = { importHistory = it })
                        }
                        Spacer(Modifier.height(4.dp))
                        // 안내문은 토글 상태와 무관하게 같다 — 분기 안에 두 번 쓰면 한쪽만 고쳐져 OFF/ON이 어긋난다.
                        Text(OnboardingCopy.HISTORY_GUIDE, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                        if (importHistory) {
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HISTORY_MONTH_OPTIONS.forEach { (months, label) ->
                                    MonthChip(
                                        text = label,
                                        selected = selectedMonths == months,
                                        // 4개가 카드 폭을 나눠 갖는다 — 고정 패딩이면 마지막 칩이 카드 밖으로 밀린다
                                        // (EditorSheet 크기 칩과 같은 처리)
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedMonths = months },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(OnboardingCopy.HISTORY_POLICY, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // 카메라 — 선택 (비활성). 사진 인증 기능이 생기면 SHOW_CAMERA_CARD로 되살린다.
            if (SHOW_CAMERA_CARD) SoodalCard(Modifier.fillMaxWidth().then(Modifier.alpha(0.45f))) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoodalIcon(icon = SoodalIcons.Camera, tint = colors.textTertiary, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Text("카메라 (선택)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("수동 입력 인증에 써요. 추후 업데이트 예정이에요.",
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                    }
                }
            }
        }

        // 에러 메시지
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                fontSize = 13.sp,
                color = colors.warn,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.weight(1f))
        SoodalButton(
            text = when {
                !isHealthConnectAvailable -> "Health Connect 설치 필요"
                permissionRequested && !permissionGranted -> "다시 시도하기"
                alreadyGranted -> "연결됨 · 계속하기"
                else -> "Health Connect 연결하기"
            },
            onClick = {
                errorMessage = null
                if (isHealthConnectAvailable) {
                    scope.launch {
                        // 이미 연결돼 있고 추가로 요청할 권한(지난 기록)도 없으면 런처를 생략한다 —
                        // HC 요청 화면이 순간 떴다 사라지며 상단바가 깜빡이는 것을 피한다.
                        val needsHistory = importHistory && !viewModel.hasHistoryPermission()
                        if (alreadyGranted && !needsHistory) {
                            onPermissionFlowReturned()
                            return@launch
                        }
                        try {
                            permissionLauncher.launch(healthPermissions)
                        } catch (e: Exception) {
                            // 예외 원문은 로그에만 — 사용자에게는 할 수 있는 조치(HC 업데이트)만 말한다.
                            Timber.e(e, "Health Connect 권한 요청 실패")
                            errorMessage = OnboardingCopy.PERMISSION_LAUNCH_FAILED
                        }
                    }
                } else {
                    errorMessage = OnboardingCopy.HC_APP_MISSING
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        SoodalButton("나중에 하기", onClick = onSkip, style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}

/** 토글 스위치 — 알림 온보딩과 동일한 시각 언어 (44x24, 좌우 대칭 썸). */
@Composable
private fun HistoryToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = SoodalDesign.colors
    // 켜짐 트랙은 편집 시트 저장 버튼과 같은 진한 하늘색 그라데이션 — 단색 accentBlue보다 또렷하다.
    val trackBackground = if (checked) Modifier.background(colors.gradBlueVivid) else Modifier.background(colors.surface3)
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "historyThumb",
    )
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .then(trackBackground)
            .pressable(onClick = { onCheckedChange(!checked) }),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .shadow(2.dp, androidx.compose.foundation.shape.CircleShape)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * 기간 선택 칩 — 닉네임 화면 SelectChip과 같은 시각 언어. 폭은 부모가 [modifier]의 weight로 나눠 준다.
 *
 * @param text 칩 라벨
 * @param selected 선택 상태 (강조색 배경·테두리)
 * @param modifier 부모가 주는 폭 분배용 Modifier
 * @param onClick 탭 콜백
 */
@Composable
private fun MonthChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accentBlue.copy(alpha = 0.15f) else colors.surface1)
            .border(1.dp, if (selected) colors.accentBlue else colors.glassBorder, shape)
            .pressable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            color = if (selected) colors.accentBlue else colors.textSecondary,
        )
    }
}

/** 카메라 권한 카드 노출 여부 — 사진 인증 기능 도입 전까지 숨긴다. */
private const val SHOW_CAMERA_CARD = false
