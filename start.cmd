@echo off
chcp 65001 >nul
rem Запуск ColorToMQTT одним процессом и с ограничением памяти.
rem Собирает jar, если его нет. "start.cmd build" пересобирает после обновления кода (нужен Maven).
cd /d "%~dp0"

set JAR=target\color-to-mqtt.jar
if "%~1"=="build" goto build
if not exist "%JAR%" goto build
goto run

:build
echo Сборка %JAR%...
call mvn -q -DskipTests package || exit /b 1

:run
java -Xms16m -Xmx128m -XX:+UseSerialGC -Xss512k -jar "%JAR%"
