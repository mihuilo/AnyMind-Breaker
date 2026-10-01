# Any-Mind-Breaker

Офлайн-сборник интеллектуальных головоломок для Android: криптограмма, судоку и общий мета-слой со статистикой.

## Стек

Kotlin, Jetpack Compose (Material 3), MVVM + UDF, Navigation Compose, Room, DataStore, Coroutines + Flow, Gradle Kotlin DSL.

## Сборка

Нужны JDK 17+ и Android SDK (путь указывается в `local.properties`, параметр `sdk.dir`).

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Ветки

- `develop` — текущая разработка.
- `master` — только проверенные результаты из `develop`.
