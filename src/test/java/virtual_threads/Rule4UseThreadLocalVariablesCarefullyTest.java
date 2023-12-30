package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    private final InheritableThreadLocal<String> threadLocal = new InheritableThreadLocal<>();

    @Test
    public void useThreadLocalVariable() throws InterruptedException {
        threadLocal.set("zero");
        assertEquals("zero", threadLocal.get());

        threadLocal.set("one");
        assertEquals("one", threadLocal.get());

        Thread childThread = new Thread(() -> {
            System.out.println(threadLocal.get()); // "one"
        });
        childThread.start();
        childThread.join();

        threadLocal.remove();
        assertNull(threadLocal.get());
    }


    private final ScopedValue<String> scopedValue = ScopedValue.newInstance();

    @Test
    public void useScopedValue() {
        ScopedValue.where(scopedValue, "zero").run(
            () -> {
                assertEquals("zero", scopedValue.get());

                ScopedValue.where(scopedValue, "one").run(
                    () -> assertEquals("one", scopedValue.get())
                );
                assertEquals("zero", scopedValue.get());

                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    scope.fork(() -> {
                            System.out.println(scopedValue.get()); // "zero"
                            return null;
                        }
                    );
                    scope.join().throwIfFailed();
                } catch (InterruptedException | ExecutionException e) {
                    fail(e);
                }
            }
        );

        assertThrows(NoSuchElementException.class, () -> assertNull(scopedValue.get()));
    }
}
