# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ (previously known as _fibers_) are lightweight threads designed to develop _high-throughput_ concurrent applications. Pre-existing Java threads were based on operating system (OS) threads that proved insufficient to meet the demands of modern concurrency. Applications such as web servers, databases, or message brokers nowadays must serve millions of concurrent requests, but the OS and therefore the JVM cannot efficiently handle more than a few thousand threads.

Currently, programmers can either use threads as the units of concurrency and write synchronous blocking code. These applications are easy to develop, but they are not scalable, because the number of OS threads is limited. Or, they can use other concurrent models (futures, async/await, coroutines, actors, etc.) that reuse threads without blocking them. Such solutions, while showing much better scalability, are much more difficult to write, debug, and understand.

New lightweight _virtual threads_ managed by the JVM, can be used alongside the existing heavyweight _platform threads_ managed by the OS. Programmers can create millions of virtual threads and get similar scalability using much simpler synchronous blocking code.

<sub>All information in this article corresponds to the reference implementation in OpenJDK.</sub>


## Platform threads and virtual threads

A thread is a _thread of execution_ in a program, that is an independently scheduled execution unit that belongs to a process in the OS. This entity has a program counter and stack. The _Thread_ class is a wrapper in the JVM to manage threads of execution in the OS. This class has fields, methods, and constructors. There are two kinds of threads, platform threads and virtual threads.


### Platform threads

_Platform threads_ are _kernel-mode_ threads mapped one-to-one to _kernel-mode_ OS threads. The OS schedules OS threads and therefore, platform threads. The OS affects the thread creation time and the _context switching_ time, as well as the number of platform threads. Platform threads usually have a large, fixed-size stack allocated in a process _stack segment_. (For the JVM running on Linux x64 the default stack size is 1 MB, so 1000 OS threads require 1 GB of stack memory). So, the number of available platform threads is limited to the number of OS threads. A JVM on a consumer-grade computer can support no more than a few thousand platform threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of a limited resource.


### Virtual threads

_Virtual threads_ are _user-mode_ threads mapped many-to-many to _kernel-mode_ OS threads. Virtual threads are scheduled by the JVM, rather than the OS. A virtual thread is a regular Java object, so the thread creation time and context switching time are negligible. The stack size of virtual threads is much smaller than for platform threads and is dynamically sized. Thus, the number of virtual threads does not depend on the limitations of the OS. A JVM on the same computer can support millions of virtual threads.

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
   <td>metadata size
   </td>
   <td>> 2 KB
   </td>
   <td>200-300 B
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

Continuation (also known as _delimited continuation_ or _coroutine_) is a sequential code that may suspend execution at some point and pass control outside. When a continuation is resumed, control returns to the last suspending point with the previous execution context. Continuation is a low-level construct implemented by the internal _jdk.internal.vm.Continuation_ class and developers should not use them directly.

The scheduler manages the suspending and resuming of the coroutines. The scheduler is pluggable and a _ForkJoinPool_ FIFO executor is used by default.This scheduler is optimized for transactions that are typically short-lived and often blocked.

The virtual threads scheduler contains a few platform threads used as _carrier threads_. By default, their initial number is equal to the number of available processors, and their maximum number is 256.

When a virtual thread calls a blocking I/O method in the Java core library, the scheduler performs the following actions:



* _unmounts_ the virtual thread from the carrier thread;
* suspends the continuation and saves its content;
* copies the stack frames of the virtual thread from the stack of the carrier thread to the heap;
* start a non-blocking I/O operation in the OS kernel;
* the scheduler can schedule another virtual thread on the same carrier thread.

When the I/O operation completes in the OS kernel, the scheduler performs the opposite actions:



* copies the stack frames of the virtual thread from the heap to the stack of the carrier thread;
* restores the content of the continuation and resumes it;
* waits until a carrier thread is available;
* _mounts_ the virtual thread to the carrier thread.

To provide this behavior, most of the blocking operations in the Java core library (mainly I/O and _java.util.concurrent_) have been refactored. However, some operations do not yet support this feature and instead _capture_ the carrier thread. This behavior can be caused by limitations of the OS (which affects many file system operations) or of the JDK (such as with the _Object.wait()_ method). The capture of an OS thread is compensated by temporarily adding a platform thread to the JVM scheduler.

A virtual thread also cannot be unmounted during blocking operations when it is _pinned_ to its carrier. This occurs when a virtual thread executes a _synchronized_ block/method, a _native method,_ or a _foreign function_. During pinning, the scheduler does not create an additional carrier thread, so frequent and long-lived pinning may worsen the scalability.


## How to use virtual threads

The _Thread_ class has public constructors and the inner _Thread.Builder_ interface for creating and starting both platform and virtual threads.

For backward compatibility, the public constructors of the _Thread_ class can create only platform threads. Virtual threads are instances of the nonpublic class _VirtualThread_ that cannot be instantiated directly. So, to create virtual threads you should use a builder that implements the _Thread.Builder.OfVirtual_ sub-interface. By analogy, to create platform threads you should use a similar builder that implements the _Thread.Builder.OfPlatform_ sub-interface. These builders are returned from static factory methods _Thread.ofVirtual()_ and _Thread.ofPlatform()_ respectively.

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

Thread thread = builder.unstarted(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("java.lang.VirtualThread", thread.getClass().getName());
assertEquals("a virtual thread", thread.getName());
assertTrue(thread.isDaemon());
assertEquals(5, thread.getPriority());
```


<sub>In the platform thread builder, you can specify additional parameters: thread group, <em>daemon</em> flag, priority, and stack size. </sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a _Runnable_ task. (Note that by default, the virtual thread name is empty).


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
thread.join();

assertTrue(thread.isVirtual());
assertEquals("java.lang.VirtualThread", thread.getClass().getName());
assertEquals("", thread.getName());
```


The _thread factory_ allows you to create virtual threads by specifying a _Runnable_ task to the instance of the _ThreadFactory_ interface. The parameters of virtual threads are specified by the current state of the thread builder from which this factor is created. (Note that the thread factory is thread-safe, but the thread builder is not).


```
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread");

ThreadFactory threadFactory = builder.factory();

Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("java.lang.VirtualThread", thread.getClass().getName());
assertEquals("a virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());
```


The _executor service_ allows you to execute _Runnable_ and _Callable_ tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```



## How to properly use virtual threads

Virtual threads almost entirely support the API and semantics of the pre-existing _Thread_ class. Their implementation effectively shifts the task of handling OS thread blocking from the programmer to the JVM. But to get a real performance gain and avoid known pitfalls, the programmer must know the details of this implementation.


### Do not use virtual threads for CPU-bound tasks

The platform threads are preemptive. The OS scheduler uses _time slices_ to periodically suspend and resume multiple platform threads. Thus, multiple platform threads executing CPU-bound tasks will eventually show progress even if none of them explicitly returns control to the OS scheduler using the _Thread.yield()_ method.

Virtual threads are non-preemptive (see “Modern Operating Systems”, 4th edition, by Andrew S. Tanenbaum and Herbert Bos, 2015). A virtual thread suspends only if it is blocked on an I/O or other supported operation. If you run a virtual thread with a CPU-bound task, this thread monopolizes the OS thread until the task is completed, and other virtual threads experience _starvation_. You should currently only use platform threads for CPU-bound tasks.


### Write blocking synchronous code in the thread-per-task style

Blocking platform threads is costly because it wastes a limited resource. Various asynchronous frameworks use tasks as more fine-grained units of concurrency instead of threads. These frameworks reuse threads without blocking them and indeed achieve higher application scalability. The price to pay for this is a significant increase in the complexity of application development. Since much of the Java platform assumes that execution context is contained in a thread, all that context is lost once we decouple tasks from threads. Debugging and profiling are difficult in such asynchronous applications, and stack traces no longer provide useful information.

In contrast, blocking virtual threads is low-cost, and moreover, it is their main design feature. While the blocked virtual thread is waiting for the operation to complete, the carrier thread and the underlying OS thread are actually not blocked. This allows programmers to create simpler yet efficient concurrent code in the thread-per-task style. In addition, virtual threads almost fully support the syntax and semantics of the well-known _Thread_ class.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#write-blocking-synchronous-code-in-the-thread-per-task-style)


### Do not pool virtual threads

Creating a platform thread is a rather time-consuming process because it requires the creation of an OS thread. Thread pool executors are designed to reduce this time by reusing threads between task executions. They contain a pool of pre-created worker threads to _Runnable_ and _Callable_ tasks are passed through a blocking queue.

Unlike creating platform threads, creating virtual threads is a fast process. Therefore, there is no need to create a pool of virtual threads. If the program requires an _ExecutorService_ instance, use a specially designed implementation for virtual threads, which is returned from the static factory method _Executors.newVirtualThreadPerTaskExecutor()_. This executor does not use a thread pool and creates a new virtual thread for each submitted task. In addition, this executor itself is lightweight, so you can create and close it at any desired code within the _try-with-resources_ block.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#do-not-pool-virtual-threads)


### Use semaphores instead of fixed thread pools to limit concurrency

The main purpose of thread pools is to reuse threads between executing multiple tasks. When tasks are submitted to a thread pool, they are inserted into a queue. Tasks are retrieved from the queue by worker threads for execution. An additional purpose of using thread pools with a _fixed number_ of worker threads can be to limit the concurrency of a particular operation. They can be used when an external resource cannot process more than a predefined number of concurrent requests.

However, since there is no need to reuse virtual threads, there is no need to use any thread pools for them. Instead, it is better to use a _Semaphore_ with the same number of permits to limit concurrency. Just as a thread pool contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/ThreadPoolExecutor.java#L454) of tasks, a semaphore contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/locks/AbstractQueuedSynchronizer.java#L319) of threads blocked on it.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-semaphores-instead-of-fixed-thread-pools-to-limit-concurrency)


### Use thread-local variables carefully or switch to scoped values

To achieve better scalability of virtual threads, you should reconsider using _thread-local variables_ and _inheritable-thread-local variables_. Thread-local variables provide each thread with its copy of a variable, which is set to a value independent of the values set by other threads. A thread-local variable works as an implicit, thread-bound parameter and allows passing data from a caller to a callee through a sequence of intermediate methods.

Virtual threads support thread-local behavior in the same way as platform threads. Because virtual threads can be very numerous, the following features of thread-local variables can have a more significant negative effect:



* _unconstrained mutability_ (any code that can call the _get_ method of a thread-local variable can call the _set_ method of that variable, even if an object in a thread-local variable is immutable)
* _unbounded lifetime_ (once a copy of a thread-local variable is set via the _set_ method, the value is retained for the lifetime of the thread, or until code in the thread calls the _remove_ method)
* _expensive inheritance_ (each child thread copies, not reuses, _inheritable-thread-local variables_ of the parent thread)

Sometimes, _scoped values_ may be a better alternative to thread-local variables. Unlike a thread-local variable, a scoped value is written once, is available only for a bounded context, and is inherited in a _structured concurrency_ scope.

<sub>Scoped values are a preview feature in Java 20 and have not been released at the time of writing.</sub>

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-thread-local-variables-carefully-or-switch-to-scoped-values)


### Use synchronized blocks and methods carefully or switch to reentrant locks

To improve scalability using virtual threads, you should revise _synchronized_ blocks and methods to avoid frequent and long-running pinning (such as I/O operations). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. Alternatively, you can replace _synchronized_ block or method with a _ReentrantLock_, which also guarantees mutually exclusive access.

Running  your application with _-Djdk.tracePinnedThreads=full_ prints a complete stack trace when a thread blocks while pinned (highlighting native frames and frames holding monitors), running with _-Djdk.tracePinnedThreads=short_ prints just the problematic stack frames.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-synchronized-blocks-and-methods-carefully-or-switch-to-reentrant-locks)


## Conclusion

Virtual threads are designed for developing high-throughput concurrent applications when a programmer can create millions of units of concurrency with the well-known _Thread_ class. Virtual threads are intended to replace platform threads in those applications that spend most of their time blocked on I/O operations.

To summarize, these design features make virtual threads effective in these situations:



* virtual threads are _user-mode_ threads, so the overhead of their creation and _context switching_ is negligible
* the Java core library has been refactored to make most of the operations non-blocking
* the virtual thread stack is much smaller and dynamically resizable

Java _virtual threads_ are a solution to achieve the same level of throughput that Golang is already demonstrating with its _goroutines_. Considerable work has been done in the Java core library to make it compatible with virtual threads: refactored to make I/O and concurrent operations non-blocking, as well as getting rid of thread-local variables. Its pre-existing blocking code still behaves the same, but OS threads are not actually blocked. For your applications to benefit from using virtual threads, you need to follow known guidelines. Also, third-party dependencies used in your applications must be refactored by their owners or patched to become compatible with virtual threads.

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-java-virtual-threads).
