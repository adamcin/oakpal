package net.adamcin.oakpal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class OpearFileTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpearFileTest.class);

    @Test
    public void testFindDefaultPlanLocation() {
        Result<OpearFile> opearResult = OpearFile.fromDirectory(new File("src/test/resources/plans/bar"));
        assertFalse("opearResult should not be a failure", opearResult.isFailure());
        Result<URL> urlResult = opearResult.map(Opear::getDefaultPlan);
        assertFalse("urlResult should not be a failure", urlResult.isFailure());

        Result<OakpalPlan> planResult = urlResult.flatMap(OakpalPlan::fromJson);
        assertFalse("plan should load successfully", planResult.isFailure());

        List<String> checklists = planResult.map(OakpalPlan::getChecklists).getOrElse(Collections.emptyList());
        assertEquals("checklists should contain test/bar checklist",
                Collections.singletonList("test/bar"), checklists);

        Result<OpearFile> notAPlanResult = OpearFile.fromDirectory(new File("src/test/resources/plans/none"));
        assertFalse("notAPlanResult should not be a failure", notAPlanResult.isFailure());
        Result<URL> notAPlanUrlResult = notAPlanResult.map(Opear::getDefaultPlan);
        assertFalse("notAPlanUrlResult should not be a failure", notAPlanUrlResult.isFailure());

        Result<OakpalPlan> notAPlanPlanResult = notAPlanUrlResult.flatMap(OakpalPlan::fromJson);
        assertFalse("notAPlanPlan should load successfully", notAPlanPlanResult.isFailure());

        List<String> notAPlanChecklists = notAPlanPlanResult.map(OakpalPlan::getChecklists).getOrElse(Collections.emptyList());
        assertEquals("notAPlanChecklists should contain no checklists",
                Collections.singletonList("net.adamcin.oakpal.core/basic"), notAPlanChecklists);

    }

    @Test
    public void testFindPlanLocation() throws Exception {
        final File fooDir = new File("src/test/resources/plans/foo");
        Result<OpearFile> opearResult = OpearFile.fromDirectory(fooDir);

        Result<URL> fooUrlResult = opearResult.flatMap(opear -> opear.getSpecificPlan("other-plan.json"));
        assertEquals("foo plan url should be correct",
                new URL(fooDir.toURI().toURL(), "other-plan.json"),
                fooUrlResult.getOrElse(OakpalPlan.BASIC_PLAN_URL));

        Result<URL> foo2UrlResult = opearResult.flatMap(opear -> opear.getSpecificPlan("no-plan.json"));
        assertTrue("foo2 plan url should be failure", foo2UrlResult.isFailure());
    }
}
