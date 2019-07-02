package net.adamcin.oakpal.core;

import static net.adamcin.oakpal.core.Fun.result1;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class ResultTest {

    @Test
    public void testCollectOrFailOnFirst() {
        List<Result<Integer>> rawValues = Stream.of(0, 1, 2, 3, 4, 5, 6)
                .map(result1(value -> value / (value % 3)))
                .collect(Collectors.toList());

        Result<List<Integer>> allAreGood = rawValues.stream().collect(Result.tryCollect(Collectors.toList()));

        assertTrue("allAreGood is a failure", allAreGood.isFailure());
        assertTrue("failure is an " + allAreGood.getError().get().getClass().getName(),
                allAreGood.findCause(ArithmeticException.class::isInstance).isPresent());
    }
}