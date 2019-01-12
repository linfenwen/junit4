package org.junit.tests.running.classes;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class ThreadsTest {

    private List<Boolean> interruptedFlags = new CopyOnWriteArrayList<Boolean>();
    private JUnitCore core = new JUnitCore();

    public static class TestWithInterrupt {

        @Test
        public void interruptCurrentThread() {
            Thread.currentThread().interrupt();
        }

        @Test
        public void otherTestCaseInterruptingCurrentThread() {
            Thread.currentThread().interrupt();
        }

    }

    @Test
    public void currentThreadInterruptedStatusIsClearedAfterEachTestExecution() {
        core.addListener(new RunListener() {
            @Override
            public void testFinished(Description description) {
                interruptedFlags.add(Thread.currentThread().isInterrupted());
            }
        });

        Result result = core.run(TestWithInterrupt.class);

        assertEquals(0, result.getFailureCount());
        assertEquals(asList(false, false), interruptedFlags);
    }

    @RunWith(BlockJUnit4ClassRunner.class)
    public static class TestWithInterruptFromAfterClass {
        @AfterClass
        public static void interruptCurrentThread() {
            Thread.currentThread().interrupt();
        }

        @Test
        public void test() {
            // no-op
        }
    }

    @Test
    public void currentThreadInterruptStatusIsClearedAfterSuiteExecution() {
        core.addListener(new RunListener() {
            @Override
            public void testSuiteFinished(Description description) {
                interruptedFlags.add(Thread.currentThread().isInterrupted());
            }
        });

        Request request = new Request() {
            @Override
            public Runner getRunner() {
                try {
                    return new BlockJUnit4ClassRunner(TestWithInterruptFromAfterClass.class) {
                    };
                } catch (InitializationError e) {
                    return new ErrorReportingRunner(TestWithInterruptFromAfterClass.class, e);
                }
            }
        };

        Result result = core.run(request);

        assertEquals(0, result.getFailureCount());
        assertEquals(singletonList(false), interruptedFlags);
    }
}
