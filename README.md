# TinyCraft

TinyCraft - небольшая Java/LWJGL voxel-песочница в духе Minecraft. В проекте есть singleplayer, чанковый мир, биомы, пещеры, деревни, мобы, инвентарь, крафт, сундуки, печки, жидкости, чат, LAN/Direct IP multiplayer и dedicated server.

Текущая стабильная база: `v0.2 Final`. Проект продолжается: следующий ориентир - `TinyCraft v0.3`, где основной фокус на более живом gameplay и понятном multiplayer через VPS.

Лаунчер вынесен в отдельную папку `TinyCraftLauncher/`, чтобы его можно было разрабатывать отдельно, не смешивая с игрой.

## Скриншоты

![LAN multiplayer player model](docs/screenshots/snapshot7-lan-player.png)

![Coast and ocean](docs/screenshots/snapshot7-coast.png)

![Village in forest](docs/screenshots/snapshot7-village.png)

![Mountain biome](docs/screenshots/snapshot7-mountains.png)

![Mineshaft interior](docs/screenshots/snapshot7-mineshaft.png)

## Быстрый старт

Нужна Java 8 или новее. Проект собирается с `--release 8`, поэтому код совместим с Java 8 runtime. Сейчас bundled LWJGL natives рассчитаны на Windows.

Сборка и запуск игры из исходников:

```powershell
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
java -cp "out;lib/*" TinyCraft
```

На Windows можно проще:

```powershell
.\run-game.bat
```

## Dedicated Server

Dedicated server запускается без OpenGL-окна, renderer, GLFW и аудио. Он слушает TCP-порт `25566` и принимает обычных клиентов TinyCraft.

Запуск из исходников:

```powershell
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
java -cp "out;lib/*" TinyCraftServer --world server_world --port 25566
```

На Windows:

```powershell
.\run-server.bat
```

На Linux/VPS:

```bash
chmod +x run-server.sh
./run-server.sh
```

Подробная инструкция для VPS лежит в `docs/SERVER_HOSTING_RU.md`.

Основные настройки сервера создаются в локальном `server.properties`:

```properties
port=25566
world=server_world
seed=
terrain=default
maxPlayers=8
motd=TinyCraft v0.2 Final Server
allowPvp=true
allowCheats=false
viewDistance=8
```

## Сборка релизов

Собрать Windows ZIP игры:

```powershell
.\build-release.bat
```

Результат появится в `release/TinyCraft-v0.2-final-windows/` и `release/TinyCraft-v0.2-final-windows.zip`.
Лаунчер собирается отдельно из `TinyCraftLauncher/`.

## Тесты

Сборка всех исходников:

```powershell
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
```

Headless-тесты протокола и server-side логики:

```powershell
.\run-tests.bat
```

## Что лежит в папке

- `TinyCraft.java` - главный клиент игры: меню, игровой loop, ввод, HUD, интеграция мира, renderer и multiplayer.
- `VoxelWorld.java`, `WorldGenerator.java`, `RegionStorage.java` - мир, генерация, чанки, сохранения.
- `OpenGlRenderer.java`, `UiRenderer.java`, `SkyRenderer.java`, `WaterRenderer.java` - отрисовка мира и интерфейса.
- `InventorySystem.java`, `GameData.java`, `Blocks.java` - предметы, блоки, инвентарь, общие игровые данные.
- `TinyCraftServer.java`, `GameServer.java`, `ServerProperties.java`, `ServerAccessList.java` - dedicated server и его настройки.
- `MultiplayerManager.java`, `MultiplayerProtocol.java`, `NetworkConnection.java` - сетевой код и протокол.
- `assets/`, `sounds/`, `lib/` - ресурсы и зависимости.
- `tests/` - JUnit/headless-тесты.
- `docs/` - карта кода, чеклисты, VPS-инструкция и скриншоты.
- `out/`, `build/`, `dist/`, `release/`, `*.class`, `logs/`, `saves/` - локальные результаты сборки/запуска; они не являются исходниками.

## Полезные документы

- `ROADMAP.md` - ближайший план развития игры и сервера.
- `docs/CODE_OVERVIEW_RU.md` - карта кода: с какого файла начинать и где искать частые баги.
- `docs/QA_CHECKLIST_RU.md` - ручной чеклист проверки.
- `docs/SERVER_HOSTING_RU.md` - как поднять сервер на VPS.
- `TinyCraftLauncher/` - отдельная папка лаунчера с собственным запуском и сборкой.
- `FAQ.md` и `KNOWN_ISSUES.md` - вопросы, ограничения и известные проблемы.

## Возможности v0.2 Final

- Singleplayer survival/creative/spectator.
- Чанковый voxel-мир с биомами, горами, пляжами, океанами, реками, пещерами, рудами и шахтами.
- Деревни, дома, фермы, дороги, жители и простые структуры.
- Инвентарь, хотбар, крафт, сундуки, печки и верстак.
- Мобы, дроп, яйца спавна, здоровье, голод, бой и базовый PvP.
- Чат с русским вводом, команды и debug overlay.
- LAN/Direct IP подключение к integrated host или dedicated server.
- Server-side синхронизация блоков, чанков, игроков, мобов, здоровья, дропа, инвентаря и containers.
- Таблица игроков по Tab с ping, здоровьем и статусом.

## Ограничения

- Встроенного relay/NAT traversal пока нет.
- Официального публичного списка серверов пока нет.
- Текущие LWJGL natives в `lib/` рассчитаны на Windows.
- Совместимость сохранений между будущими версиями нужно проверять отдельно.
