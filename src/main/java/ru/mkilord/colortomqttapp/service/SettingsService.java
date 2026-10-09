package ru.mkilord.colortomqttapp.service;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Настройки приложения: общие для всех профилей (подключение к MQTT) и профили
 * со всем остальным. Активный профиль используется для захвата.
 */
public interface SettingsService {

    /** Общие для всех профилей ключи: подключение к брокеру. */
    Set<String> GLOBAL_KEYS = Set.of("broker", "topic", "username", "password");

    String DEFAULT_PROFILE = "Основной";

    /** Имена профилей по алфавиту. */
    List<String> profiles();

    String activeProfile();

    /** Действующие настройки активного профиля: по умолчанию, профиль, общие. */
    Properties loadOrElseLoadDefault();

    /** Действующие настройки указанного профиля. */
    Properties loadProfile(String name);

    Properties loadDefault();

    /**
     * Сохраняет настройки профиля. Общие ключи из {@code settings} пишутся в общий файл
     * и действуют для всех профилей.
     */
    void saveProfile(String name, Properties settings);

    void activate(String name);

    /**
     * Создает профиль копией {@code copyFrom}; если он null, с настройками по умолчанию.
     */
    void createProfile(String name, String copyFrom);

    void renameProfile(String from, String to);

    /** Удаляет профиль. Последний профиль удалить нельзя; вместо активного активируется другой. */
    void deleteProfile(String name);

    /** Возвращает профиль к настройкам по умолчанию, подключение к MQTT не меняется. */
    Properties resetProfile(String name);
}
