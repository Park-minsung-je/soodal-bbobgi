-keepattributes Signature
-keepattributes *Annotation*

# 서버 DTO — Gson 리플렉션 직렬화라 필드명 유지 필수
-keep class com.soodalbbobgi.app.data.remote.** { *; }

# 에셋 매니페스트 — AssetStore가 도메인 모델을 Gson으로 직접 읽고 쓴다 (manifest.json)
-keep class com.soodalbbobgi.app.domain.model.AssetManifest { *; }
-keep class com.soodalbbobgi.app.domain.model.AssetFile { *; }

-keep class com.kakao.sdk.** { *; }
-keep class com.google.gson.** { *; }

# R8 full mode에서 TypeToken 제네릭 시그니처 보존 (Gson 2.10.x 대응)
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
