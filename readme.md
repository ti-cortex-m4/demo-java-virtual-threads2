# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ are lightweight threads designed to develop _high-throughput_ concurrent applications. Pre-existing Java threads were based on operating system (OS) threads that proved insufficient to meet the demands of modern concurrency. Applications such as web servers, databases, or message brokers nowadays must serve millions of concurrent requests, but the OS and therefore the JVM cannot efficiently handle more than a few thousand threads.

Currently, programmers can either use threads as the units of concurrency and write synchronous blocking code. These applications are easy to develop, but they are not scalable, because the number of OS threads is limited. Or, they can use other concurrent models (futures, async/await, coroutines, actors, etc.) that reuse threads without blocking them. Such solutions, while showing much better scalability, are much more difficult to write, debug, and understand.

New lightweight _virtual threads_ managed by the JVM, can be used alongside the existing heavyweight _platform threads_ managed by the OS. Programmers can create millions of virtual threads and get similar scalability using much simpler synchronous blocking code.

<sup>All information in this article corresponds to OpenJDK 21.</sup>


## Platform threads and virtual threads

A thread is a _thread of execution_ in an application, that is an independently scheduled execution unit that belongs to a process in the OS. This entity has a program counter and stack. The `Thread` class is a wrapper in the JVM to manage threads of execution in the OS. This class has fields, methods, and constructors. There are two kinds of threads, platform threads and virtual threads.


### Platform threads

_Platform threads_ are _kernel-mode_ threads mapped one-to-one to _kernel-mode_ OS. The OS schedules OS threads and therefore, platform threads. The OS affects the thread creation time and the context switching time, as well as the number of platform threads. Platform threads usually have a large, fixed-size stack allocated in a process _stack segment_. (For the JVM running on Linux x64 the default stack size is 1 MB, so 1000 OS threads require 1 GB of stack memory). So, the number of available platform threads is limited to the number of OS threads. A JVM on a consumer-grade computer can support up to ten thousand platform threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of a limited resource.


### Virtual threads

_Virtual threads_ are _user-mode_ threads mapped many-to-many to _kernel-mode_ OS threads. Virtual threads are scheduled by the JVM, rather than the OS. A virtual thread is a regular Java object, so the thread creation time and context switching time are negligible. The stack size of virtual threads is much smaller than for platform threads and is dynamically sized. When a virtual thread is inactive, its stack is stored in the JVM heap. Thus, the number of virtual threads does not depend on the limitations of the OS. A JVM on the same computer can support millions of virtual threads.

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
   <td>startup time
   </td>
   <td>> 1000 µs
   </td>
   <td>1-10 µs
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

Continuations are a sequential code that can suspend itself and later be resumed. When a continuation suspends, it saves its content and passes control outside. When a continuation is resumed, control returns to the last suspending point with the previous context.

<sub>Continuation is a low-level construct implemented by the internal <em>jdk.internal.vm.Continuation</em> class and developers should not use them directly. </sub>

The scheduler executes the tasks of virtual threads on a pool of a few platform threads used as _carrier threads_. By default, their initial number is equal to the number of available hardware threads, and their maximum number is 256. The scheduler is pluggable, and by default JVM uses a `ForkJoinPool` FIFO executor.

When a virtual thread calls, for example, a blocking I/O method, the scheduler performs the following actions:



* _unmounts_ the virtual thread from the carrier thread;
* suspends the continuation and saves its content;
* start a non-blocking I/O operation in the OS kernel;
* the scheduler can execute another virtual thread on the same carrier thread.

When the I/O operation completes in the OS kernel, the scheduler performs the opposite actions:



* restores the content of the continuation and resumes it;
* waits until a carrier thread is available;
* _mounts_ the virtual thread to the carrier thread.

To provide this behavior, most of the blocking operations in the Java core library (mainly I/O and _java.util.concurrent_) have been refactored. However, some operations do not yet support this feature and instead _capture_ the carrier thread. This behavior can be caused by limitations of the OS (which affects many file system operations) or of the JDK (such as with the `Object.wait()` method). The capture of an OS thread is compensated by temporarily adding a carrier thread to the scheduler.

A virtual thread also cannot be unmounted during blocking operations when it is _pinned_ to its carrier. This occurs when a virtual thread executes a _synchronized_ block/method, a _native method_, or a _foreign function_. During pinning, the scheduler does not create an additional carrier thread, so frequent and long-lived pinning may worsen the scalability.


## How to use virtual threads

Virtual threads are instances of the nonpublic `java.lang.VirtualThread` class, which is a subclass of the `java.lang.Thread` class.

![thread class diagram](/images/thread_class_diagram.png)

The `Thread` class has public constructors and the inner `Thread.Builder` interface for creating and starting both platform and virtual threads. For backward compatibility, all public constructors of the `Thread` class can create only platform threads. Virtual threads are instances of the nonpublic class that cannot be instantiated directly. The only way to create virtual threads is to use a builder. A similar builder exists for creating platform threads.

Summary of methods of the _Thread_ class to handle virtual and platform threads:


<table>
  <tr>
   <td>Modifier and type
   </td>
   <td>Method
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td><em>final boolean</em>
   </td>
   <td><em>isVirtual()</em>
   </td>
   <td>Returns <em>true</em> if this thread is a virtual thread.
   </td>
  </tr>
  <tr>
   <td><em>static Thread.Builder.OfVirtual</em>
   </td>
   <td><em>ofVirtual()</em>
   </td>
   <td>Returns a builder for creating a virtual <em>Thread</em> or <em>ThreadFactory</em> that creates virtual threads.
   </td>
  </tr>
  <tr>
   <td><em>static Thread.Builder.OfPlatform</em>
   </td>
   <td><em>ofPlatform()</em>
   </td>
   <td>Returns a builder for creating a platform <em>Thread</em> or <em>ThreadFactory</em> that creates platform threads.
   </td>
  </tr>
  <tr>
   <td><em>static Thread</em>
   </td>
   <td><em>startVirtualThread(Runnable)</em>
   </td>
   <td>Creates a virtual thread to execute a task and schedules it to execute. 
   </td>
  </tr>
</table>


There are four ways to create virtual threads:



* the _thread builder_
* the _static factory method_
* the _thread factory_
* the _executor service_

The virtual _thread builder_ allows you to create a virtual thread with all available parameters: name, _inheritable-thread-local variables_ inheritance flag, uncaught exception handler, and `Runnable` task. (Note that the virtual threads are _daemon_ threads and have a fixed thread priority that cannot be changed).


```java
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));

Thread thread = builder.unstarted(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("a virtual thread", thread.getName());
assertTrue(thread.isDaemon());
assertEquals(5, thread.getPriority());
```


<sub>In the platform thread builder, you can specify additional parameters: thread group, <em>daemon</em> flag, priority, and stack size. </sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a `Runnable` task. (Note that by default, the virtual thread name is empty).


```java
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
thread.join();

assertTrue(thread.isVirtual());
assertEquals("", thread.getName());
```


The _thread factory_ allows you to create virtual threads by specifying a `Runnable` task to the instance of the `ThreadFactory` interface. The parameters of virtual threads are specified by the current state of the thread builder from which this factory is created. (Note that the thread factory is thread-safe, but the thread builder is not).


```java
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread");

ThreadFactory threadFactory = builder.factory();

Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("a virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());
```


The _executor service_ allows you to execute `Runnable` and `Callable` tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```java
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```



## How to properly use virtual threads

Virtual threads almost entirely support the API and semantics of the pre-existing _Thread_ class. Their implementation effectively shifts the task of handling OS thread blocking from the programmer to the JVM. But to get a real performance gain and avoid known pitfalls, the programmer must know the details of this implementation.


### Do not use virtual threads for CPU-bound tasks

The OS scheduler for platform threads is _preemptive_. The OS scheduler uses _time slices_ to periodically suspend and resume platform threads. Thus, multiple platform threads executing CPU-bound tasks will eventually show progress even if none of them explicitly yields.

The JVM scheduler for virtual threads is _non-preemptive_ (see "Modern operating systems", 4th edition, by Andrew S. Tanenbaum and Herbert Bos, 2015). A virtual thread is suspended only if it is blocked on an I/O or other supported operation. If you run a virtual thread with a CPU-bound task, this thread monopolizes the OS thread until the task is completed, and other virtual threads experience _starvation_.


### Write blocking synchronous code in the thread-per-task style

Blocking platform threads is costly because it wastes a limited resource. Various asynchronous frameworks use tasks as more fine-grained units of concurrency instead of threads. These frameworks reuse threads without blocking them and indeed achieve higher application scalability. The price for this is a significant increase in development complexity. Since much of the Java platform assumes that execution context is contained in a thread, all that context is lost once we decouple tasks from threads. Debugging and profiling are difficult in such asynchronous applications, and stack traces no longer provide useful information.

In contrast, blocking virtual threads is low-cost, and moreover, it is their main design feature. While the blocked virtual thread is waiting for the operation to complete, the carrier thread and the underlying OS thread are actually not blocked. This allows programmers to create simpler yet efficient concurrent code in the thread-per-task style.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#write-blocking-synchronous-code-in-the-thread-per-task-style)


### Do not pool virtual threads

Creating a platform thread is a rather time-consuming process because it requires the creation of an OS thread. Thread pool executors are designed to reduce this time by reusing threads between executing multiple tasks. They contain a pool of pre-created worker threads to which `Runnable` and `Callable` tasks are passed through a blocking queue.

Unlike creating platform threads, creating virtual threads is a fast process. Therefore, there is no need to create a pool of virtual threads. If the application requires an `ExecutorService` instance, use a specially designed implementation for virtual threads, which is returned from the static factory method `Executors.newVirtualThreadPerTaskExecutor()`. This executor does not use a thread pool and creates a new virtual thread for each submitted task. In addition, this executor itself is lightweight, so you can create and close it at any desired code within the _try-with-resources_ block.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#do-not-pool-virtual-threads)


### Use semaphores instead of fixed thread pools to limit concurrency

The main purpose of thread pools is to reuse threads between executing multiple tasks. When tasks are submitted to a thread pool, they are inserted into a queue. Tasks are retrieved from the queue by worker threads for execution. An additional purpose of using thread pools with a _fixed number_ of worker threads can be to limit the concurrency of a particular operation. They can be used when an external resource cannot process more than a predefined number of concurrent requests.

However, since there is no need to reuse virtual threads, there is no need to use any thread pools for them. Instead, it is better to use a `Semaphore` with the same number of permits to limit concurrency. Just as a thread pool contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/ThreadPoolExecutor.java#L454) of tasks, a semaphore contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/locks/AbstractQueuedSynchronizer.java#L319) of threads blocked on it.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-semaphores-instead-of-fixed-thread-pools-to-limit-concurrency)


### Use thread-local variables carefully or switch to scoped values

To achieve better scalability of virtual threads, you should reconsider using _thread-local variables_ and _inheritable-thread-local variables_. Thread-local variables provide each thread with its own copy of a variable, and inheritable-thread-local variables additionally copy these variables from the parent thread to the child thread. Thread-local variables are typically used to cache mutable objects that are expensive to create. They are also used to implicitly pass thread-bound parameters and return values through a sequence of intermediate methods.

Virtual threads support thread-local behavior (after much consideration by the Project Loom team) in the same way as platform threads. Because virtual threads can be very numerous, the following features of thread-local variables can have a more significant negative effect:



* _unconstrained mutability_ (any code that can call the _get_ method of a thread-local variable can call the _set_ method of that variable, even if an object in a thread-local variable is immutable)
* _unbounded lifetime_ (once a copy of a thread-local variable is set via the _set_ method, the value is retained for the lifetime of the thread, or until code in the thread calls the _remove_ method)
* _expensive inheritance_ (each child thread copies, not reuses, _inheritable-thread-local variables_ of the parent thread)

Sometimes, _scoped values_ may be a better alternative to thread-local variables. Unlike a thread-local variable, a scoped value is written once, is available only for a bounded context, and is inherited in a _structured concurrency_ scope.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-thread-local-variables-carefully-or-switch-to-scoped-values)


### Use synchronized blocks and methods carefully or switch to reentrant locks

To improve scalability using virtual threads, you should revise _synchronized_ blocks and methods to avoid frequent and long-running pinning (such as I/O operations). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. Alternatively, you can replace a _synchronized_ block or method with a `ReentrantLock`, which also guarantees mutually exclusive access.

<sub>Running your application with <em>-Djdk.tracePinnedThreads=full</em> prints a complete stack trace when a thread blocks while pinned (highlighting native frames and frames holding monitors), running with <em>-Djdk.tracePinnedThreads=short</em> prints just the problematic stack frames.</sub>

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-synchronized-blocks-and-methods-carefully-or-switch-to-reentrant-locks)


## Conclusion

Virtual threads are designed for developing high-throughput concurrent applications when a programmer can create millions of units of concurrency with the well-known `Thread` class. Virtual threads are intended to replace platform threads in those applications that spend most of their time blocked on I/O operations.

To summarize, these design features make virtual threads effective in these situations:



* virtual threads are _user-mode_ threads, so the overhead of their creation and context switching is negligible
* the Java core library has been refactored to make most of the operations non-blocking
* the virtual thread stack is much smaller and dynamically resizable

Java _virtual threads_ are a solution to achieve the same level of throughput that Golang is already demonstrating with its _goroutines_. The OpenJDK team has been working on preparing the JDK for virtual threads within the Project Loom for several years. The pre-existing blocking code in the Java core library still behaves the same way, but OS threads are not actually blocked.

For your applications to benefit from using virtual threads, you need to follow the mentioned guidelines. Also, third-party libraries used in your applications must be refactored by their owners or patched to become compatible with virtual threads.

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-java-virtual-threads).
