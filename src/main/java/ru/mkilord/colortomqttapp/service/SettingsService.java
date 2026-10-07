package ru.mkilord.colortomqttapp.service;

import java.io.IOException;
import java.util.Properties;

public interface SettingsService {

    void save(Properties editedProperties);

    Properties load() throws IOException;
    Properties loadOrElseLoadDefault();
    Properties loadDefault();

    /**
     * Удаляет сохраненные настройки и возвращает настройки по умолчанию.
     */
    Properties resetToDefaults();
}
