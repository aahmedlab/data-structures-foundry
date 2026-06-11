# ThreadSafeHitCounter Test Cases - Detailed Explanation

## Overview

The `ThreadSafeHitCounter` is a lock-free, thread-safe implementation of a hit counter that uses **atomic operations** and **Compare-And-Set (CAS)** to handle concurrent access without traditional locks. This test suite validates both functional correctness and thread-safety guarantees.

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
mvn test -Dtest=ThreadSafeHitCounterTest
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

| Aspect | HitCounter | ThreadSafeHitCounter |
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

The tests validate that `ThreadSafeHitCounter`:
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
