package virtual_threads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    @Test
    public void threadLocalVariablesTest() throws InterruptedException {
        assertNull(CONTEXT.get());

        CONTEXT.set("zero");
        assertEquals("zero", CONTEXT.get()); // unconstrained mutability

        doSomething();
        assertEquals("one", CONTEXT.get()); // unbounded lifetime

        Thread childThread = new Thread(new Runnable() {
            @Override
            public void run() {
                assertEquals("one", CONTEXT.get()); // expensive inheritance
            }
        });
        childThread.join();

        CONTEXT.remove();
        assertNull(CONTEXT.get());
    }

    private void doSomething() {
        CONTEXT.set("one");
        assertEquals("one", CONTEXT.get());
    }

    private static final ScopedValue<String> CONTEXT2 = ScopedValue.newInstance();

    @Test
    public void scopedValuesTest() {
        assertNull(CONTEXT2.get());

        CONTEXT2.set("zero");
        assertEquals("zero", CONTEXT2.get()); // unconstrained mutability

        doSomething2();
        assertEquals("one", CONTEXT2.get()); // unbounded lifetime

        Thread childThread = new Thread(new Runnable() {
            @Override
            public void run() {
                assertEquals("one", CONTEXT2.get()); // expensive inheritance
            }
        });
        childThread.join();

        CONTEXT2.remove();
        assertNull(CONTEXT2.get());
    }

    private void doSomething2() {
        CONTEXT2.set("one");
        assertEquals("one", CONTEXT2.get());
    }
}
