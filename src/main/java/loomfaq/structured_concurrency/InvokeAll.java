package loomfaq.structured_concurrency;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InvokeAll {

  public static void main(String[] args) throws Exception {
    System.out.println(new InvokeAll().invokeAll());
  }

  public long invokeAll() throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var futures = subTasks().map(scope::fork).toList();
      scope.join();
      scope.throwIfFailed();
      return futures.stream().map(StructuredTaskScope.Subtask::get).reduce(0, Integer::sum);
    }
  }

  private Stream<Callable<Integer>> subTasks() {
    return IntStream.range(0, 10_000)
        .mapToObj(
            i ->
                () -> {
                  try {
                    Thread.sleep(Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3)));
                  } catch (InterruptedException e) {
                    // ignore
                  }
                  return i;
                });
  }
}
