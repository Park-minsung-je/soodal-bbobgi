# 수달 뽑기 (Soodal Bbobgi)

수영을 기록하면 조개를 받고, 조개로 수달 캐릭터를 뽑아 모으는 안드로이드 앱.

수영장에 다녀오면 Health Connect에서 기록을 가져와 달력에 쌓고, 하루 한 번 조개를 지급한다.
모은 조개로 룰렛을 돌려 수달 캐릭터·배경·액자를 수집하고, 도트아트 프로필 카드를 꾸며
갤러리에 저장하거나 공유할 수 있다.

## 주요 기능

- **수영 기록** — Health Connect 연동 (거리·시간·칼로리·심박 곡선), 수동 입력, 영법별 거리
- **달력 & 통계** — 월별 달력, 일별 상세, 주간/월간 통계, 심박 차트
- **재화** — 조개(하루 1회 수영 보상) · 진주(중복 아이템 자동 교환)
- **뽑기(인양소)** — 물리 감속 룰렛 → 상자 오픈 → 결과. 캐릭터/배경/액자 수집
- **프로필 카드** — 수집한 에셋으로 도트아트 카드 합성, 갤러리 저장·공유
- **계정** — Google / Kakao OAuth, 서버 동기화 (기기 변경 시 복원)

## 기술 스택

- Kotlin · Jetpack Compose (minSdk 29, targetSdk 35)
- MVVM + Clean Architecture, Hilt DI
- Room, Retrofit, Health Connect API
- 글래스모피즘 UI (Haze), Baseline Profile

## 프로젝트 구조

```
app/
├── core/           DI 모듈(Hilt), 디자인 시스템, 공용 UI, 유틸
├── data/           Room DB·DAO·Entity, Health Connect, Repository 구현체, 원격 API
├── domain/         도메인 모델, Repository 인터페이스, UseCase (Android 의존성 없음)
└── presentation/   Compose 화면 (onboarding, home, calendar, gacha, shop, profile, settings)
baselineprofile/    Baseline Profile 생성기 (Macrobenchmark)
```

## 빌드 설정

서버 주소와 OAuth 키는 소스에 두지 않는다. `local.properties.sample`을 `local.properties`로
복사한 뒤 값을 채운다 (`local.properties`는 git 미추적, 같은 이름의 환경변수도 가능).
값이 없으면 Gradle이 즉시 실패한다.

| 키 | 내용 |
|---|---|
| `SOODAL_BASE_URL` | API 서버 베이스 URL (끝에 `/` 포함) |
| `SOODAL_ASSET_BASE_URL` | 에셋 다운로드 베이스 URL (끝에 `/` 없음) |
| `SOODAL_KAKAO_NATIVE_APP_KEY` | Kakao 네이티브 앱 키 |
| `SOODAL_GOOGLE_WEB_CLIENT_ID` | Google Sign-In 웹 클라이언트 ID |

```bash
./gradlew assembleDebug          # 디버그 빌드
./gradlew assembleRelease        # 릴리즈 APK (R8 난독화 + 리소스 축소)
./gradlew testDebugUnitTest      # 유닛 테스트
```

## 브랜치

- `main` — 배포된 상태
- `dev` — 개발 통합 (feature/fix 브랜치는 여기로)
- `release/YYYY-MM-DD` — 배포 시점 스냅샷
