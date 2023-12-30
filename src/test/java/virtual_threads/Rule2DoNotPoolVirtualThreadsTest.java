package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Rule2DoNotPoolVirtualThreadsTest {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println(executorService); // java.util.concurrent.ThreadPerTaskExecutor@23941fb4

            Future<String> future = executorService.submit(() -> "alpha");
            future.get();
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        // don't do that
        try (var executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
            System.out.println(executorService); // java.util.concurrent.ThreadPoolExecutor@f68f0dc[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0]

            Future<String> future = executorService.submit(() -> "omega");
            future.get();
        }
    }
}
