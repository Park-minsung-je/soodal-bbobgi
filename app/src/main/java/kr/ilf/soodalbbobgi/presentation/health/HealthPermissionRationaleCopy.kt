package kr.ilf.soodalbbobgi.presentation.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager

/**
 * 권한 사용 근거 화면의 한 항목 — Health Connect 권한 하나와 그 용도.
 *
 * @param permission 앱이 요청하는 HC 권한 문자열 (테스트가 실제 요청 셋과 대조한다)
 * @param title 데이터 이름
 * @param description 무엇을 읽고 어디에 쓰는지
 */
data class HealthDataItem(val permission: String, val title: String, val description: String)

/**
 * 권한 사용 근거 화면 문구 — 시스템 설정·Health Connect 권한 화면의 "개인정보처리방침" 진입점에서
 * 앱이 어떤 건강 데이터를 왜 읽는지 설명한다. Android 의존이 없어 유닛 테스트로 권한 셋과 대조한다.
 */
object HealthPermissionRationaleCopy {
    const val TITLE = "건강 데이터 사용 안내"

    const val INTRO = "수달 뽑기는 Health Connect에서 수영 기록만 읽어 옵니다. " +
        "기록을 새로 쓰거나 고치지 않으며, 수영이 아닌 운동은 가져오지 않습니다."

    /** 요청하는 권한별 설명 — 순서대로 화면에 나열한다. */
    val dataItems: List<HealthDataItem> = listOf(
        HealthDataItem(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            "운동 세션",
            "수영 세션의 시작·종료 시각과 영법별 구간을 읽어 기록 하나로 만듭니다.",
        ),
        HealthDataItem(
            HealthPermission.getReadPermission(DistanceRecord::class),
            "거리",
            "세션 동안 수영한 거리입니다. 하루 기록과 통계에 씁니다.",
        ),
        HealthDataItem(
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            "소모 칼로리",
            "세션 동안 소모한 칼로리를 기록 카드에 표시합니다.",
        ),
        HealthDataItem(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            "심박수",
            "세션 중 최대·최소·평균 심박과 차트용 요약 곡선을 만듭니다.",
        ),
        HealthDataItem(
            HealthPermission.getReadPermission(SpeedRecord::class),
            "속도",
            "구간 페이스를 계산하는 데 씁니다.",
        ),
        HealthDataItem(
            HealthConnectManager.BG_READ_PERMISSION,
            "백그라운드 읽기",
            "새 수영 기록 알림을 켠 경우에만, 앱이 닫혀 있어도 새 기록이 들어왔는지 확인합니다.",
        ),
        HealthDataItem(
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
            "모든 기간의 데이터",
            "지난 기록 가져오기를 켠 경우에만, 최근 30일보다 오래된 기록을 최대 12개월까지 읽습니다.",
        ),
    )

    /** 읽은 데이터를 쓰는 곳. */
    val purposes: List<String> = listOf(
        "달력·통계·심박 차트로 수영 기록을 보여 줍니다.",
        "하루에 한 번, 수영한 날에 조개를 지급합니다.",
        "기기를 바꿔도 기록을 이어 볼 수 있게 복원합니다.",
    )

    /** 보관·공유 원칙. */
    val storage: List<String> = listOf(
        "기록은 이 기기와 수달 뽑기 서버에 저장되며, 계정을 탈퇴하면 함께 삭제됩니다.",
        "광고·마케팅 등 다른 목적에 쓰지 않고, 제3자에게 판매하거나 제공하지 않습니다.",
    )

    /** 권한 철회 안내. */
    val revoke: List<String> = listOf(
        "권한은 언제든 Health Connect 설정에서 철회할 수 있습니다.",
        "철회하면 새 기록을 가져오지 못하지만, 뽑기·상점 등 다른 기능은 계속 쓸 수 있습니다.",
    )
}
