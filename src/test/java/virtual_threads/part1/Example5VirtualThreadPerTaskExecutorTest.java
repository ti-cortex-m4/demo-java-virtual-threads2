package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example5VirtualThreadPerTaskExecutorTest {

    @Test
    public void virtualThreadPerTaskExecutorTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertEquals("java.util.concurrent.ThreadPerTaskExecutor", executor.getClass().getName());

            Future<?> future = executor.submit(() -> System.out.println("run"));
            future.get();
        }
    }
}
