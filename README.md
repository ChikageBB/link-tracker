# Link Tracker Bot

Telegram-бот для отслеживания ссылок.

## Требования

- Java 21+
- Maven 3.9+
- Telegram Bot Token

## Быстрый старт

1. Склонируйте репозиторий:

```bash
git clone <url>
cd link-tracker/bot
```

2. Создайте файл `.env` в корне модуля:

```env
APP_TELEGRAM_TOKEN=ваш_токен_от_BotFather
```

3. Запустите приложение:

```bash
mvn spring-boot:run
```

## Запуск тестов

```bash
mvn test
```

Тесты используют WireMock для эмуляции Telegram API — реальный токен не нужен.

## Команды бота

- `/start` — запустить бота
- `/help` — список доступных команд

