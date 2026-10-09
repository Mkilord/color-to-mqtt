package ru.mkilord.colortomqttapp.domain.error;

public class ProfileNotFoundException extends DomainException {

    public ProfileNotFoundException(String name) {
        super("Профиль «" + name + "» не найден");
    }
}
