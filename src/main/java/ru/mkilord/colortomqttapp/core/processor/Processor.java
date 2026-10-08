package ru.mkilord.colortomqttapp.core.processor;

public interface Processor {
    String PROCESSOR_KEY = "processor";

    void process(int width, int height, PointConsumer action);
}
