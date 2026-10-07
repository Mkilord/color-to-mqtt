# ColorToMQTTApp

[![CI](https://github.com/Mkilord/ColorToMQTTApp/actions/workflows/ci.yml/badge.svg)](https://github.com/Mkilord/ColorToMQTTApp/actions/workflows/ci.yml)

Spring Boot приложение, которое следит за цветом в центре экрана и отправляет его в MQTT. Подходит для подсветки в умном доме: лампа повторяет цвет того, что сейчас на экране.

## Как работает

После запуска приложение повторяет цикл с паузой `updatePeriod` мс между кадрами:

1. Снимает область в центре экрана размером `screenWight` x `screenHeight` через `java.awt.Robot`. Если область больше экрана, она обрезается.
2. Считает средний цвет по пикселям в шахматном порядке, шаг сетки `cellSize`.
3. Сравнивает его с последним отправленным цветом. По умолчанию сравнение идет в HSB: если разница по тону, насыщенности или яркости больше допуска, цвет считается новым. Тон сравнивается по кругу.
4. Сдвигает H, S, B на заданные значения (тон по кругу) и ограничивает их диапазонами `min*` и `max*`.
5. Публикует цвет в топик MQTT с QoS 0. Соединение с брокером одно на весь сеанс; при обрыве клиент переподключается сам, а кадры на время обрыва пропускаются.

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

Веб-интерфейс откроется на http://localhost:8080. На главной странице кнопки запуска и остановки и текущий цвет. На странице `/settings` редактируются все параметры захвата и показывается превью области захвата.

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
| `updatePeriod` | Пауза между кадрами, мс |
| `cellSize` | Шаг сетки при подсчете среднего цвета, px |
| `stateTracker` | Способ сравнения цветов: по допускам HSB, по расстоянию в RGB или любое изменение |
| `hueTolerance`, `saturationTolerance`, `brightnessTolerance` | Допуск изменения цвета, доли 0..1 |
| `sensitivity` | Порог для сравнения в RGB, % |
| `modifyHue`, `modifySaturation`, `modifyBrightness` | Сдвиг H, S, B перед отправкой |
| `minHUE`, `maxHUE`, `minSaturation`, `maxSaturation`, `minBrightness`, `maxBrightness` | Допустимые диапазоны после сдвига |

Настройки, сохраненные на странице `/settings`, проверяются, пишутся в файл `settings.txt` в рабочей директории (формат Java Properties) и применяются поверх `app.defaultSettings`. Если захват запущен, он перезапускается с новыми параметрами. Кнопка сброса удаляет `settings.txt` и возвращает значения по умолчанию. Логин и пароль MQTT в файл не сохраняются и всегда берутся из переменных окружения. Путь к файлу меняется свойством `app.settings-file`.

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

## Тесты

```bash
mvn verify
```

Unit-тесты покрывают обработку кадра, расчет цвета, трекеры, сдвиг и ограничения, формат сообщения MQTT, сохранение настроек и страницу настроек. GitHub Actions запускает сборку и тесты на каждый push.
