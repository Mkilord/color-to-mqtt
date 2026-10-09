# HTTP API

[Назад к README](../README.md)

Приложение слушает `http://localhost:8080`. Авторизации нет, поэтому порт 8080 не стоит открывать за пределы домашней сети.

| Запрос | Ответ |
|---|---|
| `POST /api/capture/start`, `/api/capture/stop` | Запуск и остановка захвата, `{"message": ...}` |
| `GET /api/status` | Состояние захвата: `running`, `color`, `broker`, `topic`, `connected`, `lastPayload`, `lastSentAt`, `error`, `errorAt`, `performance` (`fps`, `captureMs`, `processMs`, `idle`) |
| `POST /api/test-color` | Отправляет на лампы цвет `{"hue":120,"sat":100,"brightness":40}` как есть, без коррекции |
| `GET /api/profiles` | Профили и активный профиль. `POST /api/profiles` (`name`, `copyFrom`) создает, `/activate`, `/rename`, `/delete` меняют |
| `GET /api/screen` | Размер экрана `{"width":..., "height":...}`, 503 без графической среды |
| `GET /api/snapshot?width=&height=` | Снимок области захвата заданного размера, JPEG |

Ошибки приходят в виде `{"error": "текст"}`: 404 профиль не найден, 409 имя занято или удаляется последний профиль, 422 недопустимые значения (еще `errors` по полям формы), 503 нет экрана или брокера.

Пример:

```bash
curl -X POST http://localhost:8080/api/test-color \
  -H "Content-Type: application/json" \
  -d '{"hue":120,"sat":100,"brightness":40}'
```
