# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ are lightweight threads designed to increase throughput in concurrent applications. Pre-existing Java threads were based on operating system (OS) threads that proved insufficient to meet the demands of modern concurrency. Applications such as web servers, databases, or message brokers nowadays must serve millions of concurrent requests, but the OS and therefore the JVM cannot efficiently handle more than a few thousand threads.

Currently, programmers can either use threads as the units of concurrency and write synchronous blocking code in the _thread-per-request_ model. These applications are easier to develop, but they are not scalable because the number of OS threads is limited. Or, programmers can use other concurrent models (futures, async/await, coroutines, actors, etc.) that reuse threads without blocking them. Such solutions, while having much better scalability, are much more difficult to implement, debug, and understand.

Virtual threads developed inside Project Loom should solve this dilemma. New lightweight _virtual threads_ managed by the JVM, can be used alongside the existing heavyweight _platform threads_ managed by the OS. Programmers can create millions of virtual threads and achieve similar scalability using much simpler synchronous blocking code.

<sup>All information in this article corresponds to OpenJDK 21.</sup>


## Why virtual threads?


### Concurrency and parallelism

Before telling what virtual threads are, we need to explain how they can increase the throughput of concurrent applications. To begin with, it is worth explaining what _concurrency_ is and how it differs from _parallelism_.

Parallelism is a technique to accelerate a single task by splitting it into cooperating subtasks scheduled on multiple computing resources. The main performance parameter in parallel applications is _latency_ (duration of task processing in time units). An example of a parallel code is the Fork/Join framework.

Concurrency, in contrast, is a technique to schedule largely independent tasks to multiple computing resources. The main performance parameter in concurrent applications is _throughput_ (number of tasks processed per time unit). An example of a concurrent application is a server.


### Little's Law

In mathematical theory, [Little's Law](https://www.google.com/search?q=Little%27s+Law) is a theorem that describes the behavior of concurrent systems. A _system_ means some arbitrary boundary in which tasks (customers, transactions, or requests) arrive, spend time inside, and then leave. The theorem applies to a _stable_ system, where tasks enter and leave at the same rate (rather than accumulating in an unbounded queue). Also, tasks should not be interrupted and not interfere with each other. (All the variables in the theorem refer to long-term averages in an arbitrary time period, within which probabilistic fluctuations are irrelevant).

![Little's Law](/images/Little's Law.svg)

The theorem states that the number _L_ of tasks being concurrently handled (_capacity_) in such a system is equal to the arrival rate _λ_ (_throughput_) multiplied by the time _W_ that a task spends in the system (_latency_):

L = λ*W

Additionally, since Little's Law applies to any system with arbitrary boundaries, it applies to any subsystem of that system.


### Servers are concurrent systems

Little's Law can be applied to the servers as well. A server can be thought of as a system that processes requests and contains several subsystems (CPU, memory, disc, network, etc). In servers, as concurrent applications, we are primarily interested in increasing throughput rather than reducing latency.

λ = L/W

In properly designed servers, requests do not interfere with each other and slow down the server only slightly. Thus, the latency of each request depends on the inherent properties of the server and can be considered constant. Simplistically, if we want to increase the throughput of a server, we must increase its capacity.

Next, we are interested in processing requests in the CPU subsystem, since in most servers the CPU is the bottleneck. When we move to the CPU subsystem, we also move from requests to threads as units of concurrency. We will consider servers designed on the thread-per-request model.

λ = L<sub>CPU</sub>/W<sub>CPU</sub> = N<sub>cores</sub>/W<sub>CPU</sub>

On most servers, threads process I/O-bound tasks. These threads use the CPU for a short time and most of the time wait for blocking operations to complete. When a waiting thread is blocked, the scheduler can switch the CPU core to execute another active thread. Simplified, if the server uses the CPU for only 1/N of the request execution time, then a single CPU core can process N requests simultaneously.

N<sub>threads</sub> = λ*W = (L<sub>CPU</sub>/W<sub>CPU</sub>)*W = L<sub>CPU</sub>*(W/W<sub>CPU</sub>) = N<sub>cores</sub>*(W/W<sub>CPU</sub>)

For example, a CPU has 24 cores and the total request latency is W=100 ms. If a request spends W<sub>CPU</sub>=10 ms, then to fully utilize the CPU you need to have 240 threads. If a request requires much less computing resources and spends W<sub>CPU</sub>=0.1 ms, then to fully utilize the CPU you already need to have 24000 threads. However, a mainstream OS cannot support that number of active threads, mainly because their stack is too large. (A consumer-grade computer nowadays rarely supports more than 5000 active threads). Therefore, very often the server's computational resources are underutilized when performing I/O-bounded requests.


### Asynchronous programming is complicated

Thus, if servers are designed on the thread-per-request model, they will under-utilize their computing resources.To fully utilize all computational resources, it is necessary to abandon the _thread-per-request_ model. Typically the _asynchronous pipeline_ model is used instead, where tasks at different stages are executed on different worker threads from a thread pool.

But this solution also has serious problems. The entire Java platform is designed on using threads as units of concurrency. In the Java programming language, operators (branches, cycles, method calls) are called in a thread. Programmers are forced to use completely different control flows in various asynchronous frameworks. Exception has a stack trace that shows  where in a thread the error occurred. In asynchronous frameworks, stack traces are almost useless, because they contain the context of a different thread than the one in which the error occurred. The Java tools (debuggers, profilers) have limited use in asynchronous code, because they are also based on the thread as the execution context. Programmers lose all those advantages when they abandon the thread-per-request model in favor of an asynchronous model.


### User-mode threads are the solution

Thus, programmers were faced with a dilemma: waste money on hardware due to its under-utilization or waste money on development due to a programming style that is disharmonious with the design of the Java platform. The solution that the Loom Project team has chosen is to implement user-mode threads (but not coroutines) similar to those used in Erlang and Go. This solution provides an excellent concurrent capacity because this is what Little's Law requires to achieve high throughput.

These lightweight threads were named _virtual threads_ by analogy to _virtual memory_. This name suggests that virtual threads are numerous and cheap thread-like entities that make good use of computational resources. Virtual threads are implemented by the JVM (instead of the OS kernel), which manages their stack at a lower granularity than the OS can. So instead of a few thousand threads at most, programmers can have millions of threads in the single OS process. This allows programmers to write simple and scalable concurrent code in the thread-per-request model, which is the only approach that is harmonious with the Java platform.


## Platform threads and virtual threads

A thread is a _thread of execution_, that is an independently scheduled execution unit that belongs to an OS process. In a simplified form, a thread has a program counter and stack. The `Thread` class is a thin wrapper in the JVM to manage the OS threads. There are two kinds of threads: platform threads and virtual threads.


### Platform threads

_Platform threads_ are _kernel-mode_ threads mapped one-to-one to _kernel-mode_ OS threads. The OS schedules OS threads and hence platform threads. The OS affects the thread creation time and the context switching time, as well as the number of platform threads. Platform threads usually have a large, fixed-size stack allocated in a process _stack segment_ with page granularity. (For the JVM running on Linux x64 the default stack size is 1 MB, so 1000 OS threads require 1 GB of stack memory). So, the number of available platform threads is limited to the number of OS threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of a limited resource.


### Virtual threads

_Virtual threads_ are _user-mode_ threads mapped many-to-many to _kernel-mode_ OS threads. Virtual threads are scheduled by the JVM, rather than the OS. A virtual thread is a regular Java object, so the thread creation time and context switching time are negligible. The stack of virtual threads is much smaller than for platform threads and is dynamically sized. When a virtual thread is inactive, its stack is stored in the JVM heap. Thus, the number of virtual threads does not depend on the limitations of the OS.

>Virtual threads are suitable for executing tasks that spend most of the time blocked and are not intended for long-running CPU-intensive operations.

A summary of the quantitative differences between platform and virtual streams:


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
  <tr>
   <td>amount
   </td>
   <td>&lt; 5000
   </td>
   <td>millions
   </td>
  </tr>
</table>


The implementation of virtual threads consists of two parts: continuation and scheduler.

Continuations are a sequential code that can suspend itself and later be resumed. When a continuation suspends, it saves its content and passes control outside. When a continuation is resumed, control returns to the last suspending point with the previous context.

By default, virtual threads use a work-stealing `ForkJoinPool` scheduler. This scheduler is pluggable, and any other scheduler that implements the `Executor` interface can be used instead. The schedulers do not even need to know that they are scheduling continuations. From their view, they are ordinary tasks that implement the `Runnable` interface. The scheduler executes virtual threads on a pool of several platform threads used as _carrier threads_. By default, their initial number is equal to the number of available _hardware threads_, and their maximum number is 256.

When a virtual thread calls a blocking I/O method, the scheduler performs the following actions:



* _unmounts_ the virtual thread from the carrier thread;
* suspends the continuation and saves its content;
* start a non-blocking I/O operation in the OS kernel;
* the scheduler can execute another virtual thread on the same carrier thread.

When the I/O operation completes in the OS kernel, the scheduler performs the opposite actions:



* restores the content of the continuation and resumes it;
* waits until a carrier thread is available;
* _mounts_ the virtual thread to the carrier thread.

To provide this behavior, most of the blocking operations in the Java standard library (mainly I/O and synchronization constructs from the _java.util.concurrent_ package) have been refactored. However, some operations do not yet support this feature and _capture_ the carrier thread instead. This behavior can be caused by current limitations of the OS or of the JDK. The capture of an OS thread is compensated by temporarily adding additional carrier thread to the scheduler.

A virtual thread also cannot be unmounted during some blocking operations when it is _pinned_ to its carrier. This occurs when a virtual thread executes a _synchronized_ block/method, a _native method_, or a _foreign function_. During pinning, the scheduler does not create an additional carrier thread, so frequent and prolonged pinning may degrade scalability.


## How to use virtual threads

Virtual threads are instances of the nonpublic `VirtualThread` class, which is a subclass of the `Thread` class.

![thread class diagram](/images/thread_class_diagram.png)

The `Thread` class has public constructors and the inner `Thread.Builder` interface for creating and starting threads. For backward compatibility, all public constructors of the `Thread` class can create only platform threads for now. Virtual threads are instances of a class that does not have public constructors, so the only way to create virtual threads is to use a builder. A similar builder exists for creating platform threads.

The `Thread` class has new methods to handle virtual threads:


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
   <td><em>static Thread</em>
   </td>
   <td><em>startVirtualThread(Runnable)</em>
   </td>
   <td>Creates a virtual thread to execute a task and schedules it to execute. 
   </td>
  </tr>
</table>


There are four ways to create and start virtual threads:



* the thread builder
* the static factory method
* the thread factory
* the executor service

The virtual _thread builder_ allows you to create a virtual thread with all available parameters: name, _inheritable-thread-local variables_ inheritance flag, uncaught exception handler, and `Runnable` task. (Note that the virtual threads are _daemon_ threads and have a fixed thread priority that cannot be changed).


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("thread %s failed with exception %s", t, e));

Thread thread = builder.unstarted(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("virtual thread", thread.getName());
assertTrue(thread.isDaemon());
assertEquals(5, thread.getPriority());
```


<sub>In the platform thread builder, you can specify additional parameters: thread group, <em>daemon</em> flag, priority, and stack size. </sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a `Runnable` task. (Note that by default, the virtual thread name is empty).


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
thread.join();

assertTrue(thread.isVirtual());
assertEquals("", thread.getName());
```


The _thread factory_ allows you to create virtual threads by specifying a `Runnable` task to the instance of the `ThreadFactory` interface. The parameters of virtual threads are specified by the current state of the thread builder from which this factory is created. (Note that the thread factory is thread-safe, but the thread builder is not).


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread");

ThreadFactory threadFactory = builder.factory();

Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertTrue(thread.isVirtual());
assertEquals("virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());
```


The _executor service_ allows you to execute `Runnable` and `Callable` tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```



## How to properly use virtual threads

The Project Loom team had a choice whether to make virtual threads a separate class in the _strand_ hierarchy or inherit them from the `Thread` class. They have chosen the second option, and now existing concurrent code can, with little or no change, use virtual threads. But as a result of this compromise, some features are widely used for platform threads (for example, thread pools and thread-local variables) but useless for virtual threads. It is the programmer's responsibility to know and avoid known pitfalls.


### Do not use virtual threads for CPU-bound tasks

The OS scheduler for platform threads is preemptive. The OS scheduler uses _time slices_ to periodically suspend and resume platform threads. Thus, multiple platform threads executing CPU-bound tasks will eventually show progress even if none of them explicitly yields.

Currently, virtual threads can be preempted only at any call to the Java standard library. Their specifications allow using time-slice preemption, but the default scheduler does not use this (because the Project Loom team has no reason to do that in the current implementation). So now a virtual thread can be suspended, only if it is blocked on an I/O or other supported operation. If you run a virtual thread with a CPU-bound task, this thread monopolizes the OS thread until the task is completed, and other virtual threads experience _starvation_.


### Write blocking synchronous code in the thread-per-request style

Blocking platform threads is costly because it wastes a limited resource. Various asynchronous frameworks use tasks as more fine-grained units of concurrency instead of threads. These frameworks reuse threads without blocking them and indeed achieve higher application scalability. The price for this is a significant increase in development complexity. Since much of the Java platform assumes that execution context is contained in a thread, all that context is lost once we decouple tasks from threads.

In contrast, blocking virtual threads is low-cost, and moreover, it is their main design feature. While the blocked virtual thread is waiting for the operation to complete, the carrier thread and the underlying OS thread are actually not blocked. This allows programmers to create simpler yet efficient concurrent code in the thread-per-task style.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#write-blocking-synchronous-code-in-the-thread-per-task-style)


### Do not pool virtual threads

Creating a platform thread is a rather time-consuming process because it requires the creation of an OS thread. Thread pool executors are designed to reduce this time by reusing threads between executing multiple tasks. They contain a pool of worker threads to which `Runnable` and `Callable` tasks are passed through a blocking queue.

Unlike creating platform threads, creating virtual threads is a fast process. Therefore, there is no need to create a pool of virtual threads. If the application requires an `ExecutorService` instance, use a specially designed implementation for virtual threads, which is returned from the static factory method `Executors.newVirtualThreadPerTaskExecutor()`. This executor does not use a thread pool and creates a new virtual thread for each submitted task. In addition, this executor itself is lightweight, so you can create and close it at any desired code within the _try-with-resources_ block.

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#do-not-pool-virtual-threads)


### Use semaphores instead of fixed thread pools to limit concurrency

The main purpose of thread pools is to reuse threads between executing multiple tasks. When a task is submitted to a thread pool, it is inserted into a queue. The task is retrieved from the queue by a worker thread for execution. An additional purpose of using thread pools with a _fixed number_ of worker threads can be to limit the concurrency of a particular operation. They can be used when an external resource cannot process more than a predefined number of concurrent requests.

However, since there is no need to reuse virtual threads, there is no need to use any thread pools for them. Instead, you should use a `Semaphore` with the same number of permits to limit concurrency. Just as a thread pool contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/ThreadPoolExecutor.java#L454) of tasks, a semaphore contains a [queue](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/concurrent/locks/AbstractQueuedSynchronizer.java#L319) of threads blocked on it.

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

To improve scalability using virtual threads, you should revise _synchronized_ blocks and methods to avoid frequent and long-running pinning (such as I/O operations). Pinning is not a problem if such operations are short-lived (such as in-memory operations) or infrequent. Alternatively, you can replace a _synchronized_ block or method with a `ReentrantLock`, that also guarantees mutually exclusive access.

<sub>Running your application with <em>-Djdk.tracePinnedThreads=full</em> prints a complete stack trace when a thread blocks while pinned (highlighting native frames and frames holding monitors), running with <em>-Djdk.tracePinnedThreads=short</em> prints just the problematic stack frames.</sub>

[code examples](https://github.com/aliakh/demo-java-virtual-threads/blob/main/src/test/java/virtual_threads/part2/readme.md#use-synchronized-blocks-and-methods-carefully-or-switch-to-reentrant-locks)


## Conclusion

Virtual threads are designed for developing high-throughput concurrent applications when a programmer can create millions of units of concurrency with the well-known `Thread` class. Virtual threads are intended to replace platform threads in those applications that spend most of their time blocked on I/O operations.

Implementing virtual threads while keeping platform threads to the same `Thread` class hierarchy is a compromise. All existing concurrent code can use virtual threads with minimal changes. It is the programmer's responsibility to use virtual threads correctly. This mainly concerns _thread pools_ and _thread-local variables_. Instead of thread pools, you need to create a virtual thread for each task. Thread-local variables should be used with caution and, if possible, replaced with _scoped values_. Particular attention should be paid to the _structural concurrency_ that was implemented in the Project Loom along with virtual threads. This is a new technique to manage numerous virtual threads as a single unit of work.

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-java-virtual-threads).
