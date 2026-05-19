# TinyCraft Dedicated Server: быстрый запуск на VPS

## Что купить

Стартовая конфигурация для небольшого публичного сервера:

- VPS/VDS: 2 vCPU, 4 GB RAM, 40-50 GB NVMe.
- OS: Ubuntu 24.04 LTS.
- Сеть: публичный IPv4.
- Firewall/security group: открыть TCP-порт `25566`.

Подойдут простые VPS у Selectel или Timeweb Cloud. Цены меняются, поэтому перед оплатой проверяй официальный тариф.

## Как запустить

1. Подключиться к VPS по SSH.
2. Поставить Java:

```sh
sudo apt update
sudo apt install -y openjdk-17-jre-headless openjdk-17-jdk-headless
```

3. Залить папку TinyCraft на сервер.
4. В папке игры запустить:

```sh
chmod +x run-server.sh
./run-server.sh
```

5. После первого запуска появятся:

- `server.properties`
- `ops.json`
- `whitelist.json`
- `banned-players.json`
- `logs/latest.log`
- `backups/`

## Настройки

Главные поля в `server.properties`:

```properties
port=25566
world=server_world
maxPlayers=8
motd=TinyCraft Snapshot 9 Server
allowPvp=true
allowCheats=false
whitelist=false
viewDistance=8
```

После изменения настроек перезапусти сервер.

## Команды консоли

```text
help
status
list
say <message>
kick <player> [reason]
save-all
op <player>
deop <player>
whitelist on|off|list|add <player>|remove <player>
ban <player> [reason]
pardon <player>
tp <player> <x> <y> <z>
gamemode <survival|creative|spectator> <player>
stop
```

## Как подключаться

В клиенте TinyCraft укажи:

- Host: публичный IP VPS или домен.
- Port: `25566`.

Если используешь домен, создай `A` record на IP VPS. SRV-записи пока не нужны.
