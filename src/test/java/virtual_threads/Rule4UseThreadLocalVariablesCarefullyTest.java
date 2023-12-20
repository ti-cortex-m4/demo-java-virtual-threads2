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
        //create
        //modify in method - change
        //delete
        //inherit in child thread
    }

    private void doSomething() {
        CONTEXT.set("one");
        assertEquals("one", CONTEXT.get());
    }

//    private static final ScopedValue<String> NAME = ScopedValue.newInstance();

    @Test
    public void scopedValuesTest() {
//        System.out.println(NAME.get());
//        ScopedValue.where(NAME, "one");
//        System.out.println(NAME.get());

        //create
        //modify in method - do not change
        //delete ???
        //inherit in child thread ???
    }
}
