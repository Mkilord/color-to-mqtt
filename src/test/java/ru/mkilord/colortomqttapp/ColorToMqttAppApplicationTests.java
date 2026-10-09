package ru.mkilord.colortomqttapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.storage-dir=target/test-storage")
class ColorToMqttAppApplicationTests {

    @Test
    void contextLoads() {
    }
}
