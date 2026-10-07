# ColorToMQTTApp

Spring Boot приложение, которое следит за цветом в центре экрана и отправляет его в MQTT. Подходит для подсветки в умном доме: лампа повторяет цвет того, что сейчас на экране.

## Как работает

С периодом `updatePeriod` приложение повторяет цикл:

1. Снимает область в центре экрана размером `screenWight` x `screenHeight` через `java.awt.Robot`.
2. Считает средний цвет по пикселям в шахматном порядке, шаг сетки `cellSize`.
3. Сравнивает его с последним отправленным цветом в HSB. Если разница по тону, насыщенности или яркости больше допуска, цвет считается новым.
4. Сдвигает H, S, B на заданные значения и ограничивает их диапазонами `min*` и `max*`.
5. Публикует цвет в топик MQTT с QoS 0. При ошибке отправки переподключается к брокеру.

Формат сообщения:

```json
{"hue":210,"sat":64,"brightness":48}
```

`hue` в градусах 0..360, `sat` и `brightness` в процентах 0..100.

## Требования

- JDK 22
- Maven
- Графический рабочий стол: снимок экрана делается через AWT, на headless-сервере приложение работать не будет
- MQTT-брокер, например Mosquitto

## Запуск

```bash
export MQTT_BROKER=tcp://<host>:1883
export MQTT_USERNAME=<user>
export MQTT_PASSWORD=<password>
mvn spring-boot:run
```

Веб-интерфейс откроется на http://localhost:8080. На главной странице кнопки запуска и остановки и текущий цвет. На странице `/settings` можно поменять адрес брокера, период опроса и размер области захвата и посмотреть превью этой области.

## Настройка

Настройки по умолчанию лежат в `src/main/resources/application.yaml`, раздел `app.defaultSettings`. Параметры MQTT берутся из переменных окружения:

| Переменная | Назначение | По умолчанию |
|---|---|---|
| `MQTT_BROKER` | Адрес брокера | `tcp://localhost:1883` |
| `MQTT_TOPIC` | Топик для публикации | `colorToMQTT` |
| `MQTT_USERNAME` | Пользователь брокера | пусто |
| `MQTT_PASSWORD` | Пароль брокера | пусто |

Основные параметры захвата и обработки:

| Ключ | Назначение |
|---|---|
| `screenWight`, `screenHeight` | Размер области в центре экрана, px |
| `updatePeriod` | Период опроса, мс |
| `cellSize` | Шаг сетки при подсчете среднего цвета, px |
| `hueTolerance`, `saturationTolerance`, `brightnessTolerance` | Допуск изменения цвета, доли 0..1 |
| `modifyHue`, `modifySaturation`, `modifyBrightness` | Сдвиг H, S, B перед отправкой |
| `minHUE`, `maxHUE`, `minSaturation`, `maxSaturation`, `minBrightness`, `maxBrightness` | Допустимые диапазоны после сдвига |

Настройки, сохраненные на странице `/settings`, пишутся в файл `settings.txt` в рабочей директории (формат Java Properties) и применяются поверх `app.defaultSettings` при следующем запуске захвата. Логин и пароль MQTT в файл не сохраняются и всегда берутся из переменных окружения.

## Скриншоты

![Главная страница](https://github.com/user-attachments/assets/52b3e70b-8391-4697-954a-5acd3e9d3e9a)
![Настройки](https://github.com/user-attachments/assets/b5b07601-3803-4e86-93cd-1ae480ccb143)

## Расширение

Реализации шагов выбираются по полному имени класса в настройках и создаются через `AbstractFactory`. Класс должен иметь конструктор с `java.util.Properties` или конструктор без параметров.

| Ключ | Интерфейс | Реализации |
|---|---|---|
| `processor` | `Processor` | `ChessProcessor` |
| `detector` | `ColorDetector` | `AverageColorDetector` |
| `screenAreaType` | `ScreenArea` | `DefaultScreenArea` |
| `stateTracker` | `ColorStateTracker` | `ToleranceColorStateTracker`, `DefaultColorStateTracker`, `SimpleColorStateTracker` |

## Структура

```
src/main/java/ru/mkilord/colortomqttapp/
  core/screenshoter   снимок области экрана
  core/processor      обход пикселей
  core/detector       расчет цвета
  core/tracker        проверка, изменился ли цвет
  core/modifier       сдвиг H, S, B
  core/limit          ограничение диапазонов
  core/publisher      отправка в MQTT (Eclipse Paho)
  service             цикл обработки, настройки, превью
  controller          веб-интерфейс на Thymeleaf
```
