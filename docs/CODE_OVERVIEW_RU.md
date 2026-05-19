# TinyCraft: карта кода

Этот файл нужен как быстрый вход в проект. Если что-то ломается, начинай отсюда, а не с хаотичного поиска по всем `.java`.

## Главные файлы

- `TinyCraft.java` - клиент игры: главный loop, ввод, меню, HUD, inventory UI, интеграция renderer/world/multiplayer.
- `VoxelWorld.java` - мир: чанки, генерация, блоки, мобы, dropped items, сохранения, сетевой mirror world.
- `MultiplayerManager.java` - TCP multiplayer: protocol handlers, server-authority, inventory/container sync, block updates, chat commands.
- `MultiplayerProtocol.java` - номера пакетов, версия protocol, framing, binary helpers.
- `InventorySystem.java` - предметы, стеки, player inventory, creative inventory, chest/furnace/workbench containers.
- `GameServer.java` - headless dedicated server: tick loop, autosave, console commands, ops/whitelist/ban.
- `TinyCraftServer.java` - entrypoint dedicated server.
- `OpenGlRenderer.java`, `UiRenderer.java`, `SkyRenderer.java`, `WaterRenderer.java` - отрисовка мира и интерфейса.
- `GameData.java`, `Blocks.java` - базовые constants, block registry, player/mob structs, shared data classes.
- `WorldGenerator.java` - terrain/biomes/caves/structures.
- `RegionStorage.java` - сохранение чанков.
- `ServerProperties.java`, `ServerAccessList.java` - `server.properties`, ops, whitelist, ban list.

## Как читать поток игры

1. `run-game.bat` запускает `TinyCraft`.
2. `TinyCraft.main` создает окно, renderer, world и входит в игровой loop.
3. В singleplayer ввод сразу меняет `VoxelWorld`.
4. В multiplayer клиент отправляет запросы в `MultiplayerManager`.
5. Dedicated server принимает запрос, проверяет правила, меняет свой `VoxelWorld`.
6. Сервер шлет `BLOCK_UPDATE`, `INVENTORY_SYNC`, `CONTAINER_UPDATE`, snapshots.
7. Клиент применяет эти ответы и перестраивает mesh.

## Где чинить частые баги

- Спавн/падение в пустоту: `VoxelWorld.placePlayerAtSpawn`, `safeStandingYAt`, `MultiplayerManager.repairUnsafeJoinPosition`.
- Дыры в чанках: `TinyCraft` loading gate, `MultiplayerManager` chunk request/data flow, `VoxelWorld.readNetworkColumn`.
- Не ломаются/не ставятся блоки: client calls in `TinyCraft`, server `handleBlockAction` in `MultiplayerManager`, `VoxelWorld.breakBlock/placeBlock`.
- Не работает creative inventory: `TinyCraft.onClientServerPlayerState`, `InventorySystem.handleClick`, server `CONTAINER_CLICK`.
- Команды: local chat in `ChatSystem`, dedicated console/client commands in `GameServer`, LAN fallback in `MultiplayerManager`.
- Предметы исчезают/дюпаются: `PlayerInventory.handleClick`, `sendInventorySync`, container handlers in `MultiplayerManager`.

## Важные правила Snapshot 9

- Protocol version: `MultiplayerProtocol.VERSION = 4`.
- Snapshot 8 clients intentionally incompatible.
- Dedicated server authoritative for player mode/health, inventory, containers, block place/break.
- User-facing namespace is `tinycraft:name`; internal block registry still uses legacy names until the registry is renamed.
- Windows-first release: current bundled LWJGL natives are Windows-only.
