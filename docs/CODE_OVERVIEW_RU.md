# TinyCraft: карта кода

Этот файл нужен как быстрый вход в проект. Если что-то ломается, начинай отсюда, а не с хаотичного поиска по всем `.java`.

## Как устроен проект

TinyCraft пока остается простым Java-проектом без Maven/Gradle: исходники игры лежат в корне, а сборка идет командами `javac *.java` и `.bat`-скриптами. Лаунчер развивается в отдельном репозитории `TinyCraftLauncher`.

- Игра: клиент, renderer, мир, инвентарь, gameplay.
- Сервер: dedicated server, настройки, консольные команды.
- Multiplayer: TCP-протокол, синхронизация мира, игроков, inventory/containers.
- Ресурсы: `assets/`, `sounds/`, `lib/`, shaders и screenshots.
- Тесты: headless JUnit tests в `tests/`.

## Главные файлы

- `TinyCraft.java` - клиент игры: меню, главный loop, ввод, HUD, inventory UI, интеграция renderer/world/multiplayer.
- `VoxelWorld.java` - мир: чанки, генерация, блоки, мобы, dropped items, сохранения, сетевой mirror world.
- `WorldGenerator.java` - terrain, биомы, caves, structures.
- `RegionStorage.java` - сохранение и загрузка регионов/чанков.
- `InventorySystem.java` - предметы, стеки, player inventory, creative inventory, chest/furnace/workbench containers.
- `GameData.java`, `Blocks.java` - общие структуры, constants, block registry, player/mob data.
- `OpenGlRenderer.java`, `UiRenderer.java`, `SkyRenderer.java`, `WaterRenderer.java` - отрисовка мира, UI, неба и воды.
- `AudioEngine.java` - звуки.
- `ChatSystem.java` - чат, ввод, команды на стороне клиента.
- `InputController.java`, `PlayerController.java` - состояние ввода, движение, урон, смерть, голод и регенерация игрока.
- `DebugInfo.java` - сбор и кэширование данных для масштабируемого меню `F3`.

## Сервер и multiplayer

- `TinyCraftServer.java` - entrypoint dedicated server.
- `GameServer.java` - headless server: tick loop, autosave, console commands, players, world.
- `ServerProperties.java` - чтение и запись `server.properties`.
- `ServerAccessList.java` - ops, whitelist, bans.
- `MultiplayerManager.java` - основная multiplayer-логика: protocol handlers, server authority, inventory/container sync, block updates, chat commands.
- `MultiplayerProtocol.java` - номера пакетов, версия протокола, framing, binary helpers.
- `NetworkConnection.java` - сетевое соединение.
- `LanClientTransport.java`, `LanServerTransport.java` - LAN/integrated transport.
- `GameClientSession.java`, `PlayerListEntry.java` - состояние клиента и списка игроков.
- `TinyCraftLauncher` - отдельный репозиторий лаунчера со своей сборкой и ресурсами.

## Как читать поток игры

1. `run-game.bat` запускает `TinyCraft`.
2. `TinyCraft.main` создает окно, renderer, world и входит в игровой loop.
3. В singleplayer ввод сразу меняет `VoxelWorld`.
4. В multiplayer клиент отправляет запросы в `MultiplayerManager`.
5. Dedicated server принимает запрос, проверяет правила и меняет свой `VoxelWorld`.
6. Сервер шлет `BLOCK_UPDATE`, `INVENTORY_SYNC`, `CONTAINER_UPDATE`, snapshots.
7. Клиент применяет ответы и перестраивает mesh.

## Где чинить частые баги

- Спавн/падение в пустоту: `VoxelWorld.placePlayerAtSpawn`, `safeStandingYAt`, `MultiplayerManager.repairUnsafeJoinPosition`.
- Дыры в чанках: loading gate в `TinyCraft`, chunk request/data flow в `MultiplayerManager`, `VoxelWorld.readNetworkColumn`.
- Не ломаются/не ставятся блоки: client calls в `TinyCraft`, server `handleBlockAction` в `MultiplayerManager`, `VoxelWorld.breakBlock/placeBlock`.
- Не работает creative inventory: `TinyCraft.onClientServerPlayerState`, `InventorySystem.handleClick`, server `CONTAINER_CLICK`.
- Команды: local chat в `ChatSystem`, dedicated console/client commands в `GameServer`, LAN fallback в `MultiplayerManager`.
- Предметы исчезают или дюпаются: `PlayerInventory.handleClick`, `sendInventorySync`, container handlers в `MultiplayerManager`.
- Голод, регенерация или смерть: `PlayerController`, поля player state в `GameData.java`, сохранение/восстановление в `TinyCraft.java`.
- `F2`, `F3` или панорама: обработка клавиш в `TinyCraft.java`, захват и UI в `OpenGlRenderer.java`, данные overlay в `DebugInfo.java`.

## Сборка и временные файлы

- `out/` - результат `javac`.
- `build/` - промежуточная сборка.
- `dist/` - локальный app image.
- `release/` - ZIP-релизы.
- `*.class` в корне - случайные/старые class-файлы, их можно пересобрать.
- `logs/`, `saves/`, `server.properties`, `profile.properties`, `options.txt` - локальные данные запуска.

Эти файлы не считаются исходниками и должны игнорироваться Git.

## Важные правила текущей базы

- Текущая база и ближайший релиз: `v0.2.1`.
- Следующий ориентир разработки: `v0.3`.
- Protocol version в v0.2.1: `MultiplayerProtocol.VERSION = 7`.
- Dedicated server authoritative for player mode/health, inventory, containers, block place/break.
- User-facing namespace is `tinycraft:name`; internal block registry still uses legacy names until the registry is renamed.
- Windows-first release: current bundled LWJGL natives are Windows-only.
