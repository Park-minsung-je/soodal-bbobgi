package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "swim_logs",
    indices = [
        // 하루에 여러 세션이 있을 수 있다 — HC 세션의 정체성은 hcRecordId (서버산 행은 null 허용)
        Index(value = ["date"]),
        Index(value = ["hcRecordId"], unique = true),
    ]
)
data class SwimLogEntity(
    @PrimaryKey(autoGenerate = true)
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
    val synced: Boolean = false,
    val hcRecordId: String? = null,
    /** 세션 중 최대/최소 심박(bpm). HC에 심박 기록이 없으면 null. */
    val maxHr: Int? = null,
    val minHr: Int? = null,
    /** 평균 심박(bpm) — 수동 입력용. HC 기록은 시계열에서 계산하므로 null. */
    val avgHr: Int? = null,
    /** 실제 운동 시간(초) — HC 세그먼트/랩에서 계산. 없으면 null (경과 시간으로 폴백). */
    val activeSeconds: Int? = null,
    /** 차트용 다운샘플 심박 시계열 ("오프셋초:bpm,..." 직렬화). 없으면 null. */
    val hrSeries: String? = null,
    val createdAt: Long,
)
