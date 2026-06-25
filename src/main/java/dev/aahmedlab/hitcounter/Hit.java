package dev.aahmedlab.hitcounter;

/**
 * An immutable snapshot of the number of hits recorded at a single timestamp.
 *
 * <p>Used as the slot type in both {@link HitCounter} and {@link ThreadSafeHitCounter}.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Record immutability:</b> The (timestamp, count) pair is published as a single immutable
 *       unit, so concurrent readers in {@link ThreadSafeHitCounter} can never observe a torn or
 *       partially updated state.
 *   <li><b>Replace-on-write:</b> Incrementing a count creates a new {@code Hit} rather than
 *       mutating an existing one, enabling atomic compare-and-set slot updates.
 * </ul>
 *
 * @param timestamp the timestamp (in seconds) these hits were recorded at
 * @param count the number of hits recorded at {@code timestamp}
 */
public record Hit(int timestamp, int count) {}
