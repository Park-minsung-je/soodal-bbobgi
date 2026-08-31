# 수달 뽑기 (Soodal Bbobgi)

수영을 기록하면 조개를 받고, 조개로 수달 캐릭터를 뽑아 모으는 안드로이드 앱.

## 주요 기능

- **수영 기록** — Health Connect 연동 + 수동 입력, 달력·통계·심박 차트
- **뽑기(인양소)** — 조개로 룰렛을 돌려 캐릭터·배경·액자 수집
- **프로필 카드** — 수집한 에셋으로 꾸미고 갤러리 저장·공유
- **계정** — Google / Kakao OAuth, 서버 동기화

## 기술 스택

- Kotlin · Jetpack Compose (minSdk 29, targetSdk 35)
- MVVM + Clean Architecture, Hilt DI
- Room, Retrofit, Health Connect API, Haze
- Baseline Profile

## 프로젝트 구조

```
app/
├── core/           DI 모듈(Hilt), 디자인 시스템, 공용 UI, 유틸
├── data/           Room DB·DAO·Entity, Health Connect, Repository 구현체, 원격 API
├── domain/         도메인 모델, Repository 인터페이스, UseCase (Android 의존성 없음)
└── presentation/   Compose 화면 (onboarding, home, calendar, gacha, shop, profile, settings)
baselineprofile/    Baseline Profile 생성기 (Macrobenchmark)
```

## 빌드

`local.properties.sample`을 `local.properties`로 복사한 뒤 값을 채운다.

```bash
./gradlew assembleDebug          # 디버그 빌드
./gradlew assembleRelease        # 릴리즈 APK
./gradlew testDebugUnitTest      # 유닛 테스트
```

## 브랜치

- `main` — 배포된 상태
- `dev` — 개발 통합 (feature/fix 브랜치는 여기로)
- `release/YYYY-MM-DD` — 배포 시점 스냅샷
