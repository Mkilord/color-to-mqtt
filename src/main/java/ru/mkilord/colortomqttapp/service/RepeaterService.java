package ru.mkilord.colortomqttapp.service;

public interface RepeaterService {
    void repeat(Runnable runnable);

    void stop();
}
