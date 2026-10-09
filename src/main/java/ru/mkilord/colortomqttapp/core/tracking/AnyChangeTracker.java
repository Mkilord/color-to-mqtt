package ru.mkilord.colortomqttapp.core.tracking;

import java.awt.Color;

public final class AnyChangeTracker extends ColorChangeTracker {

    @Override
    protected boolean differs(Color previous, Color next) {
        return !previous.equals(next);
    }
}
