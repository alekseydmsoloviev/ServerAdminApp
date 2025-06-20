

# Обязательно к прочтению!!!!


Проект Fast API сервера ```https://github.com/alekseydmsoloviev/SynapseChat_v4.0``` содержит два FastAPI-приложения:

* **Публичный API** – обслуживает чаты и историю сообщений.
* **Админ-панель** – управление пользователями, конфигурацией и моделями Ollama.


Файл логов задаётся переменной окружения `LOG_PATH` (по умолчанию `app.log`). Он
создаётся при запуске сервера и удаляется при завершении.
Переменная `DAILY_LIMIT` задаёт общий дневной лимит запросов ко всем моделям.
При его превышении сервер отклоняет новые обращения, но индивидуальные
лимиты пользователей остаются неизменными.

## Запуск

Рекомендуется использовать скрипт `cli.py`. При первом запуске он предложит
создать администратора и задать порты. По умолчанию API работает на `PORT=8000`,
админ-панель – на `ADMIN_PORT=8080`.

```
python cli.py
```

После старта админ-панель доступна по адресу
`http://localhost:<ADMIN_PORT>/admin` и автоматически запустит публичный API.

## Аутентификация

Все эндпоинты (кроме `/ping`) требуют HTTP Basic авторизации. Административные
маршруты `/admin/api/...` и WebSocket `/admin/ws` доступны только пользователям с
флагом `is_admin=True`.

## Публичный API

Базовый URL: `http://<host>:<PORT>`

### GET `/ping`
Проверка доступности сервиса. Возвращает
```json
{ "message": "pong", "user": "<username>" }
```

### POST `/chat/{session_id}`
Отправить сообщение в модель внутри указанной сессии. Если сессия отсутствует,
она создаётся автоматически.

Запрос:
```json
{ "model": "llama2", "prompt": "Привет!" }
```
Ответ:
```json
{ "response": "<ответ модели>" }
```

### GET `/history/sessions`
Список чатов пользователя.
```json
[
  { "session_id": "abcd", "created_at": "2024-03-01T12:34:56Z" }
]
```

### GET `/history/{session_id}`
Сообщения выбранной сессии.
```json
[
  {
    "role": "user",
    "model": "llama2",
    "content": "Привет",
    "timestamp": "2024-03-01T12:35:00Z"
  },
  {
    "role": "assistant",
    "model": "llama2",
    "content": "Здравствуйте",
    "timestamp": "2024-03-01T12:35:01Z"
  }
]
```

### DELETE `/history/{session_id}`
Удалить сессию и все её сообщения.

### GET `/limits`
Текущий дневной лимит запросов пользователя.
```json
{ "daily_limit": 100, "used": 5, "remaining": 95 }
```

## Административный API

Базовый URL: `http://<host>:<ADMIN_PORT>/admin/api`

### Пользователи
* `GET /users` – список пользователей.
* `POST /users` – создать или обновить пользователя.
  ```json
  { "username": "u", "password": "p", "daily_limit": 1000 }
  ```
* `DELETE /users/{username}` – удалить обычного пользователя.
* `GET /users/{username}` – подробная информация о пользователе.

### Конфигурация
* `GET /config` – текущее значение порта API и дневного лимита.
* `POST /config` – обновить порт и лимит.
  ```json
  { "port": "8000", "daily_limit": "1000" }
  ```
  Значение `daily_limit` ограничивает суммарное число запросов от всех
  пользователей. Оно не изменяет индивидуальные лимиты, заданные для
  конкретных учётных записей.

### Модели
* `GET /models` – установленные модели.
* `GET /models/available` – модели, доступные для установки.
* `GET /models/{name}/variants` – все варианты конкретной модели.
* `POST /models/{name}/install` – установить модель.
* `DELETE /models/{name}` – удалить модель.

### Сессии
* `GET /sessions` – список чатов с количеством сообщений.
* `GET /sessions/{id}` – полный состав сообщений указанного чата.
* `DELETE /sessions/{id}` – удалить чат вместе с его сообщениями.

Заголовок чата (`title`) берётся из первого сообщения пользователя.

### Управление сервером
* `POST /restart` – перезапуск публичного API.
* `GET /status` – порт API, состояние процесса и число сессий.
* `GET /logs` – последние строки лог-файла (параметр `lines`).
* `GET /usage` – суммарное число запросов по пользователям.

## WebSocket `/admin/ws`

WebSocket требует тех же учётных данных администратора. Авторизацию можно
передать через заголовок `Authorization: Basic` либо через параметры запроса
`username` и `password`.

После подключения каждые пять секунд сервер отправляет сообщение с текущими показателями.

Поле `network` показывает средний трафик за последние пять секунд в мегабитах в секунду и округляется до сотых:

```json
{
  "type": "metrics",
  "cpu": 12.3,
  "memory": 45.6,
  "network": 1.2,
  "disk": 80.0,
  "day_total": 5,
  "total": 42,
  "users": ["admin", "user1"],
  "models": ["llama2", "gemma"],
  "port": "9000"
}
```
Помимо этих сообщений сервер отправляет строки прогресса установки моделей в виде `{"type": "progress", "data": "..."}`.

Во время установки моделей дополнительно передаются строки прогресса:
```json
{ "type": "progress", "data": "Downloading… 40% (20s left)" }
```
Эти строки также сохраняются в лог-файл.

## Пример подключения к WebSocket

```
AUTH=$(printf 'admin:password' | base64)
websocat -H "Authorization: Basic $AUTH" ws://localhost:8080/admin/ws
```

## Структура мобильного приложения

В репозитории находится Android-приложение для управления сервером. Основной код расположен в каталоге `app/src/main/java/com/example/serveradminapp`.

### Основные экраны
- `LoginActivity` — ввод адреса сервера и учетных данных. Сохраняет подключения в базе через `AccountDbHelper`.
- `MainActivity` — главный экран с нижней навигацией, запускает постоянный WebSocket через `ServerApi.startMetricsSocket()`.
- `DashboardFragment` — отображает текущие показатели сервера в `GaugeView` и статистику сообщений.
- `UsersFragment` и `UsersActivity` — управление пользователями (создание, удаление, просмотр) с использованием методов `listUsers`, `createUser`, `updateUser`, `deleteUser`.
- `ModelsFragment` и `ModelsActivity` — установка и удаление моделей (`listModels`, `availableModels`, `installModel`, `installModelVariant`, `deleteModel`).
- `ChatsFragment` и `ChatsActivity` — список чатов, `ChatDetailActivity` показывает историю сообщений через `listSessions`, `getSession` и `deleteSession`.
- `SettingsFragment` и `SettingsActivity` — настройка порта и дневного лимита (`updateConfig`), смена языка и перезапуск сервера.

### Вспомогательные классы
- `ServerApi` — обертка над `OkHttpClient` с методами для REST API и WebSocket. Также сохраняет и восстанавливает учетные данные.
- `LocaleUtil` — управление локалью без закрытия WebSocket (`restart` вызывает `Activity.recreate()`).
- `Account` и `AccountDbHelper` — SQLite-хранилище предыдущих подключений.
- `GaugeView` — кастомный виджет для отображения процента загрузки.

### Ресурсы
- Макеты экранов находятся в `app/src/main/res/layout`.
- Строки локализации расположены в `app/src/main/res/values` и `app/src/main/res/values-ru`.

### Сборка
Для сборки debug-версии выполните:

```bash
./gradlew assembleDebug
```
