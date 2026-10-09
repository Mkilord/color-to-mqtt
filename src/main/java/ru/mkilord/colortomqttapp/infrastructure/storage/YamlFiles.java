package ru.mkilord.colortomqttapp.infrastructure.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Чтение и атомарная запись YAML, сравнение и наложение деревьев для хранения отличий.
 */
final class YamlFiles {

    private YamlFiles() {
    }

    static JsonNode read(ObjectMapper yaml, Path file) {
        try {
            var node = yaml.readTree(file.toFile());
            return node == null || node.isMissingNode() ? yaml.createObjectNode() : node;
        } catch (IOException e) {
            throw new StorageException("Не удалось прочитать " + file, e);
        }
    }

    /** Пишет во временный файл и подменяет: при сбое старый файл остается целым. */
    static void write(ObjectMapper yaml, Path file, JsonNode content) {
        try {
            Files.createDirectories(file.getParent());
            var temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            yaml.writeValue(temp.toFile(), content);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Не удалось сохранить " + file, e);
        }
    }

    /** Только то, что в {@code value} отличается от {@code base}. */
    static ObjectNode diff(ObjectNode value, ObjectNode base) {
        var result = value.objectNode();
        value.properties().forEach(entry -> {
            var key = entry.getKey();
            var mine = entry.getValue();
            var theirs = base.get(key);
            if (mine.isObject() && theirs != null && theirs.isObject()) {
                var nested = diff((ObjectNode) mine, (ObjectNode) theirs);
                if (!nested.isEmpty()) {
                    result.set(key, nested);
                }
            } else if (!same(mine, theirs)) {
                result.set(key, mine);
            }
        });
        return result;
    }

    /** {@code base} с наложенными поверх значениями {@code overrides}. */
    static ObjectNode merge(ObjectNode base, JsonNode overrides) {
        var result = base.deepCopy();
        if (overrides == null || !overrides.isObject()) {
            return result;
        }
        overrides.properties().forEach(entry -> {
            var current = result.get(entry.getKey());
            if (current != null && current.isObject() && entry.getValue().isObject()) {
                result.set(entry.getKey(), merge((ObjectNode) current, entry.getValue()));
            } else {
                result.set(entry.getKey(), entry.getValue());
            }
        });
        return result;
    }

    /** 0.2 и 0.2f после YAML могут прийти разными типами: числа сравниваются по значению. */
    private static boolean same(JsonNode a, JsonNode b) {
        if (b == null) {
            return false;
        }
        if (a.isNumber() && b.isNumber()) {
            return Float.compare(a.floatValue(), b.floatValue()) == 0;
        }
        return a.equals(b);
    }
}
