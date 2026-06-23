# Data Structures Foundry

A from-scratch collection of data structures, algorithms, and concurrency primitives in Java — built to explore internals, thread safety, and performance trade-offs.

## Overview

This project contains hand-built implementations spanning core data structures, probabilistic structures, and system-design building blocks, many with both single-threaded and thread-safe variants. It serves as a learning resource and portfolio for understanding how these structures work under the hood.

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Data Structures](#data-structures)
  - [Deduplicator](#deduplicator)
  - [Hit Counter](#hit-counter)
  - [LRU Cache](#lru-cache)
  - [Max Stack](#max-stack)
  - [Top K Frequent Elements](#top-k-frequent-elements)
  - [Rate Limiter](#rate-limiter)
- [Building](#building)
- [Running Tests](#running-tests)
  - [Test Strategy](#test-strategy)
- [Design Principles](#design-principles)
- [Requirements](#requirements)
- [License](#license)

## Project Structure

All sources live under `src/main/java/dev/aahmedlab`, grouped by package. Each package maps to a section in [Data Structures](#data-structures).

| Package | Classes | Section |
| --- | --- | --- |
| [`dedup`](src/main/java/dev/aahmedlab/dedup) | [`Deduplicator`](src/main/java/dev/aahmedlab/dedup/Deduplicator.java), [`ConcurrentDeduplicator`](src/main/java/dev/aahmedlab/dedup/ConcurrentDeduplicator.java) | [Deduplicator](#deduplicator) |
| [`hitcounter`](src/main/java/dev/aahmedlab/hitcounter) | [`HitCounter`](src/main/java/dev/aahmedlab/hitcounter/HitCounter.java), [`ConcurrentHitCounter`](src/main/java/dev/aahmedlab/hitcounter/ConcurrentHitCounter.java), [`Hit`](src/main/java/dev/aahmedlab/hitcounter/Hit.java) | [Hit Counter](#hit-counter) |
| [`lru`](src/main/java/dev/aahmedlab/lru) | [`LRU`](src/main/java/dev/aahmedlab/lru/LRU.java), [`ConcurrentLRU`](src/main/java/dev/aahmedlab/lru/ConcurrentLRU.java), [`Node`](src/main/java/dev/aahmedlab/lru/Node.java) | [LRU Cache](#lru-cache) |
| [`maxstack`](src/main/java/dev/aahmedlab/maxstack) | [`TwoStackMaxStack`](src/main/java/dev/aahmedlab/maxstack/TwoStackMaxStack.java), [`PopMaxStack`](src/main/java/dev/aahmedlab/maxstack/PopMaxStack.java), [`ConcurrentMaxStack`](src/main/java/dev/aahmedlab/maxstack/ConcurrentMaxStack.java), [`DoubleLinkedList`](src/main/java/dev/aahmedlab/maxstack/DoubleLinkedList.java), [`Node`](src/main/java/dev/aahmedlab/maxstack/Node.java) | [Max Stack](#max-stack) |
| [`frequentelement`](src/main/java/dev/aahmedlab/frequentelement) | [`TopKFrequentElements`](src/main/java/dev/aahmedlab/frequentelement/TopKFrequentElements.java) | [Top K Frequent Elements](#top-k-frequent-elements) |
| [`ratelimiter`](src/main/java/dev/aahmedlab/ratelimiter) | [`TokenBucket`](src/main/java/dev/aahmedlab/ratelimiter/TokenBucket.java), [`ConcurrentTokenBucket`](src/main/java/dev/aahmedlab/ratelimiter/ConcurrentTokenBucket.java), [`LockFreeTokenBucket`](src/main/java/dev/aahmedlab/ratelimiter/LockFreeTokenBucket.java), [`FixedWindow`](src/main/java/dev/aahmedlab/ratelimiter/FixedWindow.java), [`ConcurrentFixedWindow`](src/main/java/dev/aahmedlab/ratelimiter/ConcurrentFixedWindow.java), [`SlidingWindowLog`](src/main/java/dev/aahmedlab/ratelimiter/SlidingWindowLog.java), [`SlidingWindowCounter`](src/main/java/dev/aahmedlab/ratelimiter/SlidingWindowCounter.java) | [Rate Limiter](#rate-limiter) |

## Data Structures

### Deduplicator
- **Deduplicator**: Single-threaded TTL-based deduplication using a HashMap and ring of HashSet buckets
- **ConcurrentDeduplicator**: Thread-safe variant using ConcurrentHashMap and concurrent Set buckets

### Hit Counter
- **HitCounter**: Single-threaded sliding-window hit counter using a circular array
- **ConcurrentHitCounter**: Thread-safe variant using AtomicReference and AtomicInteger with lock-free CAS

### LRU Cache
- **LRU**: Single-threaded LRU cache with O(1) get/put using HashMap and doubly linked list
- **ConcurrentLRU**: Thread-safe variant using ReentrantLock for synchronization

### Max Stack
- **TwoStackMaxStack**: Single-threaded max stack using two-stack technique for O(1) push, pop, peek, getMax
- **PopMaxStack**: Single-threaded max stack with O(log n) popMax using DoubleLinkedList and TreeMap
- **ConcurrentMaxStack**: Thread-safe max stack (same implementation as PopMaxStack, synchronized)

### Top K Frequent Elements
- **TopKFrequentElements**: Stateless utility class with two approaches:
  - Bucket sort: O(n) time complexity
  - Min-heap: O(n log k) time complexity

### Rate Limiter
- **TokenBucket**: Single-threaded token bucket with time-based refill
- **ConcurrentTokenBucket**: Thread-safe token bucket using a single intrinsic lock
- **LockFreeTokenBucket**: Thread-safe token bucket using AtomicReference with lock-free CAS
- **FixedWindow**: Single-threaded fixed-window limiter (not thread-safe)
- **ConcurrentFixedWindow**: Thread-safe fixed-window limiter using a single intrinsic lock
- **SlidingWindowLog**: Thread-safe sliding-window-log limiter; exact accounting via a timestamp deque
- **SlidingWindowCounter**: Thread-safe sliding-window-counter limiter; approximate, O(1) memory using weighted previous/current window counts

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```

All tests use JUnit 5 and include both single-threaded and concurrent test scenarios.

### Test Strategy

Each implementation is verified with a layered set of scenarios:

- **Input validation**: Constructors reject non-positive capacity or window/refill values (`IllegalArgumentException`).
- **Functional correctness**: Initial state, exhausting capacity, the boundary case of capacity one, and time-based behavior (window reset/slide or token refill via short `Thread.sleep` pauses).
- **Concurrency safety** (thread-safe variants only):
  - *Capacity is never exceeded*: many threads hammer a shared limiter; the total allowed equals the configured capacity.
  - *Same-time bursts*: a `CountDownLatch` releases all threads simultaneously, repeated via `@RepeatedTest` to surface race conditions.
  - *No lost updates*: every request is accounted for as either allowed or denied (allowed + denied == total requests).
  - *Parameterized sweeps*: `@ParameterizedTest` runs the same concurrent checks across a range of capacities.
- **Single-threaded variants** (e.g. `FixedWindow`) are intentionally tested without concurrency assertions, since they make no thread-safety guarantees.

## Design Principles

- **Single-threaded variants**: Focus on algorithmic correctness and efficiency
- **Thread-safe variants**: Use appropriate synchronization mechanisms (locks, CAS, concurrent collections)
- **Comprehensive Javadoc**: Each class includes detailed documentation on data structures, concurrency model, design decisions, and thread safety
- **Deterministic testing**: Time-dependent components use injectable Clock for testability

## Requirements

- Java 17+
- Maven 3.x
- Lombok (for boilerplate reduction)
- Spotless (for code formatting with google-java-format)

## License

This project is provided as a learning resource.
