package kr.ilf.soodalbbobgi.core.update

/** 인앱 업데이트를 어떤 방식으로 띄울지. */
enum class UpdateMode {
    /** 띄우지 않음 — 새 버전이 없거나 Play가 어느 방식도 허용하지 않음. */
    NONE,
    /** 유연 — 하단 시트로 안내하고 백그라운드로 받은 뒤 재시작만 요구. 일반 업데이트. */
    FLEXIBLE,
    /** 즉시 — 전체 화면으로 강제. 서버가 정한 최소 버전보다 낮을 때. */
    IMMEDIATE,
}

/**
 * 인앱 업데이트 방식을 정한다.
 *
 * 서버 최소 버전([minVersionCode])보다 낮은 앱은 구버전이 서버와 호환되지 않는다는 뜻이므로 즉시 방식,
 * 그 외 새 버전은 유연 방식. Play가 해당 방식을 허용하지 않으면 허용되는 다른 쪽으로 내려간다.
 * 서버 값을 못 받았으면([minVersionCode] null) 강제하지 않는다.
 *
 * @param updateAvailable Play가 새 버전을 알고 있는지
 * @param currentVersionCode 지금 설치된 versionCode
 * @param minVersionCode 서버가 정한 최소 versionCode (못 받았으면 null)
 * @param immediateAllowed Play가 즉시 방식을 허용하는지
 * @param flexibleAllowed Play가 유연 방식을 허용하는지
 */
fun decideUpdateMode(
    updateAvailable: Boolean,
    currentVersionCode: Int,
    minVersionCode: Int?,
    immediateAllowed: Boolean,
    flexibleAllowed: Boolean,
): UpdateMode {
    if (!updateAvailable) return UpdateMode.NONE
    val forced = minVersionCode != null && currentVersionCode < minVersionCode
    return when {
        forced && immediateAllowed -> UpdateMode.IMMEDIATE
        flexibleAllowed -> UpdateMode.FLEXIBLE
        immediateAllowed -> UpdateMode.IMMEDIATE
        else -> UpdateMode.NONE
    }
}
