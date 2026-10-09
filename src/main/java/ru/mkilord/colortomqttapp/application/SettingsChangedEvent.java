package ru.mkilord.colortomqttapp.application;

/**
 * Изменились настройки, с которыми идет захват: активный профиль или подключение.
 */
public record SettingsChangedEvent(String reason) {
}
