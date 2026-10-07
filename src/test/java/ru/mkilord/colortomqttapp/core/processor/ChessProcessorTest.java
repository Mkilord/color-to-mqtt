package ru.mkilord.colortomqttapp.core.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChessProcessorTest {

    @Test
    void visitsCellsInCheckerboardOrder() {
        var points = new ArrayList<List<Integer>>();

        new ChessProcessor(10).process(40, 20, (x, y) -> points.add(List.of(x, y)));

        assertThat(points).containsExactly(
                List.of(10, 0), List.of(30, 0),
                List.of(0, 10), List.of(20, 10));
    }

    @Test
    void visitsNothingWhenAreaIsSmallerThanCell() {
        var points = new ArrayList<List<Integer>>();

        new ChessProcessor(20).process(5, 5, (x, y) -> points.add(List.of(x, y)));

        assertThat(points).isEmpty();
    }
}
