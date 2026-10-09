package ru.mkilord.colortomqttapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.mkilord.colortomqttapp.config.AppProperties;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.storage-dir=target/test-storage", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ColorToMqttAppApplicationTests {

    @Autowired
    AppProperties properties;

    /** Относительная папка считается от рабочей директории, а не от временной папки Tomcat. */
    @Test
    void storageIsRelativeToWorkingDirectory() {
        assertThat(properties.storagePath())
                .isEqualTo(Path.of("target/test-storage").toAbsolutePath().normalize());
        assertThat(properties.storagePath().resolve("settings.yaml")).exists();
    }
}
