package ru.mkilord.colortomqttapp.core.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GridProcessorTest {

    @Test
    void visitsEveryCell() {
        var points = new ArrayList<List<Integer>>();
        new GridProcessor(10).process(30, 20, (x, y) -> points.add(List.of(x, y)));

        assertThat(points).containsExactly(
                List.of(0, 0), List.of(10, 0), List.of(20, 0),
                List.of(0, 10), List.of(10, 10), List.of(20, 10));
    }

    @Test
    void visitsTwiceAsManyPointsAsChess() {
        var grid = new int[1];
        var chess = new int[1];
        new GridProcessor(10).process(200, 200, (x, y) -> grid[0]++);
        new ChessProcessor(10).process(200, 200, (x, y) -> chess[0]++);

        assertThat(grid[0]).isEqualTo(chess[0] * 2);
    }
}
