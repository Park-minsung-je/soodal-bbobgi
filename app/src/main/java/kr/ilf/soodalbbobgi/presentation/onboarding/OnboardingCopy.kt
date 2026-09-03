package kr.ilf.soodalbbobgi.presentation.onboarding

/**
 * 온보딩 권한·알림 화면의 안내 문구.
 *
 * 권한이 필요한 항목은 마지막 줄에 필요한 권한을 적는다 — 두 화면이 같은 규칙을 지키도록
 * 한곳에 모으고 테스트로 고정한다. HC 과거 데이터 권한은 HC 권한 화면의 공식 라벨을 그대로 쓴다.
 */
internal object OnboardingCopy {
    /** HC 필수 카드 — 읽는 데이터 5종은 [kr.ilf.soodalbbobgi.data.health.HealthConnectManager.BASE_REQUEST_PERMISSIONS]와 맞춘다. */
    const val HC_REQUIRED =
        "수영 기록을 자동으로 동기화하려면 Health Connect의 운동·거리·심박수·속도·칼로리 읽기 권한이 필요해요."

    /** HC 미설치 카드. */
    const val HC_NOT_INSTALLED = "Health Connect가 설치되어 있지 않아요. Google Play에서 설치해 주세요."

    /** HC가 권한 화면에 표시하는 과거 데이터 권한의 공식 라벨. */
    const val HC_HISTORY_LABEL = "모든 기간의 데이터에 액세스"

    /** 지난 기록 카드 — 토글 ON/OFF 공통. */
    const val HISTORY_GUIDE =
        "오늘 이전의 수영 기록도 가져와 캘린더에 저장할 수 있어요.\n" +
            "Health Connect의 '$HC_HISTORY_LABEL' 권한이 필요해요."

    /** 지난 기록 카드 — 기간 칩 아래 보상·소요시간 안내. */
    const val HISTORY_POLICY =
        "조개는 오늘 수영한 기록에만 드려요 (새벽 2시 전엔 어제 기록까지). 지난 기록은 캘린더에만 채워져요.\n" +
            "기간이 길수록 가져오는 데 시간이 더 걸려요."

    /** 지난 기록을 켰는데 과거 데이터 권한이 거부된 경우 토스트. */
    const val HISTORY_PERMISSION_DENIED =
        "'$HC_HISTORY_LABEL' 권한이 거부돼 지난 기록 가져오기를 껐어요. 다시 켜면 권한을 다시 요청해요."

    const val PERMISSION_DENIED =
        "필수 권한이 아직 허용되지 않았어요.\n다시 시도하거나, 나중에 설정 > 연동에서 연결할 수 있어요."

    const val PERMISSION_LAUNCH_FAILED =
        "권한 요청 화면을 열 수 없어요. Health Connect 앱을 업데이트한 뒤 다시 시도해 주세요."

    const val HC_APP_MISSING = "Health Connect 앱이 설치되어 있지 않아요."

    // ── 알림 단계 ──
    const val NOTIFICATION_INTRO = "원하는 알림만 골라 켜 주세요. 나중에 설정에서도 바꿀 수 있어요."

    const val REMINDER =
        "정한 시간까지 오늘 수영 기록이 없으면 잊지 않게 알려드려요.\n" +
            "Android 알림 권한이 필요해요."

    const val NEW_RECORD =
        "조개를 받을 수 있는 새 수영 기록이 확인되면 알려드려요.\n" +
            "Android 알림 권한과 Health Connect 백그라운드 읽기 권한이 필요해요."

    const val NEW_RECORD_LOCKED = "Health Connect를 연결한 뒤 설정에서 켤 수 있어요."

    /** 말투 규칙 테스트용 전체 목록. */
    val ALL: List<String> = listOf(
        HC_REQUIRED, HC_NOT_INSTALLED, HISTORY_GUIDE, HISTORY_POLICY, HISTORY_PERMISSION_DENIED,
        PERMISSION_DENIED, PERMISSION_LAUNCH_FAILED, HC_APP_MISSING,
        NOTIFICATION_INTRO, REMINDER, NEW_RECORD, NEW_RECORD_LOCKED,
    )
}
