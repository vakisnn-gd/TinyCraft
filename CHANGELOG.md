# История изменений

## v0.2 Final

Это последний релиз TinyCraft. Разработка проекта завершена; версия опубликована как финальная точка учебного/экспериментального Java/LWJGL Minecraft-like проекта.

### Финальный проход после v0.2.1

- Paradise перенесен в отдельное измерение через `dimensionId`, без координатного offset.
- Multiplayer стал серверно-авторитетным для gamemode, creative inventory, item drops, furnace/container updates и ломания блоков.
- F3+F4 в multiplayer больше не меняет режим локально: смена проходит через серверную `/gamemode`.
- Добавлены короткие команды `/gm`, `/gmc`, `/gms`, `/gmsp`.
- Исправлен экран отключения: клиент показывает причину kick/disconnect.
- Улучшена изоляция chunk/mesh state между измерениями.
- Финальный Windows zip переименован в `TinyCraft-v0.2-final-windows.zip`.

## v0.2.1

v0.2.1 стабилизирует dedicated server после Snapshot 8: добавляет server-authority MVP для инвентаря/контейнеров, усиливает сетевой протокол и вводит JUnit headless tests.

### Добавлено

- `INVENTORY_SYNC` для полного авторитетного состояния инвентаря клиента.
- `CONTAINER_OPEN_REQUEST`, `CONTAINER_OPEN`, `CONTAINER_CLICK`, `CONTAINER_CLOSE`, `CONTAINER_UPDATE` для server-side окон сундуков, печек и верстака.
- Серверная валидация открытия контейнеров по типу блока и дистанции игрока.
- Серверная валидация `BLOCK_ACTION` по дистанции, координатам и авторитетному held item.
- Rate limit для частых player state, chunk request, block action, attack, chat/command и container click пакетов.
- JUnit 4, `run-tests.bat` и headless tests для протокола, inventory codecs, handshake и authority validation.

### Изменено

- Multiplayer protocol поднят до `VERSION = 4`.
- Клиентский multiplayer mirror больше не применяет локально ломание/установку блоков до серверного `BLOCK_UPDATE`.
- `/give`, `/clear`, `/gamemode` и `/tp` проходят через multiplayer/server command path.
- README, FAQ, KNOWN_ISSUES и ROADMAP обновлены под v0.2.1.
- Клиент v0.2.1 несовместим со Snapshot 8 host/server по сетевому протоколу.

### Ограничения v0.2.1

- Это все еще prerelease без public server browser, relay/NAT traversal и внешних аккаунтов.
- Server authority для buckets, spawn eggs, еды и выбрасывания предметов требует следующего прохода.
- Rate limits и проверки протокола не являются полноценным anti-cheat для публичных серверов.
- Только Windows zip в готовом релизе, потому что в `lib/` лежат Windows LWJGL natives.

## v0.2 Snapshot 8

Snapshot 8 добавляет первый dedicated server MVP и несколько заметных multiplayer-улучшений поверх Snapshot 7. Это prerelease, а не stable-релиз.

### Добавлено

- `TinyCraftServer` - headless dedicated server без окна, renderer, GLFW и аудио.
- `GameServer` - серверный tick loop, autosave, console commands и сохранение server world.
- `server.properties` с настройками `port`, `world`, `seed`, `terrain`, `maxPlayers`, `motd`, `allowPvp`, `allowCheats`, `viewDistance`.
- `run-server.bat` для Windows-запуска сервера.
- Повторное использование server world в `saves/<world>` при следующих запусках.
- Сохранение network player state при disconnect, `save`, autosave и `stop`.
- Player list по Tab с ping, здоровьем, game mode/status.
- Multiplayer-команды `/list`, `/ping`, `/msg`, `/kick`.
- Console-команды dedicated server: `help`, `status`, `list`, `say`, `kick`, `save`, `stop`.
- Ping/Pong keepalive и timeout disconnect.
- Server-side mob attack packet.
- Авторитетные обновления здоровья игрока.
- `INVENTORY_ADD` для выдачи подобранных сервером предметов клиенту.
- Более явные ошибки для protocol mismatch, duplicate UUID, full server и connection timeout.

### Изменено

- Multiplayer protocol поднят до `VERSION = 3`.
- README, FAQ, KNOWN_ISSUES и ROADMAP обновлены под Snapshot 8.
- Dedicated server слушает `0.0.0.0:25566` по умолчанию.
- Клиент Snapshot 8 несовместим со старыми Snapshot 7 host/server по сетевому протоколу.

### Ограничения Snapshot 8

- Только Windows zip в готовом релизе, потому что в `lib/` лежат Windows LWJGL natives.
- Нет публичного server browser, relay, NAT traversal и внешних аккаунтов.
- Нет полноценного anti-cheat.
- Полная server-side модель инвентаря и контейнеров еще не завершена.
- Никнеймы над игроками пока отключены.

## v0.2 Snapshot 7

Snapshot 7 добавил первый рабочий MVP LAN-мультиплеера без внешних аккаунтов. Главная цель релиза - дать двум копиям игры подключиться друг к другу напрямую, увидеть один авторитетный мир и проверить базовую синхронизацию игроков, чанков, блоков, чата и времени.

### Добавлено

- Экран "Мультиплеер" с Direct IP подключением.
- Подменю открытия текущего мира для LAN.
- Интегрированный LAN-host внутри обычной игры.
- TCP-протокол без сторонних сетевых библиотек.
- Handshake `HELLO`/`WELCOME`.
- Локальный профиль игрока в `profile.properties`.
- Mirror-режим клиента.
- Передача чанков через `CHUNK_DATA`.
- Авторитетные `BLOCK_UPDATE`.
- Чат через пакет `CHAT`.
- Синхронизация времени, мобов и выпавших предметов.
- Базовый PvP.

### Ограничения Snapshot 7

- Только Direct IP и LAN.
- Нет dedicated server.
- Нет relay, NAT traversal, browser server list и public matchmaking.
- Нет внешних аккаунтов.
- Нет anti-cheat.
- Нет синхронизации содержимого сундуков и сложных контейнеров.

## v0.2 Snapshot 6

- Переработана генерация новых миров: горы стали выше, биомы чаще и крупнее читаются на местности.
- Добавлен тип мира "Большие биомы".
- Улучшены вода/лава, туман непрогруженных чанков, прозрачные блоки и меню настроек.
- Сборка переведена на Java 8 target.

## v0.2 Snapshot 4

- Возвращена дальность прорисовки 32 чанка как нормальный игровой дефолт.
- Загрузка колонок и загрузка мешей разделены, чтобы мир догружался без регулярных фризов.
- Улучшена генерация берегов, океанов, пляжей и переходов между сушей и водой.
- Расширена minecraft-like генерация биомов.
- Добавлены заметные пещеры, шахты, деревни и survival-loop.

## v0.2

- Русский язык используется по умолчанию, в настройках добавлен переключатель языка.
- Оптимизирована подгрузка чанков.
- Исправлен урон лавы, утопления и мобов.
- Добавлены настройки VSCode для Java-файлов и `lib/*.jar`.

## v0.1

- Улучшены генерация мира, чанки, пещеры, деревни и структуры.
- Добавлены инвентарь, хотбар, крафт, сундуки, печки и верстаки.
- Добавлены мобы, дроп, еда, здоровье и голод.
- Добавлены команды телепортации, времени, режимов игры, поиска биомов/структур и debug-информации.
- Подготовлены release-сборки для Windows.
- Добавлена русская документация и MIT License.
