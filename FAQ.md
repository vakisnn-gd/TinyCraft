# FAQ

## Что это за проект?

TinyCraft - маленькая Java/LWJGL voxel-песочница. В проекте есть чанковый мир, биомы, пещеры, шахты, деревни, мобы, инвентарь, крафт, сундуки, печки, команды, LAN-мультиплеер и dedicated server.

## Какая версия сейчас актуальная?

Актуальная версия исходников и ближайший релиз - `v0.2.1`. После его публикации следующим крупным ориентиром будет `v0.3`.

## Где скачать игру?

Откройте страницу релизов:

[https://github.com/vakisnn-gd/TinyCraft/releases](https://github.com/vakisnn-gd/TinyCraft/releases)

Для новой публикации используются:

- релиз: `TinyCraft v0.2.1`
- tag: `v0.2.1`
- asset: `TinyCraft-v0.2.1-windows.zip`

Скачивайте именно `.zip` из assets. `Source code` - это архив исходников от GitHub, он больше подходит для разработки.

## Какая Java нужна?

Нужна Java 8 или новее. Проект собирается командой с `--release 8`, поэтому совместим с Java 8 runtime.

Проверить Java:

```powershell
java -version
```

## Как собрать и запустить из исходников?

```powershell
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
java -cp "out;lib/*" TinyCraft
```

На Windows также можно запустить:

```powershell
.\run-game.bat
```

## Как сделать скриншот или панораму?

- `F2` сохраняет обычный PNG-скриншот в `screenshots/`.
- `F6` автоматически снимает шесть сторон панорамы и складывает их в отдельную папку `screenshots/panorama-.../`.
- Чтобы новая панорама появилась в меню, скопируйте полученные `panorama_0.png` - `panorama_5.png` в `assets/menu_panorama/` и перезапустите игру.

## Как запустить dedicated server?

```powershell
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
java -cp "out;lib/*" TinyCraftServer --world server_world --port 25566
```

На Windows:

```powershell
.\run-server.bat
```

Сервер создает локальный `server.properties` и хранит мир в `saves/<world>`. Если запускать сервер снова с тем же `world`, он откроет тот же мир.

## Поддерживается ли мультиплеер?

Да. `v0.2.1` поддерживает Direct IP / LAN, dedicated server и server-authority проход для инвентаря/контейнеров.

Работает:

- подключение к `127.0.0.1` на одном ПК;
- подключение в локальной сети по IP;
- подключение к dedicated server при открытом TCP-порту;
- синхронизация чанков, блоков, игроков, мобов, дропа, времени и чата;
- server-side MVP для инвентаря, сундуков, печек и верстака;
- Tab player list с ping и здоровьем;
- команды `/list`, `/ping`, `/msg`, `/kick`, `/give`, `/clear`, `/gamemode`, `/tp`;
- базовый PvP и server-side mob attack.

Пока не готово:

- публичный список серверов;
- relay/NAT traversal;
- внешние аккаунты;
- полноценный anti-cheat;
- полная server-side модель особых предметных действий вроде buckets, spawn eggs и еды;
- никнеймы над игроками.

## Как подключиться к серверу?

1. Запустите `TinyCraftServer`.
2. Запустите `TinyCraft`.
3. Откройте "Мультиплеер".
4. Введите IP сервера. На том же ПК используйте `127.0.0.1`.
5. Порт оставьте `25566`, если он не менялся.
6. Нажмите "Подключиться".

Для подключения через интернет нужно вручную настроить firewall, port-forward или VPN.

## Что такое `profile.properties`?

Это локальный профиль игрока:

- UUID;
- ник.

Файл создается автоматически и не должен попадать в GitHub. Если две копии игры используют один и тот же `profile.properties`, сервер отклонит вторую как `Duplicate player uuid`.

## Какие команды есть в multiplayer?

```text
/list
/ping
/msg <player> <message>
/kick <player> [reason]
/give <id|tinycraft:name> <amount>
/clear <player>
/gamemode <survival|creative|spectator> <player>
/tp <player> <x> <y> <z>
```

На dedicated server права команд ограничиваются operator/console-стороной. Игроки получают обратную связь через чат.

## Почему релиз только Windows?

В репозитории сейчас лежат LWJGL natives только для Windows. Релиз `v0.2.1` собирается как Windows ZIP; Linux/macOS packaging в проекте пока не завершён.

## Можно ли менять код?

Да. Проект распространяется по MIT License.
