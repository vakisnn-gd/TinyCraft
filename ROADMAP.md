# Статус разработки

TinyCraft завершен на `v0.2 Final`. Этот файл оставлен как историческая карта того, что было доведено до финального состояния и что сознательно не было превращено в большой production-проект.

## Цель v0.2 Final

- Держать проект компилируемым на Java 8+.
- Держать singleplayer запускаемым.
- Дать игрокам Direct IP/LAN и dedicated server MVP.
- Переиспользовать server world между запусками.
- Показывать player list, ping и базовую диагностику соединения.
- Синхронизировать основные gameplay-события: chunks, blocks, chat, players, mobs, drops, health, pickup, inventory и containers.
- Валидировать базовые multiplayer-действия: размеры пакетов, частоту действий, дистанцию `BLOCK_ACTION`, совместимость протокола.
- Честно не добавлять внешние аккаунты, relay, NAT traversal и публичную инфраструктуру.
- Завершить проект финальным релизом без обещания следующих snapshot.

## Сделано к v0.2 Final

- `MultiplayerProtocol.VERSION = 7`; старые клиенты отклоняются как несовместимые.
- Добавлены `INVENTORY_SYNC` и `CONTAINER_*` пакеты.
- Сундуки, печки и верстак открываются и меняются через server-side window id.
- Клиентский mirror больше не применяет ломание/установку блоков до server `BLOCK_UPDATE`.
- Добавлены отдельные измерения по `dimensionId`, включая Paradise.
- Добавлены server-side gamemode, creative inventory, item drop и block breaking checks.
- Добавлен экран disconnect с причиной отключения.
- Добавлены JUnit headless tests и `run-tests.bat`.

## Что осталось как идеи, но не планируется

- LAN discovery через broadcast.
- Более подробный server connection screen.
- Whitelist и простые роли host/admin/guest.
- Совместимость сохранений между версиями протокола.
- Документация формата пакетов для разработчиков.
- Улучшение remote player animation.
- Рецепт-книга и полировка inventory UI.
- Больше звуков и texture pack.

## Не реализовано

- Публичные аккаунты и авторизация.
- Официальные публичные серверы.
- Встроенный relay/NAT traversal.
- Полноценный anti-cheat уровня публичных серверов.
- Полная совместимость с Minecraft blockstate/NBT.
- Redstone.
- Nether/End измерения.
- Импорт внешних schematic-файлов.
- Большой переписанный движок вместо текущей инкрементальной разработки.
