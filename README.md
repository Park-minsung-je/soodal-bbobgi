# 수달 뽑기 (Soodal Bbobgi)

수영 기록을 추적하고 수달 캐릭터를 수집하는 Android 네이티브 앱.

- **플랫폼**: Kotlin + Jetpack Compose (minSdk 29, targetSdk 35)
- **아키텍처**: MVVM + Clean Architecture + Hilt DI
- **데이터**: Room(로컬) + Health Connect(수영 기록) + 서버 동기화
- **인증**: OAuth 전용 (Google / Kakao)
- **핵심 기능**: 수영 기록 자동 수집, 조개·진주 재화, 뽑기(인양소), 상점, 프로필 카드 편집·합성

관련 레포: 서버(`soodal-bbobgi-server`), 관리자 웹(`soodal-bbobgi-admin`) — 각각 별도 레포지만 **브랜치 전략은 아래와 동일하게 적용**한다.

---

## 브랜치 전략

```
feature/*  ──►  dev  ──►  release/YYYY-MM-DD  ──►  (배포)  ──►  master
   작업          개발 통합        배포 스냅샷                    배포된 상태
```

| 브랜치 | 역할 | 규칙 |
|---|---|---|
| `master` | **실제 배포된 상태**만 담는다 | 개발분을 직접 머지하지 않는다. 배포 완료된 release 브랜치만 머지 |
| `dev` | 개발 통합 브랜치 | 모든 `feature/*`·`fix/*`를 여기로 머지 |
| `release/YYYY-MM-DD` | 배포 시점 **기록용 스냅샷** | 배포일 날짜로 명명. `dev`에서 따고, 배포 후 `master`로 머지 |
| `feature/*`, `fix/*` | 단일 기능/수정 작업 | 기능별로 반드시 분리 |

### 작업 흐름

1. **개발**: `dev`에서 `feature/<기능명>` 또는 `fix/<버그명>` 브랜치를 딴다
   ```bash
   git checkout dev
   git checkout -b feature/shop-stock-emphasis
   ```
2. **머지**: 작업이 끝나고 테스트·빌드가 통과하면 `dev`로 머지 (`--no-ff`로 이력 보존)
   ```bash
   git checkout dev
   git merge --no-ff feature/shop-stock-emphasis
   ```
3. **배포**: 배포 시점에 `dev`에서 배포일 날짜로 release 브랜치를 딴다
   ```bash
   git checkout dev
   git checkout -b release/2026-08-25
   ```
4. **배포 후**: release 브랜치를 `master`에 머지해 "배포된 상태"를 갱신한다
   ```bash
   git checkout master
   git merge --no-ff release/2026-08-25
   ```

### 커밋 규칙

- **한 커밋 = 한 기능.** 여러 기능을 한 커밋에 묶지 않는다
- 커밋 메시지는 **영어, 동사로 시작** (`Add` / `Implement` / `Fix` / `Update` / `Remove`)
- 리팩터링·포맷팅·문서 변경도 별도 커밋

---

## 프로젝트 구조

```
app/
├── core/           DI 모듈(Hilt), 디자인 시스템, 공용 UI, 유틸
├── data/           Room DB·DAO·Entity, Health Connect, Repository 구현체, 원격 API
├── domain/         도메인 모델, Repository 인터페이스, UseCase (Android 의존성 없음)
└── presentation/   Compose 화면 (onboarding, home, calendar, gacha, shop, profile, settings)
```

## 빌드 & 테스트

```bash
./gradlew assembleDebug          # 디버그 빌드
./gradlew assembleRelease        # 릴리즈 APK (R8 난독화 + 리소스 축소)
./gradlew bundleRelease          # Play 업로드용 AAB
./gradlew testDebugUnitTest      # 유닛 테스트
./gradlew connectedAndroidTest   # 계측 테스트 (Room DAO 등)
```

## 문서

기획·설계 문서는 상위 워크스페이스의 `docs/`에서 관리한다.

| 파일 | 내용 |
|---|---|
| `docs/PLAN.md` | 개발 마스터 플랜 |
| `docs/DECISIONS.md` | 확정된 기획·기술 결정 로그 |
| `docs/BUSINESS_LOGIC.md` | 재화·뽑기·상점·동기화 비즈니스 규칙 |
| `docs/SCREEN_SPECS.md` | 화면별 컴포넌트 명세 |
| `docs/API_SPEC.md` | 서버 API 명세 |
| `docs/SERVER_SPEC.md` | 서버·앱 공용 구현 가이드 |
