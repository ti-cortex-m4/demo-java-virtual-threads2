package virtual_threads.part2;

import org.junit.jupiter.api.Test;
import virtual_threads.AbstractTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Rule2DoNotPoolVirtualThreadsTest extends AbstractTest {

    @Test
    public void doTest() {
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println(executorService); // java.util.concurrent.ThreadPerTaskExecutor@23941fb4

            executorService.submit(() -> { sleep(1000); System.out.println("alpha"); });
        }
    }

    @Test
    public void doNotTest() {
        try (var executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
            System.out.println(executorService); // java.util.concurrent.ThreadPoolExecutor@f68f0dc[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0]

            executorService.submit(() -> { sleep(1000); System.out.println("omega"); });
        }
    }
}
