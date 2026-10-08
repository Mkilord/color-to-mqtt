package ru.mkilord.colortomqttapp.core.processor;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@Setter
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class ChessProcessor implements Processor {

    public static final String CELL_SIZE_KEY = "cellSize";

    int frameSize;

    public ChessProcessor(Properties properties) {
        this.frameSize = Integer.parseInt(properties.getProperty(CELL_SIZE_KEY));
    }

    @Override
    public void process(int width, int height, PointConsumer action) {
        for (int y = 0; y < height; y += frameSize) {
            int startX = (y / frameSize) % 2 == 0 ? frameSize : 0;
            for (int x = startX; x < width; x += frameSize * 2) {
                action.accept(x, y);
            }
        }
    }
}
