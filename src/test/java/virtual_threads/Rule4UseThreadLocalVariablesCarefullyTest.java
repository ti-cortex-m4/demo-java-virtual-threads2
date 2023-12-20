package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    private static final ScopedValue<String> CONTEXT2 = ScopedValue.newInstance();

    @Test
    public void threadLocalVariablesTest() throws InterruptedException {
        assertNull(CONTEXT.get());

        CONTEXT.set("zero");
        assertEquals("zero", CONTEXT.get()); // unconstrained mutability

        CONTEXT.set("one");
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

    @Test
    public void scopedValuesTest() {
        assertThrows(NoSuchElementException.class,
            () -> {
                assertNull(CONTEXT2.get());
            });

        ScopedValue.where(CONTEXT2, "zero").run(
            new Runnable() {
                @Override
                public void run() {
                    assertEquals("zero", CONTEXT2.get());
                    ScopedValue.where(CONTEXT2, "one").run(
                        () -> {
                            assertEquals("one", CONTEXT2.get());
                        }
                    );
                    assertEquals("zero", CONTEXT2.get());

                    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                        Supplier<String> value1   = scope.fork(() -> {
                                assertEquals("zero", CONTEXT2.get());
                                return "a";
                            }
                        );
                        Supplier<String> value2   = scope.fork(() -> {
                                assertEquals("zero", CONTEXT2.get());
                                return "z";
                            }
                        );

                        scope.join().throwIfFailed();
                        assertEquals("a", value1.get());
                        assertEquals("z", value2.get());
                    } catch (Exception e) {
                        fail(e);
                    }
                }
            });

        assertThrows(NoSuchElementException.class,
            () -> {
                assertNull(CONTEXT2.get());
            });


//        Thread childThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                assertEquals("one", CONTEXT2.get()); // expensive inheritance
//            }
//        });
//        childThread.join();
    }

}
