package ru.mkilord.colortomqttapp.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Версия и автор для подвала страниц. Версия берется из build-info, который пишет
 * spring-boot-maven-plugin при сборке; при запуске из IDE без сборки она неизвестна.
 */
@ControllerAdvice
public class AppInfoAdvice {

    public static final String AUTHOR = "mkilord";

    private final String version;

    public AppInfoAdvice(ObjectProvider<BuildProperties> buildProperties) {
        var build = buildProperties.getIfAvailable();
        this.version = build == null ? "dev" : build.getVersion();
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return version;
    }

    @ModelAttribute("appAuthor")
    public String appAuthor() {
        return AUTHOR;
    }
}
