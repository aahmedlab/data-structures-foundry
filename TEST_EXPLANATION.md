# Test Cases - Detailed Explanation

## Overview

The `ConcurrentHitCounter` is a lock-free, thread-safe implementation of a hit counter that uses **atomic operations** and **Compare-And-Set (CAS)** to handle concurrent access without traditional locks. This test suite validates both functional correctness and thread-safety guarantees.

## Implementation Key Concepts

### 1. **Lock-Free Concurrency**
- Uses `AtomicReference<Hit>[]` for thread-safe storage
- Uses `AtomicInteger` for thread-safe total hit counting
- Implements **optimistic locking** via CAS operations

### 2. **Compare-And-Set (CAS) Pattern**
```java
do {
    currentHit = hits[index].get();
    next = buildNext(currentHit, timestamp);
} while (!hits[index].compareAndSet(currentHit, next));
```
This loop retries the operation if another thread modified the value between read and write.

### 3. **Hash Collision Handling**
- Uses `timestamp % capacity` to map timestamps to array indices
- Multiple timestamps can map to same index (collision)
- Newer timestamp replaces older one at same index

### 4. **Expiration Logic**
- Hits expire when `current_timestamp - hit_timestamp >= capacity`
- `getHit()` method cleans expired entries atomically

---

## Test Categories

### **Category 1: Basic Functional Tests (Single-Threaded)**

These tests verify the correctness of basic operations without concurrency concerns.

#### **testInitialState()**
```java
assertEquals(0, counter.getHit(1));
```
**Purpose**: Verify that a newly created counter returns 0 hits.  
**What it tests**: Proper initialization of data structures.

#### **testSingleHit()**
```java
counter.hit(1);
assertEquals(1, counter.getHit(1));
```
**Purpose**: Verify that a single hit is recorded correctly.  
**What it tests**: Basic hit recording and retrieval.

#### **testMultipleHitsSameTimestamp()**
```java
counter.hit(1);
counter.hit(1);
counter.hit(1);
assertEquals(3, counter.getHit(1));
```
**Purpose**: Verify that multiple hits at the same timestamp increment the count.  
**What it tests**: The `buildNext()` logic that increments count for same timestamp:
```java
if (currentHit != null && currentHit.timestamp() == timestamp) {
    return new Hit(timestamp, currentHit.count() + 1);
}
```

#### **testMultipleHitsDifferentTimestamps()**
```java
counter.hit(1);
counter.hit(2);
counter.hit(3);
assertEquals(3, counter.getHit(3));
```
**Purpose**: Verify that hits at different timestamps are all counted.  
**What it tests**: Total hit counting across multiple timestamps.

#### **testCollisionReplacesOldTimestamp()**
```java
counter.hit(1);   // index 1
counter.hit(6);   // index 6 % 5 = 1 (collision!)
assertEquals(1, counter.getHit(6));
```
**Purpose**: Verify that hash collisions replace the old timestamp.  
**What it tests**: 
- Index calculation: `timestamp % capacity`
- Replacement logic when timestamps collide
- Total hit adjustment: `totalHits.addAndGet(1 - currentHit.count())`

#### **testCollisionAdjustsTotalHits()**
```java
counter.hit(1);
counter.hit(2);
assertEquals(2, counter.getHit(2));

counter.hit(6);  // collides with index 1 (timestamp 1)
assertEquals(2, counter.getHit(6));  // 2 + 1 - 1 = 2
```
**Purpose**: Verify that total hits are adjusted correctly during collisions.  
**What it tests**: The total hit adjustment logic:
```java
if (currentHit == null || currentHit.timestamp() == timestamp) {
    totalHits.incrementAndGet();
} else {
    totalHits.addAndGet(1 - currentHit.count());  // Remove old, add new
}
```

#### **testOldHitsAreExpired()**
```java
counter.hit(1);
counter.hit(2);
counter.hit(3);
assertEquals(3, counter.getHit(3));

assertEquals(2, counter.getHit(6));  // timestamp 1 expired (6-1=5 >= 5)
```
**Purpose**: Verify that old hits are expired based on the capacity window.  
**What it tests**: Expiration condition: `timestamp - currentHit.timestamp() >= capacity`

#### **testGetHitCleansExpiredEntries()**
```java
counter.hit(1);
counter.hit(2);
assertEquals(2, counter.getHit(2));

assertEquals(1, counter.getHit(6));  // ts 1 expired
assertEquals(0, counter.getHit(7));  // ts 2 expired
```
**Purpose**: Verify that `getHit()` removes expired entries and updates total count.  
**What it tests**: The cleanup loop in `getHit()`:
```java
for (int i = 0; i < capacity; i++) {
    while (true) {
        Hit currentHit = hits[i].get();
        if (currentHit == null) break;
        int diff = timestamp - currentHit.timestamp();
        if (diff < capacity) break;
        if (hits[i].compareAndSet(currentHit, null)){
            totalHits.addAndGet(-currentHit.count());
            break;
        }
    }
}
```

#### **testBoundaryExpiration()**
```java
counter.hit(1);
assertEquals(1, counter.getHit(5));  // 5 - 1 = 4 < 5, NOT expired
assertEquals(0, counter.getHit(6));  // 6 - 1 = 5 >= 5, EXPIRED
```
**Purpose**: Verify exact boundary condition for expiration.  
**What it tests**: The `>=` operator in expiration check (not just `>`).

---

### **Category 2: Parameterized Tests**

These tests run the same logic with different inputs to cover various scenarios.

#### **testDifferentCapacities()**
```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 5, 10, 100})
void testDifferentCapacities(int capacity)
```
**Purpose**: Verify the counter works correctly with different capacity values.  
**What it tests**: 
- Algorithm scales with capacity
- Edge cases like capacity=1 and large capacities

#### **testHitsAndGets()**
```java
@ParameterizedTest
@CsvSource({
    "1, 1, 1",   // 1 hit at ts 1, query at ts 1 → expect 1
    "5, 10, 0"   // 5 hits, query at ts 10 → all expired
})
```
**Purpose**: Test various combinations of hit sequences and query timestamps.  
**What it tests**: Correctness across multiple scenarios in a compact format.

---

### **Category 3: Edge Cases**

#### **testCapacityOne()**
```java
ThreadSafeHitCounter singleCounter = new ThreadSafeHitCounter(1);
singleCounter.hit(1);
assertEquals(1, singleCounter.getHit(1));

singleCounter.hit(2);  // replaces ts 1
assertEquals(1, singleCounter.getHit(2));
```
**Purpose**: Test the smallest possible capacity.  
**What it tests**: 
- Every new timestamp replaces the previous one
- Expiration window of size 1

#### **testZeroTimestamp()**
```java
counter.hit(0);
assertEquals(1, counter.getHit(0));
```
**Purpose**: Verify handling of timestamp 0 (edge case).  
**What it tests**: 
- Index calculation: `0 % capacity = 0`
- No special handling needed for zero

#### **testWrapAroundIndices()**
```java
for (int i = 0; i < CAPACITY; i++) {
    counter.hit(i);
}
counter.hit(CAPACITY);     // wraps to index 0
counter.hit(CAPACITY + 1); // wraps to index 1
```
**Purpose**: Verify correct behavior when indices wrap around.  
**What it tests**: Modulo arithmetic works correctly for large timestamps.

---

### **Category 4: Concurrency Tests**

These are the **critical tests** for thread-safety. They simulate real-world concurrent access patterns.

#### **testConcurrentHitsSameTimestamp()**
```java
int numThreads = 10;
int hitsPerThread = 100;
int timestamp = 1;

// 10 threads, each hitting timestamp 1 exactly 100 times
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        for (int j = 0; j < hitsPerThread; j++) {
            counter.hit(timestamp);
        }
    });
}

assertEquals(numThreads * hitsPerThread, counter.getHit(timestamp));
```
**Purpose**: Verify no lost updates when multiple threads hit the same timestamp.  
**What it tests**: 
- **Atomicity** of the CAS loop
- **No race conditions** in count increment
- Final count = 10 × 100 = 1000 (no hits lost)

**Why this matters**: Without proper synchronization, you could lose updates:
```
Thread A reads count=5
Thread B reads count=5
Thread A writes count=6
Thread B writes count=6  ← Lost Thread A's update! Should be 7
```

#### **testConcurrentHitsDifferentTimestamps()**
```java
int numThreads = 10;
int hitsPerThread = 50;

for (int i = 0; i < numThreads; i++) {
    final int threadId = i;
    executor.submit(() -> {
        for (int j = 0; j < hitsPerThread; j++) {
            counter.hit(threadId);  // Each thread hits different timestamp
        }
    });
}
```
**Purpose**: Verify concurrent writes to different array indices.  
**What it tests**: 
- No interference between different indices
- Total count correctness: 10 × 50 = 500

#### **testConcurrentHitsAndGets()**
```java
// 5 threads hitting, 5 threads calling getHit() simultaneously
for (int i = 0; i < numGetThreads; i++) {
    executor.submit(() -> {
        for (int j = 0; j < hitsPerThread; j++) {
            int hits = counter.getHit(timestamp);
            // Count should be between 0 and final total
            assertTrue(hits >= 0 && hits <= numHitThreads * hitsPerThread);
        }
    });
}
```
**Purpose**: Verify thread-safety when reads and writes happen concurrently.  
**What it tests**: 
- `getHit()` doesn't corrupt state during concurrent `hit()` calls
- No deadlocks or race conditions
- Readers see **eventually consistent** values

**Why this matters**: In a non-thread-safe version, concurrent reads/writes could:
- Corrupt internal data structures
- Return negative counts
- Cause infinite loops

#### **testConcurrentCollisions()**
```java
for (int i = 0; i < numThreads; i++) {
    final int threadId = i;
    executor.submit(() -> {
        for (int j = 0; j < hitsPerThread; j++) {
            int timestamp = threadId * CAPACITY;  // All map to index 0
            counter.hit(timestamp);
        }
    });
}

// All timestamps collide at index 0, only last survives
assertEquals(hitsPerThread, counter.getHit(numThreads * CAPACITY));
```
**Purpose**: Verify thread-safety when all threads write to the same array index.  
**What it tests**: 
- CAS loop handles **high contention** correctly
- Multiple retries due to concurrent CAS failures
- Last timestamp wins, count is correct

**Why this matters**: This is a **stress test** for the CAS mechanism. High contention causes many CAS failures, testing retry logic.

#### **testRepeatedTest: testConcurrentUpdatesSameIndex()**
```java
@RepeatedTest(10)
void testConcurrentUpdatesSameIndex() throws InterruptedException {
    int numThreads = 20;
    // 20 threads, all hitting timestamp 1
    ...
    assertEquals(numThreads * hitsPerThread, counter.getHit(1));
}
```
**Purpose**: Run the same test 10 times to catch **intermittent race conditions**.  
**What it tests**: 
- Flaky behavior (bugs that only appear sometimes)
- Thread scheduling variations

**Why this matters**: Race conditions are **non-deterministic**—they may not appear in every run. Repeating tests increases confidence.

#### **testConcurrentExpirations()**
```java
for (int i = 1; i <= 5; i++) {
    counter.hit(i);
}

// 5 threads all call getHit(10) simultaneously
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        int hits = counter.getHit(10);
        results.add(hits);
    });
}

// All threads should see 0-5 hits during cleanup
for (int result : results) {
    assertTrue(result >= 0 && result <= 5);
}

// Final state should be 0 (all expired)
assertEquals(0, counter.getHit(10));
```
**Purpose**: Verify thread-safety during concurrent expiration cleanup.  
**What it tests**: 
- Multiple threads cleaning expired entries simultaneously
- Atomic removal of expired hits
- Total count consistency

**Why this matters**: The cleanup loop has its own CAS:
```java
if (hits[i].compareAndSet(currentHit, null)){
    totalHits.addAndGet(-currentHit.count());
}
```
This test ensures no double-cleanup or missed entries.

#### **testHighConcurrencyStressTest()**
```java
int numThreads = 50;
int operationsPerThread = 1000;

// 50 threads, each doing 1000 operations (mix of hit and getHit)
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        for (int j = 0; j < operationsPerThread; j++) {
            if (j % 2 == 0) {
                counter.hit(threadId % 10);
            } else {
                counter.getHit(threadId % 10);
            }
        }
    });
}
```
**Purpose**: **Stress test** with high thread count and many operations.  
**What it tests**: 
- System stability under heavy load
- No crashes, deadlocks, or exceptions
- Performance degradation under contention

**Why this matters**: Real applications may have hundreds of concurrent users. This simulates realistic load.

#### **testAtomicityOfTotalHitsCounter()**
```java
int numThreads = 20;
int hitsPerThread = 100;

// All threads hit the same timestamp
assertEquals(numThreads * hitsPerThread, counter.getHit(timestamp));
```
**Purpose**: Specifically test the `AtomicInteger totalHits` counter.  
**What it tests**: 
- `totalHits.incrementAndGet()` is atomic
- `totalHits.addAndGet()` is atomic
- No lost increments

#### **testNoLostUpdatesUnderConcurrency()**
```java
int numThreads = 15;
int[] timestamps = {1, 2, 3, 4, 5};

// Each thread hits all 5 timestamps, 20 times each
for (int timestamp : timestamps) {
    for (int j = 0; j < hitsPerTimestampPerThread; j++) {
        counter.hit(timestamp);
    }
}

// Expected: 5 timestamps × 15 threads × 20 hits = 1500 total
int expectedTotal = timestamps.length * numThreads * hitsPerTimestampPerThread;
assertEquals(expectedTotal, counter.getHit(5));
```
**Purpose**: Verify **no updates are lost** in a complex scenario.  
**What it tests**: 
- Correctness across multiple timestamps
- All hits are counted
- Thread-safe aggregation

#### **testConcurrentGetDoesNotCorruptState()**
```java
for (int i = 1; i <= 5; i++) {
    counter.hit(i);
}

// 20 threads, each calling getHit() 100 times
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        for (int j = 0; j < 100; j++) {
            counter.getHit(5);
        }
    });
}

// State should be unchanged
assertEquals(5, counter.getHit(5));
```
**Purpose**: Verify that `getHit()` doesn't mutate state incorrectly.  
**What it tests**: 
- Read operations don't corrupt data
- Total count remains consistent

#### **testRaceConditionOnSameSlot()**
```java
int timestamp1 = 0;        // index 0
int timestamp2 = CAPACITY; // also index 0 (collision!)

// Half threads hit timestamp1, half hit timestamp2
```
**Purpose**: Test **race condition** when two different timestamps map to same index.  
**What it tests**: 
- Only one timestamp survives at each index
- CAS ensures atomic replacement
- Total count reflects the surviving timestamp

**Why this matters**: This simulates a race where:
1. Thread A tries to replace index 0 with timestamp 0
2. Thread B tries to replace index 0 with timestamp CAPACITY
3. One will win, one will retry

---

## Key Thread-Safety Mechanisms Tested

### 1. **Optimistic Locking via CAS**
```java
do {
    currentHit = hits[index].get();
    next = buildNext(currentHit, timestamp);
} while (!hits[index].compareAndSet(currentHit, next));
```
- **No locks** → better performance
- **Retry loop** → handles contention
- **Atomic guarantee** → no race conditions

### 2. **Atomic Operations**
- `AtomicReference.get()` and `compareAndSet()`
- `AtomicInteger.incrementAndGet()` and `addAndGet()`
- All operations are **linearizable** (appear atomic)

### 3. **Memory Visibility**
- Atomic classes provide **happens-before** guarantees
- Changes by one thread are visible to other threads
- No need for `volatile` or `synchronized`

### 4. **Immutability**
```java
public record Hit(int timestamp, int count) {}
```
- `Hit` is immutable (record in Java)
- No defensive copying needed
- Thread-safe by design

---

## Running the Tests

```bash
mvn test -Dtest=ConcurrentHitCounterTest
```

### Expected Behavior
- **All tests should pass** ✅
- **No race conditions** or deadlocks
- **Deterministic results** (even in concurrent tests)

### If Tests Fail
- **Flaky tests**: Run multiple times to confirm
- **Race conditions**: Check CAS logic
- **Wrong counts**: Verify totalHits adjustments

---

## Comparison: ThreadSafe vs Non-ThreadSafe

| Aspect | HitCounter | ConcurrentHitCounter |
|--------|------------|----------------------|
| **Thread-Safety** | ❌ Not thread-safe | ✅ Thread-safe |
| **Concurrency** | Single-threaded only | Lock-free concurrent |
| **Storage** | `Hit[] hits` | `AtomicReference<Hit>[] hits` |
| **Total Hits** | `int totalHits` | `AtomicInteger totalHits` |
| **Update Mechanism** | Direct assignment | CAS (Compare-And-Set) |
| **Performance** | Faster (no atomics) | Scalable under concurrency |

---

## Common Pitfalls (Avoided by These Tests)

### ❌ **Lost Updates**
```java
// Without CAS:
Hit current = hits[index];
hits[index] = new Hit(ts, current.count() + 1);
// ← Another thread could update between these lines!
```
**Solution**: CAS ensures atomic read-modify-write.

### ❌ **Dirty Reads**
```java
// Without atomic operations:
totalHits = totalHits + 1;
// ← Not atomic! Can be interrupted mid-operation.
```
**Solution**: `AtomicInteger.incrementAndGet()`.

### ❌ **Double Expiration**
```java
// Two threads could both expire the same entry
if (expired) {
    hits[i] = null;
    totalHits -= count; // ← Decremented twice!
}
```
**Solution**: CAS in expiration loop ensures only one thread expires each entry.

---

## Summary

This test suite provides **comprehensive coverage** of:
1. **Functional correctness** (basic operations)
2. **Edge cases** (capacity boundaries, collisions)
3. **Thread-safety** (concurrent access, race conditions)
4. **Stress testing** (high load, many threads)

The tests validate that `ConcurrentHitCounter`:
- ✅ Produces correct results
- ✅ Handles concurrent access safely
- ✅ Never loses updates
- ✅ Maintains data integrity
- ✅ Scales under contention

**Total Test Count**: 44 tests (including parameterized and repeated tests)

**Key Testing Tools**:
- `ExecutorService` for thread management
- `CountDownLatch` for synchronization
- `@RepeatedTest` for flaky bug detection
- `CopyOnWriteArrayList` for thread-safe result collection

---

# Deduplicator Test Cases

## Overview

The `Deduplicator` (single-threaded) and `ConcurrentDeduplicator` (thread-safe) implement TTL-based deduplication using a HashMap with a ring of HashSet buckets. Tests verify time-based expiration, bucket management, and thread-safety.

## Implementation Key Concepts

### 1. **TTL-Based Expiration**
- Keys expire after a configurable TTL (Time-To-Live) in seconds
- Uses `timestamp % ttl` to map keys to buckets
- Lazy expiry: keys older than TTL are treated as expired even before sweeper runs

### 2. **Bucket Ring Structure**
- Array of `ttl` HashSet buckets
- Each bucket holds keys for a specific second
- Sweeper clears expired buckets in batches

### 3. **Thread-Safety (ConcurrentDeduplicator)**
- Uses `ConcurrentHashMap` for the cache
- Uses `ConcurrentHashMap.newKeySet()` for buckets
- Relies on `ConcurrentHashMap.compute()` for atomic check-and-update

## Test Categories

### Basic Functional Tests

#### testSingleHit()
```java
assertTrue(deduplicator.isFirstSeen("k1"));
```
**Purpose**: Verify first occurrence returns true.

#### testSameKeyWithinTheTTLTime()
```java
assertTrue(testDedup.isFirstSeen("k1"));
clock.setInstant(Instant.ofEpochSecond(1010L));
assertFalse(testDedup.isFirstSeen("k1"));
```
**Purpose**: Verify duplicate within TTL returns false.

#### testKeyExpiresAfter60Seconds()
```java
clock.setInstant(startTime.plusSeconds(60));
assertFalse(dedup.isFirstSeen("k1"));
clock.setInstant(startTime.plusSeconds(61));
assertTrue(dedup.isFirstSeen("k1"));
```
**Purpose**: Verify exact TTL boundary (60 seconds).

### Sweeper Tests

#### testSweeperRemovesKeys()
```java
clock.setInstant(Instant.ofEpochSecond(1070L));
dedup.sweeper();
assertTrue(dedup.isFirstSeen("k1"));
```
**Purpose**: Verify sweeper removes expired keys from cache.

#### testSweeperCleansUpExpiredBuckets()
```java
clock.setInstant(Instant.ofEpochSecond(200L));
dedup.sweeper();
assertTrue(dedup.isFirstSeen("k1"));
```
**Purpose**: Verify sweeper cleans up multiple expired buckets.

### Concurrent Tests (ConcurrentDeduplicator)

#### testConcurrentIsFirstSeenSameKey()
```java
int threadCount = 10;
// 10 threads all call isFirstSeen("k1")
assertEquals(1, firstSeenCount.get());
```
**Purpose**: Verify only one thread sees a key as first-seen concurrently.

**What it tests**:
- Atomic check-and-update via `ConcurrentHashMap.compute()`
- No race conditions in duplicate detection

#### testConcurrentIsFirstSeenDifferentKeys()
```java
int threadCount = 100;
// Each thread calls isFirstSeen on unique key
assertEquals(threadCount, firstSeenKeys.size());
```
**Purpose**: Verify concurrent unique key handling.

#### testConcurrentCorrectnessWithOverlappingKeys()
```java
int threadCount = 50;
int keyCount = 100;
// Each thread processes all keys
for (int i = 0; i < keyCount; i++) {
    assertEquals(1, trueCountPerKey.get(key).get());
}
```
**Purpose**: Verify no lost updates under high concurrency.

#### testNoExceptionsUnderHighLoad()
```java
int threadCount = 50;
int operationsPerThread = 1000;
// Stress test with 50,000 operations
assertEquals(0, errors.get());
```
**Purpose**: Verify stability under heavy load.

## Key Thread-Safety Mechanisms

### ConcurrentHashMap.compute()
```java
cache.compute(key, (k, existingTimestamp) -> {
    if (existingTimestamp == null || isExpired(existingTimestamp)) {
        return currentSecond;
    }
    return existingTimestamp;
});
```
- Atomic check-and-update
- No explicit locks needed
- Handles concurrent modifications gracefully

### Volatile lastSweptSecond
```java
private volatile long lastSweptSecond;
```
- Ensures visibility across threads
- Sweeper must be single-threaded by design

## Running the Tests

```bash
mvn test -Dtest=DeduplicatorTest
mvn test -Dtest=ConcurrentDeduplicatorTest
```

---

# LRU Cache Test Cases

## Overview

The `LRU` (single-threaded) and `ConcurrentLRU` (thread-safe) implement Least Recently Used cache with O(1) get/put using HashMap and doubly linked list. Tests verify eviction, access order, and thread-safety.

## Implementation Key Concepts

### 1. **Doubly Linked List**
- Sentinel head/tail nodes eliminate null checks
- Most recently used at head, least recently used at tail
- O(1) move-to-head operation

### 2. **HashMap + Linked List**
- HashMap for O(1) key lookup
- Linked list for O(1) eviction
- Combined: O(1) get and put

### 3. **Thread-Safety (ConcurrentLRU)**
- Uses `ReentrantLock` to guard all operations
- Even `get` operations require locking (modifies list order)
- Single lock for simplicity

## Test Categories

### Basic Functional Tests

#### testBasicGetPut()
```java
lru.put(1, 10);
lru.put(2, 20);
assertEquals(10, lru.get(1));
assertEquals(-1, lru.get(3));
```
**Purpose**: Verify basic get/put operations.

#### testEviction()
```java
lru.put(1, 10);
lru.put(2, 20);
lru.put(3, 30); // Evicts key 1
assertEquals(-1, lru.get(1));
```
**Purpose**: Verify LRU eviction when capacity exceeded.

#### testUpdateExisting()
```java
lru.put(1, 10);
lru.put(1, 100); // Update moves to head
lru.put(3, 30);  // Evicts key 2
assertEquals(-1, lru.get(2));
```
**Purpose**: Verify update moves key to head (most recent).

#### testAccessOrder()
```java
lru.get(1); // Access key 1, move to head
lru.put(4, 40); // Evicts key 2
assertEquals(-1, lru.get(2));
```
**Purpose**: Verify get() updates access order.

### Edge Cases

#### testCapacityOne()
```java
LRU lru = new LRU(1);
lru.put(1, 10);
lru.put(2, 20); // Evicts key 1
assertEquals(-1, lru.get(1));
```
**Purpose**: Test smallest possible capacity.

#### testInvalidCapacity()
```java
assertThrows(IllegalArgumentException.class, () -> new LRU(0));
```
**Purpose**: Verify capacity validation.

#### testClear()
```java
lru.clear();
assertEquals(0, lru.size());
assertEquals(-1, lru.get(1));
```
**Purpose**: Verify clear operation.

## Running the Tests

```bash
mvn test -Dtest=LRUTest
mvn test -Dtest=ConcurrentLRUTest
```

---

# Max Stack Test Cases

## Overview

Three implementations of max stack:
- **TwoStackMaxStack**: Two-stack technique, O(1) push/pop/peek/getMax, no popMax
- **PopMaxStack**: Linked list + TreeMap, O(log n) popMax
- **ConcurrentMaxStack**: Thread-safe variant of PopMaxStack

## Implementation Key Concepts

### TwoStackMaxStack
- Main stack for all values
- Max stack tracks running maximum
- Duplicate maxima pushed again for correct pop behavior

### PopMaxStack
- Doubly linked list for stack order
- TreeMap of deques for max tracking
- Node handles enable O(1) removal from middle

## Test Categories

### TwoStackMaxStack Tests

#### testGetMaxMultipleElements()
```java
stack.push(5);
stack.push(10);
stack.push(3);
assertEquals(10, stack.getMax());
```
**Purpose**: Verify max tracking.

#### testPopUpdatesMax()
```java
stack.pop(); // Remove 3
assertEquals(10, stack.getMax());
stack.pop(); // Remove 10 (the max)
assertEquals(5, stack.getMax());
```
**Purpose**: Verify max updates correctly on pop.

#### testDuplicateValues()
```java
stack.push(10);
stack.push(10);
stack.pop(); // Remove one 10
assertEquals(10, stack.getMax());
```
**Purpose**: Verify duplicate handling (>= comparison).

### PopMaxStack Tests

#### testPopMaxBasic()
```java
stack.push(5);
stack.push(10);
stack.push(3);
assertEquals(10, stack.popMax());
assertEquals(3, stack.top());
```
**Purpose**: Verify popMax removes max from middle.

#### testPopMaxMiddleElement()
```java
stack.push(5);
stack.push(10);
stack.push(3);
stack.push(7);
assertEquals(10, stack.popMax());
assertEquals(7, stack.top());
```
**Purpose**: Verify popMax from middle of stack.

#### testAlternatingPopAndPopMax()
```java
stack.pop();
stack.popMax();
stack.pop();
assertEquals(3, stack.top());
```
**Purpose**: Verify mixed pop/popMax operations.

## Running the Tests

```bash
mvn test -Dtest=TwoStackMaxStackTest
mvn test -Dtest=PopMaxStackTest
mvn test -Dtest=ConcurrentMaxStackTest
```

---

# Top K Frequent Elements Test Cases

## Overview

The `TopKFrequentElements` class provides two algorithms for finding top K frequent elements:
- **Bucket Sort**: O(n) time, O(n) space
- **Min-Heap**: O(n log k) time, O(k) space

## Implementation Key Concepts

### Bucket Sort Approach
- Count frequencies using HashMap
- Create array of lists indexed by frequency
- Iterate from highest frequency to collect K elements

### Min-Heap Approach
- Count frequencies using HashMap
- Use min-heap of size K to track top K
- Keep smallest at top for easy eviction

## Test Categories

### Basic Functional Tests

#### testSingleHit()
```java
int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1,1,1,2,2,3}, 2);
assertArrayEquals(new int[]{1, 2}, result);
```
**Purpose**: Verify basic top K selection.

#### testKEqualsOne()
```java
int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1,2,3,3,3,4,4}, 1);
assertArrayEquals(new int[]{3}, result);
```
**Purpose**: Verify K=1 case (most frequent only).

#### testMultipleElementsWithDifferentFrequencies()
```java
int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{4,4,4,4,3,3,3,2,2,1}, 3);
assertArrayEquals(new int[]{2,3,4}, result);
```
**Purpose**: Verify correct frequency ordering.

### Edge Cases

#### testKGreaterThanUniqueElements()
```java
int K = 5;
int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1,1,2}, K);
assertEquals(K, result.length);
```
**Purpose**: Verify handling when K > unique elements.

#### testAllElementsSameFrequency()
```java
int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1,2,3,4}, 2);
assertEquals(K, result.length);
```
**Purpose**: Verify tie-breaking (any K elements).

#### testLargeArray()
```java
int[] nums = new int[100];
// Fill with different frequencies
int[] result = topKFrequentElements.topKFrequentUsingHeap(nums, 2);
assertArrayEquals(new int[]{1, 2}, result);
```
**Purpose**: Verify scalability.

### Algorithm Comparison

Both bucket sort and min-heap approaches are tested with identical test cases to verify:
- Correctness of both algorithms
- Consistent results
- Edge case handling

## Running the Tests

```bash
mvn test -Dtest=TopKFrequentElementsTest
```

---

# Overall Test Summary

This test suite provides comprehensive coverage for all data structures:

| Data Structure | Test Count | Key Focus |
|---------------|------------|-----------|
| HitCounter | 14 | Basic operations, expiration |
| ConcurrentHitCounter | 44 | Lock-free concurrency, CAS |
| Deduplicator | 5 | TTL expiration, sweeper |
| ConcurrentDeduplicator | 14 | Concurrent deduplication |
| LRU | 9 | Eviction, access order |
| TwoStackMaxStack | 18 | Two-stack max tracking |
| PopMaxStack | 22 | popMax operation |
| TopKFrequentElements | 20 | Bucket sort vs heap |

**Total**: 146 tests across all implementations

All tests use JUnit 5 and include both single-threaded and concurrent scenarios where applicable.
