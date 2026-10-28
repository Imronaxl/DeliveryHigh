# DeliveryFlow Web Client

SPA на ES modules без фреймворка и сборки. Nginx раздаёт файлы и проксирует `/api/` и `/ws` в backend. Leaflet и STOMP включены локально.

Из корня проекта:

```bash
docker compose up --build -d
```

Интерфейс: http://localhost:8088. Для локального backend на 8080:

```bash
cd frontend
python3 -m http.server 8000
```

Тесты не требуют установки npm-зависимостей:

```bash
node --test frontend/tests/*.test.js
```

`js/router.js` управляет hash-маршрутами. `api.js` добавляет JWT и один раз повторяет запрос после refresh. `auth.js` хранит отдельную сессию вкладки. `ws.js` подключает STOMP с JWT, восстанавливает подписки после разрыва. `map.js` оборачивает Leaflet; экраны находятся в `js/views`.

`css/base.css` содержит тему и типографику, `components.css` — общие компоненты, `views.css` — экраны. Геолокация курьера — демосимулятор; дорожной маршрутизации нет.
