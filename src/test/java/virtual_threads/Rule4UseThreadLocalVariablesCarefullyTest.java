package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    private final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @Test
    public void useThreadLocalVariable() throws InterruptedException {
        threadLocal.set("zero"); // mutability
        assertEquals("zero", threadLocal.get());

        threadLocal.set("one");
        assertEquals("one", threadLocal.get());

        Thread childThread = new Thread(() -> {
            assertEquals("one", threadLocal.get()); // expensive inheritance
        });
        childThread.join();

        threadLocal.remove();
        assertNull(threadLocal.get()); // unbounded lifetime
    }


    private final ScopedValue<String> scopedValue = ScopedValue.newInstance();

    @Test
    public void useScopedValue() throws InterruptedException {
        ScopedValue.where(scopedValue, "zero").run(
            () -> {
                assertEquals("zero", scopedValue.get());

                ScopedValue.where(scopedValue, "one").run(
                    () -> assertEquals("one", scopedValue.get()) // bounded lifetime
                );
                assertEquals("zero", scopedValue.get());

                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    Supplier<String> value = scope.fork(() -> {
                            assertEquals("zero", scopedValue.get()); // cheap inheritance
                            return null;
                        }
                    );
                    scope.join().throwIfFailed();
                    assertNull(value.get());
                } catch (InterruptedException | ExecutionException e) {
                    fail(e);
                }
            }
        );

        assertThrows(NoSuchElementException.class, () -> assertNull(scopedValue.get())); // bounded lifetime
    }
}
