# План разработки

Главное правило: сначала стабилизация, потом большие новые функции. Snapshot 9 закрепляет server authority MVP для dedicated server, inventory и контейнеров, но не обещает публичную серверную инфраструктуру.

## Цель v0.2 Snapshot 9

- Держать проект компилируемым на Java 8+.
- Держать singleplayer запускаемым.
- Дать игрокам Direct IP/LAN и dedicated server MVP.
- Переиспользовать server world между запусками.
- Показывать player list, ping и базовую диагностику соединения.
- Синхронизировать основные gameplay-события: chunks, blocks, chat, players, mobs, drops, health, pickup, inventory и containers.
- Валидировать базовые multiplayer-действия: размеры пакетов, частоту действий, дистанцию `BLOCK_ACTION`, совместимость протокола.
- Честно не добавлять внешние аккаунты, relay, NAT traversal и публичную инфраструктуру.

## Сделано в Snapshot 9

- `MultiplayerProtocol.VERSION = 4`; клиенты Snapshot 8 отклоняются как несовместимые.
- Добавлены `INVENTORY_SYNC` и `CONTAINER_*` пакеты.
- Сундуки, печки и верстак открываются и меняются через server-side window id.
- Клиентский mirror больше не применяет ломание/установку блоков до server `BLOCK_UPDATE`.
- Добавлены JUnit headless tests и `run-tests.bat`.

## Следующий проход стабилизации

1. Server authority
   - Расширить server-side модель на выбрасывание предметов из инвентаря, spawn eggs, buckets и еду.
   - Уточнить permission model для `/give`, `/clear`, `/gamemode`, `/tp` и `allowCheats`.
   - Добавить более подробные server rejection messages в отдельный UI/status слой.

2. Containers
   - Протестировать несколько клиентов, одновременно открывающих один сундук/печь.
   - Добавить server-side drop cursor при disconnect/death.
   - Полировать быстрые перемещения и визуальную обратную связь при отклоненном клике.

3. Connection UX
   - Добавить историю последних серверов.
   - Добавить более подробный экран подключения.
   - Показывать понятные ошибки для firewall, timeout, incompatible protocol и full server.
   - Добавить reconnect после временного разрыва.

4. Protocol hardening
   - Расширить headless tests на real `MultiplayerManager` loopback-сценарии.
   - Валидировать больше gameplay-пакетов против server inventory/state.
   - Добавить опциональные debug logs для packet rejection.
   - Не считать это полноценным anti-cheat для публичных серверов.

5. Observability
   - Добавить опциональные packet/debug логи.
   - Показывать в debug overlay режим сети, ping, remote players и очередь чанков.
   - Подготовить минимальные headless protocol tests без OpenGL.

## Идеи для следующих snapshot

- LAN discovery через broadcast.
- Более подробный server connection screen.
- Whitelist и простые роли host/admin/guest.
- Совместимость сохранений между версиями протокола.
- Документация формата пакетов для разработчиков.
- Улучшение remote player animation.
- Рецепт-книга и полировка inventory UI.
- Больше звуков и texture pack.

## Не планируется для ближайших snapshot

- Публичные аккаунты и авторизация.
- Официальные публичные серверы.
- Встроенный relay/NAT traversal.
- Полноценный anti-cheat уровня публичных серверов.
- Полная совместимость с Minecraft blockstate/NBT.
- Redstone.
- Nether/End измерения.
- Импорт внешних schematic-файлов.
- Большой переписанный движок вместо текущей инкрементальной разработки.
