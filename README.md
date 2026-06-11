# Concurrency Kata

A collection of concurrent data structure implementations in Java, designed to explore concurrency patterns, thread safety, and performance trade-offs.

## Overview

This project contains implementations of various data structures with both single-threaded and thread-safe variants, serving as a learning resource for understanding concurrent programming in Java.

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

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```

All tests use JUnit 5 and include both single-threaded and concurrent test scenarios.

## Design Principles

- **Single-threaded variants**: Focus on algorithmic correctness and efficiency
- **Thread-safe variants**: Use appropriate synchronization mechanisms (locks, CAS, concurrent collections)
- **Comprehensive Javadoc**: Each class includes detailed documentation on data structures, concurrency model, design decisions, and thread safety
- **Deterministic testing**: Time-dependent components use injectable Clock for testability

## Requirements

- Java 17+
- Maven 3.x
- Lombok (for boilerplate reduction)

## License

This project is provided as a learning resource.
