package ru.mkilord.colortomqttapp.core.correction;

public enum ColorZone {
    /** Темнее порога: лампы гаснут. */
    BLACK,
    /** Бледнее порога: тон случаен, уходит белый. */
    GRAY,
    COLOR
}
