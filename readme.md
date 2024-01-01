# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ are lightweight threads that are designed to make concurrent applications both simpler and more scalable. Pre-existing Java threads were based on operating system threads, which proved insufficient to meet the demands of modern concurrency. Applications such as databases or web servers should serve millions of concurrent requests, but the Java runtime cannot efficiently handle more than a few thousand. If programmers continue to use threads as the unit of concurrency, they will severely limit the throughput of their applications. Alternatively, they can switch to various _asynchronous_ APIs ([futures](https://github.com/aliakh/demo-java-completablefuture/blob/master/readme.md), [reactive streams](https://github.com/aliakh/demo-java-reactive-streams/blob/master/readme.md), etc.), which are more difficult to develop, debug, and understand, but do not block operating system threads and therefore provide much better performance.

The main goal of virtual threads is to add user-space threads managed by the Java runtime, which would be used alongside the existing heavyweight, kernel-space threads managed by operating systems. Virtual threads are much more lightweight than kernel-mode threads in memory usage, and the overhead of context switching and blocking among them is close to zero. Programmers can create millions of virtual threads in a single JVM instance, and get better performance using much simpler _synchronous blocking_ code.

<sub>Virtual threads were added in Java 19 as a preview feature and released in Java 21.</sub>


## Platform threads and virtual threads

A _thread of execution_ is the smallest unit of processing that can be scheduled that runs concurrently with other such units. A Java virtual machine uses the _Thread_ class to manage threads. There are two kinds of threads, platform threads and virtual threads.

_Platform threads_ are mapped one-to-one to kernel-mode threads scheduled by the operating system, for their entire lifetime. The number of available platform threads is limited to the number of operating system threads.  A typical Java virtual machine may support no more than a few thousands of virtual threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of limited resources.

_Virtual threads_ are user-mode threads scheduled by the Java virtual machine rather than the operating system. Virtual threads are mapped many-to-many to kernel-mode threads scheduled by the operating system. A Java virtual machine may support millions of virtual threads.

>Virtual threads are suitable for executing tasks that spend most of the time blocked and are not intended for long-running CPU-intensive operations.

Many virtual threads employ a few platform threads used as _carrier threads_. When the Java runtime schedules a virtual thread, it _mounts_ the virtual thread on a carrier thread. When the virtual thread performs a blocking I/O operation or locking, the virtual thread can _unmount_ from its carrier thread. When the carrier thread is free, the Java runtime scheduler can mount a different virtual thread on it.

<sub>Virtual threads have no access to their carrier threads.</sub>

A virtual thread cannot be unmounted during blocking operations when it is _pinned_ to its carrier. A virtual thread is pinned in the following situations:



* the virtual thread runs code inside a _synchronized_ block or method
* the virtual thread runs a _native method_ or a _foreign function_

Pinning does not make an application incorrect, but frequent and long-lived pinning might hinder its scalability.


## How to use virtual threads

The _Thread_ class has public constructors and the inner _Thread.Builder_ interface to create threads. For backward compatibility, the public constructors of the _Thread_ class can create only platform threads. Virtual threads are instances of the non-public class _VirtualThread_ which cannot be instantiated directly. So to create virtual threads you should use a builder with the _Thread.Builder.OfVirtual_ subinterface. To create platform threads you should use a similar builder with the _Thread.Builder.OfPlatform_ subinterface. These builders are returned from static factory methods _Thread.ofVirtual()_ and _Thread.ofPlatform()_ respectively.

![thread class diagram](/images/thread_class_diagram.png)

There are four ways to create and use virtual threads:



* the _thread builder_
* the _static factory method_
* the _thread factory_
* the _executor service_

The virtual _thread builder_ allows you to create a virtual thread with all their available parameters: name, _inheritable-thread-local variables_ inheritance flag, uncaught exception handler, and _Runnable_ task.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
System.out.println(builder.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadBuilder

Thread thread = builder.start(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


<sub>For the platform thread builder, you can specify additional parameters: thread group, <em>daemon</em> flag, priority, and stack size.</sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a _Runnable_ task. (Note that by default, the virtual thread name is empty).


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _thread factory_ allows you to create virtual threads by specifying a _Runnable_ task to the instance of the _ThreadFactory_ interface. The parameters of virtual threads are determined by the current state of the thread builder from which this factor is created. (Note that the thread factory is thread-safe and the thread builder is not).


```
Thread.Builder builder = Thread.ofVirtual();

ThreadFactory threadFactory = builder.factory();
System.out.println(threadFactory.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadFactory

Thread thread = threadFactory.newThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _executor service_ allows you to execute _Runnable_ and _Callable_ tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```


code examples


## How to properly use virtual threads

Virtual threads almost fully support the API and semantics of the pre-existing _Thread_ class.  But this abstraction essentially shifts the problem of handling thread blocking from the programmer to the Java runtime environment. To get real performance gains in concurrent applications, the programmer must know the details of their implementation (or at least the rules of thumb for when virtual threads can be used and when not).


### Write blocking synchronous code in the thread-per-task style

Blocking a platform thread needlessly keeps the operand system thread (a relatively limited resource) from doing useful work. Therefore, non-blocking asynchronous frameworks (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, etc.) have been developed to reduce thread blocking and increase CPU resource utilization. However, their disadvantage is a more complex concurrent model, which makes it harder for programmers to develop, debug, and understand such code. Such non-blocking asynchronous frameworks that use their techniques of preventing thread blocking would not benefit much from using virtual threads.

In contrast, blocking a virtual thread is cheap and encouraged. It allows virtual threads to write blocking synchronous code in a simple thread-per-request style. This allows developers to create simpler but still effective concurrent code.

The following non-blocking asynchronous code won't get much benefit from using virtual threads, because the _CompletableFuture_ class already manages blocking operation system threads:

<sub>The following code is a simplified example of an asynchronous multistage workflow. First, we need to call two long-running methods that return a product price in the EUR and the EUR/USD exchange rate. Then, we need to calculate the net product price from the results of these methods. Then, we need to call the third long-running method that takes the net product price and returns the tax amount. Finally, we need to calculate the gross product price from the net product price and the tax amount.</sub>


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


The following blocking synchronous code will benefit from using virtual threads because long-running tasks are executed in parallel in separate virtual threads:


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



### Do not pool virtual threads

Creating a platform thread is a rather time-consuming process, as it requires the creation of an operating system thread. Thread pool executors are designed to reuse threads between tasks and reduce this time. They contain a pool of pre-created worker threads to which _Runnable_ and _Callable_ tasks are passed through a _BlockingQueue_ instance. A worker thread is not destroyed after executing one task but is used to execute the next task read from the task queue.

Unlike platform threads, creating virtual threads is a fast process. Therefore, there is no need to create a pool of virtual threads. If the program logic requires the use of an _ExecutorService_ instance, use an executor specially designed for virtual threads, created in the static factory method _Executors.newVirtualThreadPerTaskExecutor_. This executor does not use a thread pool and creates a new virtual thread for each submitted task. Moreover, this executor itself is lightweight, and you can create and close it at any desired location within the _try-with-resources_ block.

The following code incorrectly uses a cached thread pool executor to reuse virtual threads between tasks:


```
try (var executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
   System.out.println(executorService);

   Future<String> future = executorService.submit(() -> "omega");
   future.get();
}
```


The following code correctly uses a _thread-per-task_ virtual thread executor to create a new thread for each task:


```
try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService);

   Future<String> future = executorService.submit(() -> "alpha");
   future.get();
}
```



### Use semaphores instead of fixed thread pools to limit concurrency

The main objective of thread pools is to reuse threads when executing many tasks. When tasks are submitted to a thread pool, they are placed in a task queue from which they are retrieved by worker threads for execution (see _ThreadPoolExecutor#workQueue_). An additional objective in using thread pools wi_th a fixed number of worker threads_ is that they can be used to limit the concurrency of a particular operation (for example, an external resource cannot handle more than N concurrent requests).

However, since there is no need to reuse virtual threads, there is no need to use thread pools with a fixed number of worker threads. Instead, it is better to use semaphores with the same number of permissions to limit concurrency. A semaphore also contains a queue inside it, not of tasks, but of threads blocked on it (see _AbstractQueuedSynchronizer#head_). So this replacement is quite functionally equivalent.

The following code, which uses a fixed pool of threads to limit concurrency when accessing some shared resource, will not benefit from the use of virtual threads:


```
private final ExecutorService executorService = Executors.newFixedThreadPool(8);

public Object useFixedExecutorServiceToLimitConcurrency() throws ExecutionException, InterruptedException {
   Future<Object> future = executorService.submit(() -> sharedResource());
   return future.get();
}
```


The following code, which uses a semaphore to limit concurrency when accessing some shared resource, will benefit from the use of virtual threads:


```
private final Semaphore semaphore = new Semaphore(8);

public Object useSemaphoreToLimitConcurrency() throws InterruptedException {
   semaphore.acquire();
   try {
       return sharedResource();
   } finally {
       semaphore.release();
   }
}
```



### Use thread-local variables carefully or switch to scoped values

To achieve better scalability of virtual threads, you should reconsider using _thread-local variables_ and _inheritable-thread-local variables_. Thread-local variables provide each thread with its copy of a variable. A thread-local variable works as an implicit, thread-bound parameter and allows passing data from a caller to a callee through a sequence of intermediate methods.

Virtual threads support thread-local behavior in the same way as platform threads. But because virtual threads can be very numerous, negative design features of local variables can be applied much more significantly:



* _unconstrained mutability_ (any code that can call the get method of a thread-local variable can call the set method of that variable, even if an object in a thread-local variable is immutable)
* _unbounded lifetime_ (once a copy of a thread-local variable is set via the set method, the value is retained for the lifetime of the thread, or until code in the thread calls the remove method)
* _expensive inheritance_ (each child thread copies, not reuses, inheritable-thread-local variables of a parent thread)

_Scoped values_ may be a better alternative to thread-local variables, especially when using large numbers of virtual threads. Unlike a thread-local variable, a scoped value is written once, is available only for a bounded context, and is inherited in a _structured concurrency_ scope.

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
       System.out.println(threadLocal.get()); // "one"
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
```



### Use synchronized blocks and methods carefully or switch to reentrant locks

To improve scalability when using virtual threads, you should revise _synchronized_ blocks and methods to avoid frequent and long-lived pinning (such as I/O operations). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. As an alternative, you can replace these _synchronized_ blocks and methods with _ReentrantLock_ which also guarantees mutually exclusive access.

To identify pinning, you can use the JVM flag _-Djdk.tracePinnedThreads=full_ when executing your application.

The following code uses a _synchronized_ block with an explicit object lock that causes pinning of virtual threads:


```
private final Object lockObject = new Object();

public void useSynchronizedBlock() {
   synchronized (lockObject) {
       exclusiveResource();
   }
}
```


The following code uses a _ReentrantLock_ that doesn’t cause pinning of virtual threads:


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


code examples


## Conclusion

Dan Kegel’s well-known article [The C10K problem](http://www.kegel.com/c10k.html) about 10,000 concurrent connections in web servers was relevant in 2014. After ten years, a mainstream web server needs to handle 10,000,000 simultaneous connections. When trying to achieve similar performance in Java virtual machines, it is the threads that are the bottleneck. When a mainstream Java web server stops being unable to handle several thousand concurrent connections, it still has plenty of other available resources: sockets, memory, and CPU time.

Using asynchronous solutions such as asynchronous Servlet APIs or reactive web runtimes such as Netty or Undertow does not solve the problem completely. Such reactive applications work well only when all pipelines from the web server to the storage are reactive and non-blocking. However, there are still a lot of synchronous HTTP clients, databases, message brokers, etc. around because such reactive applications are harder to develop, debug, and understand.

Java _virtual threads_ are an attempt to achieve the same level of throughput that Golang is already demonstrating with its _goroutines_ on the same hardware. Considerable work has been done in the Java core library to make the existing threading and I/O classes compatible with virtual threads. For your application to benefit from the use of virtual threads, you should follow known guidelines. As a rule of thumb, to benefit from virtual threads your application must have thousands of threads performing intensive blocking I/O operation.
