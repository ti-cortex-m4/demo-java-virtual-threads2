<!-----



Conversion time: 1.012 seconds.


Using this Markdown file:

1. Paste this output into your source file.
2. See the notes and action items below regarding this conversion run.
3. Check the rendered output (headings, lists, code blocks, tables) for proper
   formatting and use a linkchecker before you publish this page.

Conversion notes:

* Docs to Markdown version 1.0β35
* Mon Dec 18 2023 11:03:42 GMT-0800 (PST)
* Source doc: Google Translate
* This is a partial selection. Check to make sure intra-doc links work.
----->



#### Creating virtual threads

The constructors of the _Thread_ class and its subclasses can only create platform threads.The sealed _Thread.Builder_ interface provides greater ability to create threads than the constructors. The _Thread.Builder.OfPlatform_ subinterface could create platform threads, and the _Thread.Builder.OfVirtual_ subinterface could create virtual threads.


###### Creating platform threads from constructors

The following example creates a platform thread from a constructor.


```
Thread thread = new Thread(() -> System.out.println("run!"));
assertTrue(thread.isPlatform());
assertFalse(thread.isVirtual());
thread.join();
```



###### Creating platform and virtual threads from builders

The following example creates a platform thread from an implementation of the _Thread.Builder.OfPlatform_ interface that is returned from the _Thread.ofPlatform()_ method.


```
Thread thread = Thread.ofPlatform().start(() -> System.out.println("run"));
assertTrue(thread.isPlatform());
thread.join();
```


The following example creates a virtual thread from an implementation of the _Thread.Builder.ofVirtual_ interface that is returned from the _Thread.ofVirtual()_ method.


```
Thread thread = Thread.ofVirtual().start(() -> System.out.println("run"));
assertTrue(thread.isVirtual());
thread.join();
```


The following example creates a virtual thread and implicitly starts it in the builder.


```
Thread.Builder builder = Thread.ofVirtual();
Thread thread = builder.start(() -> { sleep(1000); System.out.println("run"); });
assertEquals(Thread.State.RUNNABLE, thread.getState());
thread.join();
```


The following example creates a virtual thread and then explicitly starts it.


```
Thread.Builder builder = Thread.ofVirtual();
Thread thread = builder.unstarted(() -> System.out.println("run"));
assertEquals(Thread.State.NEW, thread.getState());
thread.start();
assertEquals(Thread.State.RUNNABLE, thread.getState());
thread.join();
```


The following example creates a platform thread with all the options available in the _Thread.Builder.ofPlatform_ thread builder interface.


```
Thread.Builder builder = Thread.ofPlatform()
   .group(Thread.currentThread().getThreadGroup())
   .daemon(false)
   .priority(10)
   .stackSize(1024)
   .name("platform thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
Thread thread = builder.unstarted(() -> System.out.println("run"));

assertEquals("platform thread", thread.getName());
assertEquals("main", thread.getThreadGroup().getName());
assertFalse(thread.isDaemon());
assertEquals(10, thread.getPriority());
```


The following example creates a virtual thread with all the options available in the _Thread.Builder.OfVirtual_ thread builder interface.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
Thread thread = builder.unstarted(() -> System.out.println("run"));

assertEquals("virtual thread", thread.getName());
assertEquals("VirtualThreads", thread.getThreadGroup().getName());
assertTrue(thread.isDaemon());
assertEquals(5, thread.getPriority());
```


The previous example shows that the following parameters cannot be specified when creating virtual threads:



* thread group (always "VirtualThreads")
* daemon (always _true_)
* priority(always 5)
* stack size


###### Creating virtual threads from thread factory

Builders are the best alternative to constructors (see "Effective Java" 3rd edition by Joshua Bloch, items 1, 2). Interface _Thread.Builder_ was added in Java 21 and is not yet widely used. Much wider use is made of the _ThreadFactory_ interface, which was added in Java 1.5. The _Thread.Builder.factory_ interface method returns a thread-safe _ThreadFactory_ instance to create threads from the current state of the builder.

The following example creates a virtual thread from a factory created from the current state of the builder.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread");
ThreadFactory threadFactory = builder.factory();
Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertEquals("virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());
```



### How to properly use virtual threads

Virtual threads were created to increase parallelism (the number of tasks running at the same time) when multicore CPUs became ubiquitous (around 2000). Virtual threads almost fully support the API and semantics of the _Thread_ class. Virtual threads shift the objective of processing blocking threads from a programmer to the Java runtime. But to get a real parallelism performance boost, the programmer must know the details of their implementations (or at least the rules of thumb when to use virtual threads and when not).


###### Do not use virtual threads for CPU-intensive tasks


###### Do not use virtual threads if their number is less than 10000

As a rule of thumb, if your application never has 10,000 virtual threads or more, it is unlikely to benefit from virtual threads. Either it experiences too light a load to need better throughput, or you have not represented sufficiently many tasks to virtual threads.


###### Write blocking synchronous code in the thread-per-request style

Blocking a platform thread needlessly keeps the OS thread (a relatively scarce resource) from doing useful work. Therefore non-blocking, asynchronous frameworks (futures/promises, reactive streams, etc.) have been developed that reduce the number of thread blockings and increase the use of CPU resources. But their disadvantage is a more complex concurrent model, which makes it more difficult for programmers to create and maintain such code. Code written in non-blocking, asynchronous frameworks (), which use their own OS thread blocking avoidance, will not gain much benefit from using virtual threads.

In contrast, blocking a virtual thread is cheap and encouraged. It allows virtual threads to write blocking synchronous code in a simple thread-per-request style. This allows developers to create simpler but effective  concurrent code.

The following non-blocking, asynchronous code won't get much benefit from using virtual threads, because the framework already manages blocking OS threads.


```
CompletableFuture.supplyAsync(this::getPriceInEur)
.thenCombine(CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate)
.thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount))))
.whenComplete((grossAmountInUsd, t) -> {
if (t == null) {
assertEquals(165, grossAmountInUsd);
} else {
fail(t);
}
})
.get();
```


The following blocking, synchronous code also won't get much benefit from using virtual threads, because long-running tasks are executed sequentially in a single virtual thread.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
Future<Float> future = executorService.submit(() -> {
int priceInEur = getPriceInEur();
float netAmountInUsd = priceInEur * getExchangeRateEurToUsd();
float tax = getTax(netAmountInUsd);
return netAmountInUsd * (1 + tax);
});

float grossAmountInUsd = future.get();
assertEquals(165, grossAmountInUsd);
}
```


The following blocking, asynchronous code will benefit from using virtual threads, because long-running tasks are executed in parallel in separate virtual threads.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
Future<Integer> priceInEur = executorService.submit(this::getPriceInEur);
Future<Float> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd);
float netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get();

Future<Float> tax = executorService.submit(() -> getTax(netAmountInUsd));
float grossAmountInUsd = netAmountInUsd * (1 + tax.get());

assertEquals(165, grossAmountInUsd);
}
```



###### Do not pool virtual threads

Creating (and destroying) a platform thread is a rather lengthy process, since it requires the creation of an OS thread. Thread pools executors are designed to reuse threads between tasks and reduce this time. They contain a pool of pre-created worker threads, to which _Runnable_ and _Callble_ tasks are submitted through a _Queue_. Worker threads are created when the executor is created (and, if necessary, during its operation) and will not be destroyed and later recreated after the completion of each task.

Unlike platform threads, creating virtual threads is a fast process. Therefore, there is no need to pool virtual threads. If the program logic requires the use of an _ExecutorService_, use an executor specially designed for virtual threads _Executors.newVirtualThreadPerTaskExecutor_. This executor doesn't employ a thread pool and creates a new virtual thread for each submitted task. Moreover, this executor service itself is lightweightand you can create and close it in every required place inside the _try-with-resources_ block. The _close()_ method of the _AutoCloseable_ interface, which is implicitly called at the end of a _try-with-resources _block, automatically waits for all tasks submitted to the _ExecutorService_ to complete.

The following code incorrectly uses the thread pool executor to reuse virtual threads between tasks:


```
try (ExecutorService executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
…
}
```


The following code correctly uses a _thread-per-request_ virtual executor to create a new thread for each task:


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
…
}
```



###### Use semaphores instead of fixed thread pools to limit concurrency

The _main_ objective of thread pools is to reuse threads; they can be for 1 worker thread, several worker threads, or an unlimited number of worker threads. (Depending on the configuration of the thread pool, the number of worket threads may expand or decrease). When submitting tasks to a thread pool, they are placed in a queue, from which they are retrieved by the worker threads (see _ThreadPoolExecutor#workQueue_). 

An _additional _objective when using thread pools with a fixed number of workert threads is that they can be used  to limit the concurrency of a certain operation. (For example, some external services may not be able to handle more than N concurrent requests). But since there is no need to reuse virtual threads, instead of thread pools with a fixed number of worker threads, it is better to use [semaphores](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Semaphore.html) with the same number of permits to limit concurrency. Semaphores also contain a queue inside themselves, but not of tasks, but of threads that are blocked on it (see _AbstractQueuedSynchronizer#head_).

The following code, which uses a fixed thread pool to limit the concurrency of accessing somes to some shared service, won’t benefit from the use of virtual threads:


```
private final ExecutorService executorService = Executors.newFixedThreadPool(10);

public Object useFixedExecutorServiceToLimitConcurrency() throws ExecutionException, InterruptedException {
Future<Object> future = executorService.submit(() -> sharedResource());
return future.get();
}
```


The following code, which uses a semaphore to limit the concurrency of accessing somes to some shared service, will benefit from the use of virtual threads:


```
private final Semaphore semaphore = new Semaphore(10);

public Object useSemaphoreToLimitConcurrency() throws InterruptedException {
semaphore.acquire();
try {
return sharedResource();
} finally {
semaphore.release();
}
}
```



###### Use thread-local variables carefully or switch to scoped values

To achieve better scalability of virtual threads, you should reconsider using _thread-local variables_ and _inheritable thread-local variables_. Thread-local variables provide each thread with its own copy of a variable. A thread-local variable works as an implicit, thread-bound parameter and allows passing data from a caller to a callee through a sequence of intermediate methods.

Virtual threads support thread-local behavior in the same way as platform threads. But because virtual threads can be very numerous, negative design features of local variables can be applied much more significantly:



* unconstrained mutability (any code that can call the _get_ method of a thread-local variable can call the _set_ method of that variable, even if an object in a thread-local variable is immutable)
* unbounded lifetime (once a copy of a thread-local variable is set via the _set_ method, the value is retained for the lifetime of the thread, or until code in the thread calls the _remove_ method)
* expensive inheritance (each child thread copies, not reuses, thread-local variables of a parent thread)

[Scoped values](https://openjdk.org/jeps/446) (are a preview feature in Java 21) may be a better alternative to thread-local variables, especially when using large numbers of virtual threads. Unlike a thread-local variable, a scoped value is written once, is available only for a bounded context, and is inherited in a _structured concurrency_ scope.

The following code shows that a thread-local variable is mutable, inherited in a child thread started from the parent thread, and exists until it is removed.


```
private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

@Test
public void threadLocalVariableTest() throws InterruptedException {
THREAD_LOCAL.set("zero"); // mutability
assertEquals("zero", THREAD_LOCAL.get());

THREAD_LOCAL.set("one");
assertEquals("one", THREAD_LOCAL.get());

Thread childThread = new Thread(() -> {
assertEquals("one", THREAD_LOCAL.get()); // expensive inheritance
});
childThread.join();

THREAD_LOCAL.remove();
assertNull(THREAD_LOCAL.get()); // unbounded lifetime
}
```


The following code shows that a scoped value is immutable, is reused in a structured concurrency scope, and exists only in a bounded context.


```
private static final ScopedValue<String> SCOPED_VALUE = ScopedValue.newInstance();

 @Test
 public void scopedValueTest() {
       ScopedValue.where(SCOPED_VALUE, "zero").run(
     () -> {
          assertEquals("zero", SCOPED_VALUE.get()); // immutability

           ScopedValue.where(SCOPED_VALUE, "one").run(
               () -> assertEquals("one", SCOPED_VALUE.get()) // bounded lifetime
           );
           assertEquals("zero", SCOPED_VALUE.get());

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
```



###### Use synchronized blocks and methods carefully or switch to reentrant locks

To achieve better scalability of virtual threads, you should revise _synchronized_ blocks and methods, especially lengthy and frequent ones (that often contain I/O operations). As an althernative, you should replace them with _ReentrantLock_ that also guarantees sequential access to a resource.

When a virtual thread performs a blocking operation inside a _synchronized_ block or method, this does not release the OS thread. This situation is called _pinning_, and it contradicts the ideology of virtual threads, according to which one OS thread controls many virtual threads. Pinning is not a problem if such operations are short-lived (such as in-memory operations) even if they are frequent. Pinning is also not a problem if such operations are long-term (such as IO operations) but infrequent. Pinning is a problem if such operations are both lengthy and frequent. To get rid of pinning, you should replace _synchronized_ blocks and methods with _ReentrantLock_ that also guarantees mutually exclusive access to a resource.

The following code uses a _synchronized_ block with an explicit object lock.


```
private final Object lockObject = new Object();

public void useSynchronizedBlock() {
   synchronized (lockObject) {
       exclusiveResource();
   }
}
```


The following code uses a reentrant lock.


```
private final ReentrantLock reentrantLock = new ReentrantLock();

public void useReentrantLock() {
   reentrantLock.lock();
   try {
       exclusiveResource();
   } finally {
       reentrantLock.unlock();
   }
}
```


To detect the instances of pinning that might be harmful, JDK Flight Recorder (JFR) emits the jdk.VirtualThreadPinned thread when a blocking operation is pinned; by default this event is enabled when the operation takes longer than 20ms.

Alternatively, you can use the system property jdk.tracePinnedThreads to emit a stack trace when a thread blocks while pinned. Running with the option -Djdk.tracePinnedThreads=full prints a complete stack trace when a thread blocks while pinned, highlighting native frames and frames holding monitors. Running with the option -Djdk.tracePinnedThreads=short limits the output to just the problematic frames.

If these mechanisms detect places where pinning is both long-lived and frequent, replace the use of synchronized with [ReentrantLock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html) in those particular places (again, there is no need to replace synchronized where it guards short lived or infrequent operations). Guarding short-lived operations, such as in-memory operations, or infrequent ones with synchronized blocks or methods should have no adverse effect.
