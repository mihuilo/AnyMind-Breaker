# Any-Mind-Breaker

Офлайн-сборник интеллектуальных головоломок для Android: криптограмма, судоку и общий мета-слой со статистикой. Работает без интернета, без аккаунта и без сервера.

## Что есть

- **Судоку 9×9** — четыре уровня сложности, генератор с единственным решением, мгновенная и классическая проверка, подсказка.
- **Криптограмма** — каждая буква заменена числом, своим в каждой игре; русский и английский, жизни, три вида подсказок, таблица соответствий.
- **Сохранение** — незавершённая игра переживает закрытие приложения, с главного экрана её можно продолжить.
- **Статистика** — по каждой игре с выбором сложности и языка: победы, победы без ошибок, серии, лучшее время; история и достижения.
- **Настройки** — язык интерфейса и язык головоломок (независимо), тема, звук, вибрация, сброс прогресса.

## Стек

Kotlin, Jetpack Compose (Material 3), MVVM + UDF, Navigation Compose, Room, DataStore, Coroutines + Flow, kotlinx.serialization, Gradle Kotlin DSL.

## Структура

```
com.anymindbreaker
├── app                  приложение и общие зависимости (AppContainer)
├── core
│   ├── common/game      контракт игры, сессия, очки, GameViewModel, GameRepository
│   ├── database         Room: сессии, сохранённые игры, итоги
│   ├── datastore        настройки
│   ├── navigation       маршруты и NavHost
│   └── ui               тема, общие компоненты, звук и вибрация
└── feature
    ├── cryptogram       data / domain / presentation
    ├── sudoku           domain / presentation
    ├── statistics       domain / presentation
    ├── home, games, settings
```

Новая игра добавляется отдельным пакетом в `feature`: она реализует `PuzzleGame`, наследует `GameViewModel` и регистрируется в `GameType`, `GameCatalog` и `AppNavHost`. Существующие игры при этом не меняются.

Фразы для криптограмм лежат в `app/src/main/assets/puzzles/` и не зашиты в код: числа и подсказки назначаются при запуске игры.

## Сборка и тесты

Нужны JDK 17+ и Android SDK (путь указывается в `local.properties`, параметр `sdk.dir`).

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

UI- и интеграционные тесты запускаются на подключённом устройстве или эмуляторе:

```bash
./gradlew connectedDebugAndroidTest
```

После этого прогона приложение удаляется с устройства; вернуть его можно командой `./gradlew installDebug`.

## Ветки

- `develop` — текущая разработка.
- `master` — только проверенные результаты из `develop`.
