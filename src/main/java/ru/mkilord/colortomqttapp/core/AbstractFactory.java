package ru.mkilord.colortomqttapp.core;

import lombok.extern.log4j.Log4j2;

import java.util.Properties;

@SuppressWarnings("unchecked")
@Log4j2
public final class AbstractFactory<T> {
    public T get(String key, Properties config) {
        var className = config.getProperty(key);
        log.debug("Creating {}", key);
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class with name:" + className + " for the factory is missing or empty in the config.");
        }

        try {
            Class<?> clazz = Class.forName(className);
            try {
                var constructor = clazz.getDeclaredConstructor(Properties.class);
                return (T) constructor.newInstance(config);
            } catch (NoSuchMethodException e) {
                return (T) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + className, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load or instantiate the class", e);
        }
    }
}

