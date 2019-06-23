package net.adamcin.oakpal.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.adamcin.oakpal.core.Chart;
import net.adamcin.oakpal.core.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class LoaderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderTest.class);

    @Test
    public void testFindDefaultChartLocation() {
        Console console = mock(Console.class);
        when(console.getOakpalPath()).thenReturn(new File[0]);
        URL url = Loader.findChartLocation(console, null);
        assertNotNull("url should not be null", url);

        Result<Chart> chartResult = Chart.fromJson(url);
        assertFalse("chart should load successfully", chartResult.getError().isPresent());

        List<String> checklists = chartResult.map(Chart::getChecklists).getOrElse(Collections.emptyList());
        assertEquals("checklists should contain basic checklist",
                Collections.singletonList("net.adamcin.oakpal.core/basic"), checklists);
    }

    @Test
    public void testFindChartLocation() throws Exception {
        final Console console = mock(Console.class);
        final Map<String, String> env = new HashMap<>();
        env.put(Main.ENV_OAKPAL_PATH, "foo");
        when(console.getEnv()).thenReturn(env);
        when(console.getCwd()).thenReturn(new File("src/test/resources/charts"));
        doCallRealMethod().when(console).getOakpalPath();

        URL fooUrl = Loader.findChartLocation(console, null);
        assertEquals("foo chart url should be correct",
                new URL(console.getCwd().toURI().toURL(), "foo/oakpal-chart.json"), fooUrl);
    }
}
