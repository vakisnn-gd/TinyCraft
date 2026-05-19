# TinyCraft Snapshot 9: ручная проверка

Цель: быстро вытащить очевидные поломки перед тем, как отдавать zip другому человеку.

## 1. Сборка

```bat
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
.\run-tests.bat
```

Ожидание: компиляция проходит, тесты показывают `OK`.

## 2. Dedicated join

```bat
.\run-server.bat
.\run-game.bat
```

В клиенте: Multiplayer -> Direct IP -> `127.0.0.1:25566`.

Ожидание:

- игрок появляется около нуля, на земле или очень близко к безопасной поверхности;
- рядом нет больших незагруженных дыр после loading screen;
- server log не показывает `Too many chunk requests` или `Too many player state packets`.

## 3. Команды

В server console:

```text
give Player2873 dirt 64
gamemode creative Player2873
clear Player2873
tp Player2873 0 90 0
gamemode survival Player2873
```

В client chat, если игрок op:

```text
/give dirt 64
/give tinycraft:dirt 64
/gamemode creative
/clear
```

Ожидание: подсказки используют `tinycraft:`, не `minecraft:`.

## 4. Survival gameplay

- Поставить dirt из hotbar.
- Сломать dirt, wood, stone рядом с игроком.
- Проверить вторым клиентом, что блоки исчезают/появляются одинаково.

Ожидание: блок ломается через server update, без ghost state и без silent fail.

## 5. Creative inventory

- Console: `gamemode creative Player2873`.
- Открыть inventory.
- Кликнуть creative item.
- Положить stack в hotbar.
- Поставить несколько блоков.

Ожидание: item появляется в cursor/hotbar, блоки ставятся, inventory не расходуется.

## 6. Containers

- Поставить chest, furnace, crafting table.
- Открыть каждый.
- Проверить left click, right click, shift-click, close with Esc.
- Вторым клиентом сломать chest/furnace/table, пока первый держит UI открытым.

Ожидание: UI закрывается или становится неактивным, items не дюпаются и не исчезают.

## 7. Reconnect

- Выдать item.
- Переложить item в hotbar/storage.
- Выйти и зайти тем же profile.

Ожидание: позиция чинится на безопасную поверхность, inventory сохраняется.

## 8. Mobs/items quick check

- Понаблюдать mobs 30 секунд двумя клиентами.
- Ударить mob рядом и на границе reach.
- Выбросить/подобрать item.

Ожидание: нет сильного flicker, local mob не умирает до server snapshot, item pickup виден обоим клиентам.
