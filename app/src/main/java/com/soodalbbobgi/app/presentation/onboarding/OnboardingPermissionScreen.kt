package com.soodalbbobgi.app.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.data.health.HealthConnectManager
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

    // Health Connect 권한 요청 런처
    val healthPermissions = remember {
        setOf(
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.ExerciseSessionRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.ExerciseSessionRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.DistanceRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.DistanceRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.HeartRateRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.HeartRateRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.SpeedRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.SpeedRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class
            ),
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        permissionRequested = true
        permissionGranted = grantedPermissions.isNotEmpty()
        Timber.d("Health Connect 권한 결과: granted=$permissionGranted (${grantedPermissions.size}/${healthPermissions.size})")
        if (permissionGranted) {
            // 권한 허용 직후 에셋·HC 동기화를 시작한다 (앱 스코프라 화면 전환과 무관하게 완료됨).
            viewModel.onPermissionGranted()
            onConnect()
        } else {
            errorMessage = "권한이 허용되지 않았어요. 다시 시도하거나 나중에 설정에서 허용할 수 있어요."
        }
    }

    Column(Modifier.fillMaxSize().background(colors.bgDeep).statusBarsPadding().padding(24.dp)) {
        Text("STEP 2 / 2", fontSize = 11.sp, fontWeight = FontWeight.Bold,
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

            // 카메라 — 선택 (비활성)
            SoodalCard(Modifier.fillMaxWidth().then(Modifier.alpha(0.45f))) {
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
