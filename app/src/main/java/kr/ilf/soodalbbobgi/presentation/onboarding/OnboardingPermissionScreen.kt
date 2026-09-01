package kr.ilf.soodalbbobgi.presentation.onboarding

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

/**
 * 온보딩 2단계 — Health Connect 권한 요청 화면.
 *
 * "Health Connect 연결하기" 버튼을 누르면 Health Connect SDK가 제공하는
 * 권한 요청 다이얼로그를 띄운다. 권한 부여 완료 또는 "나중에 하기"로
 * 다음 화면(Home)으로 이동한다.
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 최초 동기화로 가져올 과거 기록 기간(개월) — 0은 안 가져오기, 최대 3개월.
    // 가져오기로 했을 때만 과거 데이터 권한이 요청 목록에 포함된다.
    var selectedMonths by remember { mutableStateOf(0) }

    // Health Connect 권한 요청 런처 — 권한 셋은 설정 화면과 공용
    // 지난 기록을 가져오기로 했을 때만 과거 데이터 권한을 함께 요청한다
    val healthPermissions = HealthConnectManager.requestPermissionsFor(includeHistory = selectedMonths > 0)

    val scope = rememberCoroutineScope()

    // 결과 셋(grantedPermissions)으로 판단하지 않는다 — HC는 이미 전부 허용된 상태에서
    // 재요청하면 빈 결과를 돌려주므로, 허용해 놓고도 화면이 멈추는 버그가 있었다.
    // 런처가 돌아오면 실제 권한 상태를 다시 조회해서 판단한다.
    // 권한만 확인하고 바로 넘어간다 — 동기화는 백그라운드로 시작하고 홈이 진행을 표시한다.
    suspend fun proceedIfGranted() {
        val granted = viewModel.hasAllPermissions()
        permissionGranted = granted
        if (!granted) {
            errorMessage = "권한이 허용되지 않았어요. 다시 시도하거나 나중에 설정에서 허용할 수 있어요."
            return
        }
        viewModel.startInitialSync(selectedMonths)
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

    // 이미 전부 허용된 채 이 화면에 온 경우(재설치 후 복원 등) 요청 없이 바로 넘어간다.
    LaunchedEffect(Unit) {
        if (isHealthConnectAvailable && viewModel.hasAllPermissions()) {
            Timber.d("Health Connect 권한 이미 허용됨 — 온보딩 권한 화면 자동 통과")
            proceedIfGranted()
        }
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
                    SoodalIcon(icon = SoodalIcons.Heart, tint = colors.warn, size = 26.dp)
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
                            text = if (!isHealthConnectAvailable) {
                                "Health Connect가 설치되어 있지 않습니다. Google Play에서 설치해주세요."
                            } else {
                                "수영 운동 데이터(거리·시간·칼로리)를 읽어오는 권한이 필요합니다."
                            },
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp,
                        )
                    }
                }
            }

            // 지난 기록 가져오기 — 가져올지부터 고르고, 가져오면 기간(최대 3개월)을 고른다.
            SoodalCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("지난 기록 가져오기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        SoodalChip("선택", color = ChipColor.Blue)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonthChip(
                            text = "안 가져오기",
                            selected = selectedMonths == 0,
                            onClick = { selectedMonths = 0 },
                        )
                        listOf(1, 2, 3).forEach { m ->
                            MonthChip(
                                text = "${m}개월",
                                selected = selectedMonths == m,
                                onClick = { selectedMonths = m },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "지난 기록까지 가져오려면 Health Connect의 '모든 기간 데이터' 권한이 추가로 필요해요.\n" +
                            "선택한 기간의 기록을 가져와 캘린더에 정리해 드려요.\n" +
                            "조개는 오늘 수영 기록에만 지급돼요 (새벽 2시 전엔 어제 기록까지).\n" +
                            "기간이 길수록 가져오는 데 시간이 걸릴 수 있어요.",
                        fontSize = 11.sp, color = colors.textTertiary, lineHeight = 17.sp,
                    )
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
                        Text("수동 입력 인증 시 사용됩니다. 추후 업데이트 예정.",
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
                else -> "Health Connect 연결하기"
            },
            onClick = {
                errorMessage = null
                if (isHealthConnectAvailable) {
                    try {
                        permissionLauncher.launch(healthPermissions)
                    } catch (e: Exception) {
                        Timber.e(e, "Health Connect 권한 요청 실패")
                        errorMessage = "권한 요청을 실행할 수 없어요: ${e.message}"
                    }
                } else {
                    errorMessage = "Health Connect 앱이 설치되어 있지 않습니다."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        SoodalButton("나중에 하기", onClick = onSkip, style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}

/** 기간 선택 칩 — 닉네임 화면 SelectChip과 같은 시각 언어. */
@Composable
private fun MonthChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.accentBlue.copy(alpha = 0.15f) else colors.surface1)
            .border(1.dp, if (selected) colors.accentBlue else colors.glassBorder, shape)
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.accentBlue else colors.textSecondary)
    }
}

/** 카메라 권한 카드 노출 여부 — 사진 인증 기능 도입 전까지 숨긴다. */
private const val SHOW_CAMERA_CARD = false
