package kr.ilf.soodalbbobgi.domain.model

data class SwimLog(
    val id: Long = 0,
    val userId: String,
    val date: String,
    /** 세션 시작 시각(epoch 초) — 같은 날 세션 정렬·시간대 표시용. 서버산 행은 null. */
    val startEpochSec: Long? = null,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val strokeFreestyleM: Int = 0,
    val strokeBreastM: Int = 0,
    val strokeBackM: Int = 0,
    val strokeFlyM: Int = 0,
    val strokeMixedM: Int = 0,
    val strokeKickM: Int = 0,
    val source: String,
    val shellsEarned: Int = 0,
    /** 서버에 일 집계가 보고된 행인지 — false면 다음 동기화 때 재전송된다. */
    val synced: Boolean = false,
    val hcRecordId: String? = null,
    /**
     * 서버에서 복원한 일 집계 행이 품은 나머지 HC 세션 레코드 ID(첫 ID는 [hcRecordId]).
     * 하루 여러 세션이 한 행에 합쳐질 때만 비어 있지 않다 — HC 삭제 이벤트 매칭용.
     */
    val extraHcRecordIds: List<String> = emptyList(),
    /** 세션 중 최대/최소 심박(bpm). 심박 기록이 없으면 null. */
    val maxHr: Int? = null,
    val minHr: Int? = null,
    /** 평균 심박(bpm) — 수동 입력용. HC 기록은 시계열에서 계산하므로 null. */
    val avgHr: Int? = null,
    /** 실제 운동 시간(초) — HC 세그먼트/랩 기반. 없으면 null (경과 시간으로 폴백). */
    val activeSeconds: Int? = null,
    /** 차트용 다운샘플 심박 시계열 ("오프셋초:bpm,..." 직렬화). 없으면 null. */
    val hrSeries: String? = null,
) {
    /** 이 행이 대표하는 HC 세션 레코드 ID 전부 — 블랙리스트·서버 보고용. */
    val allHcRecordIds: List<String> get() = listOfNotNull(hcRecordId) + extraHcRecordIds

    /**
     * 서버 백업에서 복원한 행인지 — HC 세션은 항상 시작 시각을 갖고, 서버 일 집계에는 없다.
     * (복원 행이 hcRecordId를 갖게 되면서 "hcRecordId 없음"만으로는 가려낼 수 없다)
     */
    val isServerRestored: Boolean get() = startEpochSec == null
}
