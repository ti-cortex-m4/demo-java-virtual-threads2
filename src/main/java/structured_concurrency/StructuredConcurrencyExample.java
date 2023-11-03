package structured_concurrency;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;


public class StructuredConcurrencyExample {

  public static void main(String[] args) throws Exception {
    System.out.println( new StructuredConcurrencyExample().calculate());
  }

  public int calculate() throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      StructuredTaskScope.Subtask<Integer> v1 = scope.fork(this::op1);
      int v2 = calculateInner();
      scope.join();
      scope.throwIfFailed();
      return v1.get() * v2;
    }
  }

  private int calculateInner() throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      StructuredTaskScope.Subtask<Integer> v21 = scope.fork(this::op21);
      StructuredTaskScope.Subtask<Integer> v22 = scope.fork(this::op22);
      scope.join();
      scope.throwIfFailed();
      return v21.get() + v22.get();
    }
  }

  private int op1() {
    System.out.println("Operation 1 starts");
    try {
      Thread.sleep(Duration.ofSeconds(3));
    } catch (InterruptedException e) {
      // ignored
    }
    System.out.println("Operation 1 finishes");
    return 1;
  }

  private int op21() {
    System.out.println("Operation 2.1 starts");
    try {
      Thread.sleep(Duration.ofSeconds(4));
    } catch (InterruptedException e) {
      // ignored
    }
    System.out.println("Operation 2.1 finishes");
    return 3;
  }

  private int op22() {
    System.out.println("Operation 2.2 starts");
    try {
      Thread.sleep(Duration.ofSeconds(2));
    } catch (InterruptedException e) {
      // ignored
    }
    System.out.println("Operation 2.2 finishes");
    return 3;
  }
}