package virtual_threads;

import org.junit.jupiter.api.Test;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    @Test
    public void threadLocalVariablesTest() {
        System.out.println(CONTEXT.get());
        CONTEXT.set("one");
        System.out.println(CONTEXT.get());
        CONTEXT.remove();

        //create
        //modify in method - change
        //delete
        //inherit in child thread
    }

    private static final ScopedValue<String> NAME = ScopedValue.newInstance();

    @Test
    public void scopedValuesTest() {
        System.out.println(NAME.get());
        ScopedValue.where(NAME, "one");
        System.out.println(NAME.get());

        //create
        //modify in method - do not change
        //delete ???
        //inherit in child thread ???
    }
}
