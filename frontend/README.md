# DeliveryFlow Web Client

Одностраничный клиент для DeliveryFlow API без фреймворков и сборки.

## Запуск

Через docker-compose из корня репозитория (nginx проксирует API и WebSocket):

```bash
docker compose up -d
# открыть http://localhost:8088
```

Либо любым статическим сервером, если бэкенд запущен через `./gradlew bootRun`:

```bash
cd frontend
python3 -m http.server 8000
# открыть http://localhost:8000
```

## Структура

```
frontend/
├── index.html          # единственная страница, всё остальное рисуют вьюхи
├── css/
│   ├── base.css        # переменные, reset, типографика
│   ├── components.css  # кнопки, формы, бейджи, тосты, степпер
│   └── views.css       # стили конкретных экранов
├── js/
│   ├── app.js          # точка входа
│   ├── config.js       # адреса API/WS
│   ├── api.js          # fetch-обёртка + автообновление JWT
│   ├── auth.js         # сессия в localStorage
│   ├── ws.js           # нативный WebSocket + STOMP, подписки на топики
│   ├── map.js          # обёртка над Leaflet
│   ├── header.js       # шапка с навигацией по роли и live-индикатором
│   ├── router.js       # hash-роутер с проверкой роли
│   ├── ui.js           # бейджи статусов, степпер, тосты
│   └── views/          # экраны: авторизация, клиент, курьер, админ
└── lib/                # stomp.js, leaflet (без CDN)
```
