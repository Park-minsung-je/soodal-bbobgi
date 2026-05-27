package com.soodalbbobgi.app.core.init

import com.soodalbbobgi.app.data.local.db.GachaBoxDao
import com.soodalbbobgi.app.data.local.db.ProfileCardDao
import com.soodalbbobgi.app.data.local.db.SwimLogDao
import com.soodalbbobgi.app.data.local.db.UserDao
import com.soodalbbobgi.app.data.local.entity.GachaBoxEntity
import com.soodalbbobgi.app.data.local.entity.GachaBoxItemEntity
import com.soodalbbobgi.app.data.local.entity.ProfileCardEntity
import com.soodalbbobgi.app.data.local.entity.SwimLogEntity
import com.soodalbbobgi.app.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug 빌드 전용 초기 데이터 시딩.
 * mock 사용자, 뽑기 상자, 프로필 카드, 수영 기록을 Room DB에 삽입한다.
 */
@Singleton
class DebugInitializer @Inject constructor(
    private val userDao: UserDao,
    private val gachaBoxDao: GachaBoxDao,
    private val profileCardDao: ProfileCardDao,
    private val swimLogDao: SwimLogDao,
) {
    /**
     * 앱 시작 시 호출하여 debug mock 데이터를 삽입한다.
     * IO 디스패처에서 비동기로 실행되며, 기존 데이터가 있으면 REPLACE 전략으로 덮어쓴다.
     */
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seedUser()
                seedProfileCard()
                seedGachaBoxes()
                seedSwimLogs()
                Timber.d("DebugInitializer: mock 데이터 시딩 완료")
            } catch (e: Exception) {
                Timber.e(e, "DebugInitializer: mock 데이터 시딩 실패")
            }
        }
    }

    private suspend fun seedUser() {
        val now = System.currentTimeMillis()
        val user = UserEntity(
            id = "debug_user",
            nickname = "Jinnie",
            shellBalance = 34,
            pearlBalance = 12,
            pityCounter = 63,
            lastShellGrantDate = null,
            authProvider = "debug",
            createdAt = now,
            updatedAt = now,
            synced = false,
        )
        userDao.insert(user)
    }

    private suspend fun seedProfileCard() {
        val card = ProfileCardEntity(
            userId = "debug_user",
            lastEditedAt = System.currentTimeMillis(),
        )
        profileCardDao.upsert(card)
    }

    private suspend fun seedGachaBoxes() {
        val now = System.currentTimeMillis()

        data class BoxDef(val id: Long, val name: String, val category: String, val description: String)

        val boxes = listOf(
            BoxDef(1L, "캐릭터 상자", "CHARACTER", "귀여운 수달 캐릭터를 뽑아보세요!"),
            BoxDef(2L, "배경 상자", "BACKGROUND", "프로필 카드 배경을 꾸며보세요!"),
            BoxDef(3L, "테두리 상자", "BORDER", "프로필 카드 테두리를 바꿔보세요!"),
        )

        for (box in boxes) {
            gachaBoxDao.insertBox(
                GachaBoxEntity(
                    id = box.id,
                    name = box.name,
                    description = box.description,
                    category = box.category,
                    updatedAt = now,
                )
            )

            val items = buildBoxItems(box.id, box.category)
            gachaBoxDao.insertBoxItems(items)
        }
    }

    /**
     * 상자당 10개 아이템을 생성한다.
     * 5xN(weight 1000), 3xR(weight 1000), 1xSR(weight 1500), 1xSSR(weight 500)
     * 총 weight = 10000 → N 50%, R 30%, SR 15%, SSR 5%
     */
    private fun buildBoxItems(boxId: Long, category: String): List<GachaBoxItemEntity> {
        val prefix = when (category) {
            "CHARACTER" -> "char"
            "BACKGROUND" -> "bg"
            "BORDER" -> "border"
            else -> "item"
        }

        val items = mutableListOf<GachaBoxItemEntity>()
        val baseId = boxId * 100

        // 5 x N grade (weight 1000 each)
        for (i in 1..5) {
            items.add(
                GachaBoxItemEntity(
                    id = baseId + i,
                    boxId = boxId,
                    itemKey = "${prefix}_n_$i",
                    name = "${category.lowercase()} N-$i",
                    grade = "N",
                    weight = 1000,
                    imageAsset = "placeholder/${prefix}_n_$i.webp",
                )
            )
        }

        // 3 x R grade (weight 1000 each)
        for (i in 1..3) {
            items.add(
                GachaBoxItemEntity(
                    id = baseId + 10 + i,
                    boxId = boxId,
                    itemKey = "${prefix}_r_$i",
                    name = "${category.lowercase()} R-$i",
                    grade = "R",
                    weight = 1000,
                    imageAsset = "placeholder/${prefix}_r_$i.webp",
                )
            )
        }

        // 1 x SR grade (weight 1500)
        items.add(
            GachaBoxItemEntity(
                id = baseId + 20,
                boxId = boxId,
                itemKey = "${prefix}_sr_1",
                name = "${category.lowercase()} SR-1",
                grade = "SR",
                weight = 1500,
                imageAsset = "placeholder/${prefix}_sr_1.webp",
            )
        )

        // 1 x SSR grade (weight 500)
        items.add(
            GachaBoxItemEntity(
                id = baseId + 30,
                boxId = boxId,
                itemKey = "${prefix}_ssr_1",
                name = "${category.lowercase()} SSR-1",
                grade = "SSR",
                weight = 500,
                imageAsset = "placeholder/${prefix}_ssr_1.webp",
            )
        )

        return items
    }

    private suspend fun seedSwimLogs() {
        val now = System.currentTimeMillis()

        data class SwimData(
            val day: Int,
            val distance: Int,
            val durationMin: Int,
            val calories: Int,
            val free: Int,
            val breast: Int,
            val back: Int,
            val fly: Int,
            val shells: Int,
        )

        val logs = listOf(
            SwimData(2, 800, 25, 180, 60, 30, 10, 0, 2),
            SwimData(4, 1000, 30, 220, 50, 30, 15, 5, 1),
            SwimData(7, 1500, 42, 320, 45, 30, 20, 5, 3),
            SwimData(9, 600, 18, 130, 70, 20, 10, 0, 1),
            SwimData(10, 2000, 55, 440, 40, 25, 20, 15, 2),
            SwimData(13, 1200, 35, 260, 50, 30, 15, 5, 2),
            SwimData(15, 900, 28, 200, 60, 25, 15, 0, 1),
            SwimData(16, 1800, 50, 400, 35, 30, 20, 15, 3),
            SwimData(18, 2000, 58, 450, 40, 25, 20, 15, 2),
            SwimData(20, 1200, 36, 270, 50, 30, 15, 5, 2),
            SwimData(22, 1500, 42, 320, 45, 30, 20, 5, 3),
            SwimData(23, 800, 24, 175, 60, 30, 10, 0, 1),
            SwimData(25, 1100, 32, 240, 50, 25, 20, 5, 2),
            SwimData(27, 1600, 45, 350, 40, 30, 20, 10, 3),
        )

        for (log in logs) {
            val date = "2026-05-%02d".format(log.day)
            swimLogDao.insert(
                SwimLogEntity(
                    userId = "debug_user",
                    date = date,
                    distanceMeters = log.distance,
                    durationSeconds = log.durationMin * 60,
                    calories = log.calories,
                    strokeFreeStyle = log.free,
                    strokeBreast = log.breast,
                    strokeBack = log.back,
                    strokeFly = log.fly,
                    source = "health_connect",
                    shellsEarned = log.shells,
                    synced = false,
                    createdAt = now,
                )
            )
        }
    }
}
