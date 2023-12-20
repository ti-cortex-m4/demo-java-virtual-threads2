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

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    @Test
    public void threadLocalVariablesTest() throws InterruptedException {
        THREAD_LOCAL.set("zero");
        assertEquals("zero", THREAD_LOCAL.get());

        THREAD_LOCAL.set("one");
        assertEquals("one", THREAD_LOCAL.get()); // unconstrained mutability

        Thread childThread = new Thread(() -> {
            assertEquals("one", THREAD_LOCAL.get()); // expensive inheritance
        });
        childThread.join();

        THREAD_LOCAL.remove();
        assertNull(THREAD_LOCAL.get()); // unbounded lifetime
    }

    private static final ScopedValue<String> SCOPED_VALUE = ScopedValue.newInstance();

    @Test
    public void scopedValuesTest() {
        ScopedValue.where(SCOPED_VALUE, "zero").run(
            () -> {
                assertEquals("zero", SCOPED_VALUE.get()); // immutability

                ScopedValue.where(SCOPED_VALUE, "one").run(
                    () -> assertEquals("one", SCOPED_VALUE.get())
                );
                assertEquals("zero", SCOPED_VALUE.get()); // bounded lifetime

                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    Supplier<String> value = scope.fork(() -> {
                            assertEquals("zero", SCOPED_VALUE.get()); // cheap inheritance
                            return "two";
                        }
                    );
                    scope.join().throwIfFailed();
                    assertEquals("two", value.get());
                } catch (Exception e) {
                    fail(e);
                }
            }
        );

        assertThrows(NoSuchElementException.class, () -> assertNull(SCOPED_VALUE.get())); // bounded lifetime
    }
}
