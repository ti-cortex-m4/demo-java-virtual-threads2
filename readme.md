## Introduction

Virtual threads are lightweight threads designed to use computing resources more efficiently than pre-existing Java threads. Virtual threads enable a competitive “one thread per blocking call” concurrent model that is easier for developers than _futures/promises_ and _reactive streams_. Virtual threads support the APIs and semantics of pre-existing threads. But to effectively use virtual Streams, developers need to follow some guidelines (recommendations) related to the features of their implementation.

Virtual threads were added in Java 19 as a preview feature and released in Java 21.


## Platform threads and virtual threads

A _thread_ is the smallest unit of processing that can be scheduled thatt runs concurrently with other such units. A Java thread is an instance of [java.lang.Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html). There are two kinds of threads, platform threads and virtual threads.

A _platform thread_ is implemented as a thin wrapper around an OS thread. A platform thread runs Java code on its underlying OS thread, and the platform thread captures its OS thread for the platform thread's entire lifetime. Consequently, the number of available platform threads is limited to the number of OS threads.

>Platform threads suitable for running all types of tasks but are not recommended for blocking I/O operations ( because a blocked platform thread blocks the OS thread, which may be a limited resource).

However, a virtual thread isn't tied to a specific OS thread. A virtual thread still runs code on an OS thread. However, when code running in a virtual thread calls a blocking I/O operation, the Java runtime suspends the virtual thread until it can be resumed. The OS thread associated with the suspended virtual thread is now free to perform operations for other virtual threads. Unlike platform threads, virtual threads typically have a shallow call stack, performing as few as a single HTTP client call or a single JDBC query.  The number of available platform threads in a single JVM might support millions of virtual threads.

>Virtual threads are recommended for numerous blocking I/O operations and are not recommended for long running CPU-bound operations. Virtual threads are suitable for running tasks that spend most of the time blocked, often waiting for I/O operations to complete. However, they aren't intended for long-running CPU-intensive operations.

However, virtual threads are managed by the Java runtime and are not thin, one-to-one wrappers over OS threads. Instead, virtual threads are implemented in user space by the Java runtime.

Use virtual threads in high-throughput concurrent applications, especially those that consist of a great number of concurrent tasks that spend much of their time waiting. Server applications are examples of high-throughput applications because they typically handle many client requests that perform blocking I/O operations such as fetching resources.

Virtual threads are not faster threads; they do not run code any faster than platform threads. They exist to provide scale (higher throughput), not speed (lower latency).


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


The following example creates a platform thread with all the options available in the _Thread.Builder.ofPlatform_ interface.


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


The following example creates a virtual thread with all the options available in the _Thread.Builder.OfVirtual_ interface.


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


###### Creating virtual threads from a thread factory

Builders are the best alternative to constructors (see "Effective Java" 3rd edition by Joshua Bloch, items 1 and 2). Interface _Thread.Builder_ was added in Java 21 and is not yet widely used. Much wider use is made of the _ThreadFactory_ interface, which was added in Java 1.5. The _Thread.Builder.factory()_ interface method returns a thread-safe _ThreadFactory_ instance to create threads from the current state of the builder.

The following example creates a virtual thread from a factory created from the current state of the builder.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread");
ThreadFactory threadFactory = builder.factory();
Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertEquals("virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());
```



###### Creating virtual threads from an executor service

Thread pools allow you to separate the creation (and destruction) of threads and the execution of tasks by reusable threads. The two main parts of the thread pools executors for platform threads are the worker thread pool and the task queue. Virtual thread executors created in the _Executors.newVirtualThreadPerTaskExecutor()_ method  implement the same _ExecutorService_ interface, but their implementation is different: they create a new virtual thread for each task.

The following example creates an unbounded, thread-per-task _ExecutorService_ instance, passes a _Runnable_ task to it, waits for it to complete, and closes the executor service.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```



### Scheduling virtual threads

The operating system schedules when a platform thread is run. However, the Java runtime schedules when a virtual thread is run. When the Java runtime schedules a virtual thread, it assigns or _mounts_ the virtual thread on a platform thread, then the operating system schedules that platform thread as usual. This platform thread is called a _carrier_. After running some code, the virtual thread can _unmount_ from its carrier. This usually happens when the virtual thread performs a blocking I/O operation. After a virtual thread unmounts from its carrier, the carrier is free, which means that the Java runtime scheduler can mount a different virtual thread on it.

A virtual thread cannot be unmounted during blocking operations when it is _pinned_ to its carrier. A virtual thread is pinned in the following situations:



* The virtual thread runs code inside a synchronized block or method
* The virtual thread runs a native method or a foreign function

Pinning does not make an application incorrect, but it might hinder its scalability. Try avoiding frequent and long-lived pinning by revising synchronized blocks or methods that run frequently and guarding potentially long I/O operations with java.util.concurrent.locks.ReentrantLock.


### How to properly use virtual threads

Virtual threads were created to increase parallelism (the number of tasks running at the same time) when multicore CPUs became ubiquitous (around 2000). Virtual threads almost fully support the API and semantics of the _Thread_ class. Virtual threads shift the objective of processing blocking threads from a programmer to the Java runtime. But to get a real parallelism performance boost, the programmer must know the details of their implementations (or at least the rules of thumb when to use virtual threads and when not).


###### Do not use virtual threads for CPU-intensive tasks

Use them for blocking operations (HTTP call or database query).


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

public void useThreadLocalVariable() throws InterruptedException {
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

public void useScopedValue() {
   ScopedValue.where(SCOPED_VALUE, "zero").run(
       () -> {
           assertEquals("zero", SCOPED_VALUE.get());

           ScopedValue.where(SCOPED_VALUE, "one").run(
               () -> assertEquals("one", SCOPED_VALUE.get()) // bounded lifetime
           );
           assertEquals("zero", SCOPED_VALUE.get());

           try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
               Supplier<String> value = scope.fork(() -> {
                       assertEquals("zero", SCOPED_VALUE.get()); // cheap inheritance
                       return null;
                   }
               );
               scope.join().throwIfFailed();
               assertNull(value.get());
           } catch (Exception e) {
               fail(e);
           }
       }
   );

   assertThrows(NoSuchElementException.class, () -> assertNull(SCOPED_VALUE.get())); // bounded lifetime
}
```



###### Use synchronized blocks and methods carefully or switch to reentrant locks

for good scalability with virtual threads, avoid frequent and long-lived pinning by revising synchronized blocks and methods that run often and contain I/O operations, particularly long-running ones. In this case, a good alternative to synchronization is a ReentrantLock

To achieve better scalability of virtual threads, you should revise _synchronized_ blocks and methods, especially lengthy and frequent ones (that often contain I/O operations). As an althernative, you should replace them with _ReentrantLock_ that also guarantees sequential access to a resource.

When a virtual thread performs a blocking operation inside a _synchronized_ block or method, this does not release the OS thread (_pinning_). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. To get rid of pinning, where it is both long-lived and frequent, you should replace these _synchronized_ blocks and methods with _ReentrantLock_ that also guarantees mutually exclusive access to a resource.

The following code uses a _synchronized_ block with an explicit object lock.


```
private final Object lockObject = new Object();

public void useReentrantLock() {
   synchronized (lockObject) {
       exclusiveResource();
   }
}
```


The following code uses a reentrant lock.


```
private final ReentrantLock reentrantLock = new ReentrantLock();

public void useSynchronizedBlock() {
   reentrantLock.lock();
   try {
       exclusiveResource();
   } finally {
       reentrantLock.unlock();
   }
}
```




