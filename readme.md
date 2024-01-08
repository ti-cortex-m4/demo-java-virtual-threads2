# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ are lightweight threads designed to develop _high-throughput_ concurrent applications. Pre-existing Java threads were based on operating system (OS) threads, which proved insufficient to meet the demands of modern concurrency. Applications such as web servers, databases, or message brokers nowadays must serve millions of concurrent requests, but the JVM cannot efficiently handle more than a few thousand threads.

If programmers continue to use threads as the concurrent model, they will severely limit the performance of their applications. Alternatively, they can switch to other concurrent models (for example, callbacks, [futures](https://github.com/aliakh/demo-java-completablefuture/blob/master/readme.md), or reactive streams) that do not block threads. Such solutions, while showing much better performance, are much more difficult to write, debug, and understand.

The purpose of virtual threads is to add lightweight, user-space threads managed by the JVM to be used alongside the existing heavyweight, kernel-space threads managed by the OS. Programmers can create millions of virtual threads and get much better throughput using much simpler synchronous blocking code.

<sub>Virtual threads were added in Java 19 as a preview feature and released in Java 21.</sub>


## Platform threads and virtual threads

A thread is a _thread of execution_ in a program, that is an independently scheduled execution unit that belongs to a process in the OS. This entity has a program counter and stack. The _Thread_ class is a facade to manage _threads of execution_ in the JVM. This class has fields, methods, and constructors. There are two kinds of threads, platform threads and virtual threads.


### Platform threads

_Platform threads_ are _kernel-mode_ threads mapped one-to-one to _kernel-mode_ OS threads. A platform thread is connected to an OS thread for their entire lifetime. The OS schedules OS threads and therefore, platform threads. The operating system affects the thread creation time and _context switching_ time, as well as the number of platform threads. Platform threads usually have a large, fixed-size stack allocated in a process _stack segment_. For the JVM running on Linux x64 the default stack size is 1 MB, so 1000 OS threads require 1 GB of stack memory. Simplified, the maximum number of OS threads can be calculated as the total virtual memory size divided by the stack size. So, the number of available platform threads is limited to the number of OS threads. A typical JVM can support no more than a few thousand platform threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of a limited resource.


### Virtual threads

_Virtual threads_ are _user-mode_ threads mapped many-to-many to _kernel-mode_ OS threads. Virtual threads are scheduled by the JVM, rather than the OS. A virtual thread is a regular Java object, so the thread creation time and thread switching time is negligible. The stack size of virtual threads is much smaller than for platform threads and is dynamically sized. Thus, the number of virtual threads does not depend on the limitations of the OS. A typical JVM can support millions of virtual threads.

>Virtual threads are suitable for executing tasks that spend most of the time blocked and are not intended for long-running CPU-intensive operations.

Summary of quantitative differences between platform and virtual threads:


<table>
  <tr>
   <td>
   </td>
   <td>platform threads
   </td>
   <td>virtual threads
   </td>
  </tr>
  <tr>
   <td>stack size
   </td>
   <td>1 MB
   </td>
   <td>resizable
   </td>
  </tr>
  <tr>
   <td>thread metadata size
   </td>
   <td>> 2KB
   </td>
   <td>200-300 B
   </td>
  </tr>
  <tr>
   <td>context switching time
   </td>
   <td>1-10 µs
   </td>
   <td>~ 0.2 µs
   </td>
  </tr>
</table>


The implementation of virtual threads consists of two parts: continuations and a scheduler.

Continuation (_delimited continuation_ or _coroutine_) is a sequential code that may yield execution at some point by itself and pass control outside of itself. When continuation is resumed, control returns to the last yield point, with the execution context up to the entry point remains intact. Continuation is implemented by the internal _jdk.internal.vm.Continuation_ class, and developers are not expected to use them directly.

The virtual threads scheduler manages the yelding and resuming the coroutines. It is pluggable and now for this purpose is used a dedicated _ForkJoinPool_ FIFO executor.


### Carrier threads

Many virtual threads employ a few platform threads used as _carrier threads_. Over its lifetime, a virtual thread may run on several different carrier threads. Those carrier threads belong to the scheduler. When the JVM schedules a virtual thread, it _mounts_ the virtual thread on a carrier thread. Today, most of the operations in the Java core library (I/O and _java.util.concurrent_) have been refactored to make them non-blocking. When a virtual thread is blocked on such an operation, the scheduler dispatches the operation and then _unmounts_ the virtual thread from the carrier thread. While the blocking operation from the virtual thread proceeds in the background, the carrier thread is unblocked and can execute another virtual thread. When the operation completes, the scheduler mounts the virtual thread to an available carrier thread.

<sub>The stack of the virtual thread is copied from the heap to the stack of the carrier thread during mounting and is moved back to the heap during the unmounting.</sub>

However, some operations in the Java core library do not yet support this feature and instead _capture_ the carrier thread. This behavior can be caused by limitations of the OS (which affects many file system operations) or of the JDK (such as with the _Object.wait()_ method). The capture of an OS thread is compensated by temporarily adding a platform thread to the JVM scheduler.

A virtual thread also cannot be unmounted during blocking operations when it is _pinned_ to its carrier. This occurs when a virtual thread executes a _synchronized_ block/method, a _native method,_ or a _foreign function_. During pinning, the JVM scheduler does not create an additional carrier thread, so frequent and long-lived pinning may worsen the scalability of the application.


### Green threads

Virtual threads are not _green treads_. Green threads were mapped to a single _kernel-mode_ OS thread. Green threads existed since Java 1.1 and in Java 1.3 were deprecated in favor of platform threads.

![history o _Java threads](/images/history_of_Java_threads.png)


## How to use virtual threads

The _Thread_ class has public constructors and the inner _Thread.Builder_ interface for creating and starting both platform and virtual threads.

For backward compatibility, the public constructors of the _Thread_ class can create only platform threads. Virtual threads are instances of the nonpublic class _VirtualThread_ that cannot be instantiated directly. So, to create virtual threads you should use a builder that implements the _Thread.Builder.OfVirtual_ subinterface. By analogy, to create platform threads you should use a similar builder that implements the _Thread.Builder.OfPlatform_ subinterface. These builders are returned from static factory methods _Thread.ofVirtual()_ and _Thread.ofPlatform()_ respectively.

![thread class diagram](/images/thread_class_diagram.png)

There are four ways to create virtual threads:



* the _thread builder_
* the _static factory method_
* the _thread factory_
* the _executor service_

The virtual _thread builder_ allows you to create a virtual thread with all available parameters: name, _inheritable-thread-local variables_ inheritance flag, uncaught exception handler, and _Runnable_ task. (Note that the virtual threads are _daemon_ threads and have a fixed thread priority that cannot be changed).


```
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
Thread thread = builder.start(() -> System.out.println("run"));
thread.join();
```


<sub>For the platform thread builder, you can specify additional parameters: thread group, <em>daemon</em> flag, priority, and stack size. </sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a _Runnable_ task. (Note that by default, the virtual thread name is empty).


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
thread.join();
```


The _thread factory_ allows you to create virtual threads by specifying a _Runnable_ task to the instance of the _ThreadFactory_ interface. The parameters of virtual threads are specified by the current state of the thread builder from which this factor is created. (Note that the thread factory is thread-safe, but the thread builder is not).


```
Thread.Builder builder = Thread.ofVirtual();
ThreadFactory threadFactory = builder.factory();
Thread thread = threadFactory.newThread(() -> System.out.println("run"));
thread.join();
```


The _executor service_ allows you to execute _Runnable_ and _Callable_ tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```



## How to properly use virtual threads

Virtual threads almost entirely support the API and semantics of the pre-existing _Thread_ class. Their implementation effectively shifts the task of handling OS thread blocking from the programmer to the JVM. But to get a real performance gain and avoid known pitfalls, the programmer must know the details of this implementation.


### Write blocking synchronous code in the thread-per-task style

Blocking a platform thread keeps the OS thread, a limited resource, from doing useful work. Therefore, non-blocking asynchronous frameworks have been developed to reduce thread blocking and increase CPU utilization. Such solutions, although they show much better performance, are nevertheless much more difficult to write, debug, and understand. Such frameworks that use their techniques of preventing thread blocking would not benefit much from using virtual threads.

In contrast, blocking a virtual thread is cheap and even encouraged. It allows virtual threads to write blocking synchronous code in a simple thread-per-task style. This allows programmers to create simpler yet efficient concurrent code.

The following non-blocking asynchronous code will not benefit much from using virtual threads, because the _CompletableFuture_ class already manages the blocking of the threads:

<sub>The following code is a simplified example of an asynchronous multistage workflow. First, we call two long-running methods that return a product price in the EUR and the EUR/USD exchange rate. Then we calculate the net product price from the results of these methods. Then we call the third long-running method that takes the net product price and returns the tax amount. Finally, we calculate the gross product price from the net product price and the tax amount.</sub>


```
CompletableFuture.supplyAsync(this::getPriceInEur) 
   .thenCombine(CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate) 
   .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount)))) 
   .whenComplete((grossAmountInUsd, t) -> { 
       if (t == null) {
           assertEquals(108, grossAmountInUsd);
       } else {
           fail(t);
       }
   })
   .get(); 
```


The following blocking synchronous code will benefit from using virtual threads because the much simpler code returns the same value for the same duration as the previous complex one:


```
try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   Future<Integer> priceInEur = executorService.submit(this::getPriceInEur); 
   Future<Integer> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd); 
   int netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get(); 

   Future<Integer> tax = executorService.submit(() -> getTax(netAmountInUsd)); 
   int grossAmountInUsd = netAmountInUsd * (1 + tax.get());
   assertEquals(108, grossAmountInUsd);
}
```



### Do not pool virtual threads

Creating a platform thread is a rather time-consuming process because it requires the creation of an OS thread. Thread pool executors are designed to reduce this time by reusing threads between task executions. They contain a pool of pre-created worker threads to _Runnable_ and _Callable_ tasks are passed through a blocking queue.

Unlike creating platform threads, creating virtual threads is a fast process. Therefore, there is no need to create a pool of virtual threads. If the program requires an _ExecutorService_ instance, use a specially designed implementation for virtual threads, created in the static factory method _Executors.newVirtualThreadPerTaskExecutor()_. This executor does not use a thread pool and creates a new virtual thread for each submitted task. In addition, this executor is lightweight and you can create and close it at any desired code within the _try-with-resources_ block.

The following code needlessly uses a cached thread pool executor to reuse virtual threads between tasks:


```
try (var executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
   executorService.submit(() -> { sleep(1000); System.out.println("omega"); });
}
```


The following code correctly uses a _thread-per-task_ virtual thread executor to create a new thread for each task:


```
try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   executorService.submit(() -> { sleep(1000); System.out.println("alpha"); });
}
```



### Use semaphores instead of fixed thread pools to limit concurrency

The main purpose of thread pools is to reuse threads between executing multiple tasks. When tasks are submitted to a thread pool, they are inserted into a task queue. Tasks are retrieved from the queue by worker threads for execution. An additional purpose of using thread pools with a _fixed number_ of worker threads can be to limit the concurrency of a particular operation. They can be used in a situation when an external resource cannot process more than a predefined number of concurrent requests.

However, since there is no need to reuse virtual threads, there is no need to use any thread pools for them. Instead, it is better to use semaphores with the same number of permits to limit concurrency. Just as a thread pool contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/ThreadPoolExecutor.java#L454) of tasks, a semaphore contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/locks/AbstractQueuedSynchronizer.java#L319) of threads blocked on it.

The following code, which uses a fixed pool of threads to limit concurrency when accessing some shared resource, will not benefit from the use of virtual threads:


```
private final ExecutorService executorService = Executors.newFixedThreadPool(8);

public String useFixedExecutorServiceToLimitConcurrency() throws ExecutionException, InterruptedException {
   Future<String> future = executorService.submit(this::sharedResource());
   return future.get();
}
```


The following code, which uses a semaphore to limit concurrency when accessing some shared resource, will benefit from the use of virtual threads:


```
private final Semaphore semaphore = new Semaphore(8);

public String useSemaphoreToLimitConcurrency() throws InterruptedException {
   semaphore.acquire();
   try {
       return sharedResource();
   } finally {
       semaphore.release();
   }
}
```



### Use thread-local variables carefully or switch to scoped values

To achieve better scalability of virtual threads, you should reconsider using _thread-local variables_ and _inheritable-thread-local variables_. Thread-local variables provide each thread with its copy of a variable, which is set to a value that is independent of the values set by other threads. A thread-local variable works as an implicit, thread-bound parameter and allows passing data from a caller to a callee through a sequence of intermediate methods.

Virtual threads support thread-local behavior in the same way as platform threads. But because virtual threads can be very numerous, the following design features of thread-local variables can have a more significant negative impact:



* _unconstrained mutability_ (any code that can call the _get_ method of a thread-local variable can call the _set_ method of that variable, even if an object in a thread-local variable is immutable)
* _unbounded lifetime_ (once a copy of a thread-local variable is set via the _set_ method, the value is retained for the lifetime of the thread, or until code in the thread calls the _remove_ method)
* _expensive inheritance_ (each child thread copies, not reuses, _inheritable-thread-local variables_ of the parent thread)

Getting rid of thread-local variables can be a challenge. In some cases, _scoped values_ may be a better alternative to thread-local variables, especially when using large numbers of virtual threads. Unlike a thread-local variable, a scoped value is written once, is available only for a bounded context, and is inherited in a _structured concurrency_ scope.

<sub>Scoped values are a preview feature in Java 20 and have not been released at the time of writing.</sub>

The following code shows that a thread-local variable is mutable, is inherited in a child thread started from the parent thread, and exists until it is removed.


```
private final InheritableThreadLocal<String> threadLocal = new InheritableThreadLocal<>();

public void useThreadLocalVariable() throws InterruptedException {
   threadLocal.set("zero");
   assertEquals("zero", threadLocal.get());

   threadLocal.set("one");
   assertEquals("one", threadLocal.get());

   Thread childThread = new Thread(() -> {
      assertEquals("one", threadLocal.get());
   });
   childThread.start();
   childThread.join();

   threadLocal.remove();
   assertNull(threadLocal.get());
}
```


The following code shows that a scoped value is immutable, is reused in a structured concurrency scope, and exists only in a bounded context.


```
private final ScopedValue<String> scopedValue = ScopedValue.newInstance();

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
                       assertEquals("zero", scopedValue.get());
                       return -1;
                   }
               );
               scope.join().throwIfFailed();
           } catch (InterruptedException | ExecutionException e) {
               fail(e);
           }
       }
   );

   assertThrows(NoSuchElementException.class, scopedValue::get);
}
```



### Use synchronized blocks and methods carefully or switch to reentrant locks

To improve scalability using virtual threads, you should revise _synchronized_ blocks and methods to avoid frequent and long-running pinning (such as I/O operations). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. Alternatively, you can replace these _synchronized_ blocks and methods with _ReentrantLock_, which also guarantees mutually exclusive access.

<sub>To identify pinning, you can use the JVM flag <em>-Djdk.tracePinnedThreads=full</em> when executing your application.</sub>

The following code uses a _synchronized_ block with an explicit object lock that causes pinning of virtual threads:


```
private final Object lockObject = new Object();

public String useSynchronizedBlockForExclusiveAccess() {
   synchronized (lockObject) {
       return exclusiveResource();
   }
}
```


The following code uses a _ReentrantLock_ that does not cause pinning of virtual threads:


```
private final ReentrantLock reentrantLock = new ReentrantLock();

public String useReentrantLockForExclusiveAccess() {
   reentrantLock.lock();
   try {
       return exclusiveResource();
   } finally {
       reentrantLock.unlock();
   }
}
```



## Conclusion

Virtual threads are intended for developing high-throughput concurrent applications when a programmer has access to millions of units of concurrency that have the well-known _Thread_ class. Virtual threads are intended to replace platform threads in those applications that spend most of their time blocked on I/O operations.

To summarize, these design features make virtual streams effective in these situations:



* virtual threads are _user-mode_ threads, so the overhead of their creation and _context switching_ is negligible
* the Java core library has been refactored to make operations non-blocking
* the virtual thread stack is much smaller and dynamically resizable

Java _virtual threads_ are a solution to achieve the same level of throughput that Golang is already demonstrating with its _goroutines_. Considerable work has been done in the Java core library to make it compatible with virtual threads. For your applications to benefit from using virtual threads, you need to follow known guidelines. Also, third-party dependencies used in your applications must be refactored by their owners or patched to become compatible with virtual threads.
