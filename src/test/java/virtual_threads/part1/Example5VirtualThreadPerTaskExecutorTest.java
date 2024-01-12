package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Example5VirtualThreadPerTaskExecutorTest {

    @Test
    public void virtualThreadPerTaskExecutorTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

            Future<?> future = executorService.submit(() -> System.out.println("run"));
            future.get();
        }
    }
}
