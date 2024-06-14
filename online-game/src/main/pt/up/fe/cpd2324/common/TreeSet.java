package pt.up.fe.cpd2324.common;

import java.util.concurrent.locks.ReentrantLock;

// Implements a thread-safe TreeSet
// Uses locks to synchronize access to the underlying TreeSet
public class TreeSet<T extends Comparable<T>> implements Iterable<T> {
    private final java.util.TreeSet<T> set = new java.util.TreeSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public TreeSet() {}

    public boolean add(T element) {
        this.lock.lock();
        try {
            return this.set.add(element);
        } finally {
            this.lock.unlock();
        }
    }

    public boolean remove(T element) {
        this.lock.lock();
        try {
            return this.set.remove(element);
        } finally {
            this.lock.unlock();
        }
    }

    public boolean contains(T element) {
        this.lock.lock();
        try {
            return this.set.contains(element);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public java.util.Iterator<T> iterator() {
        this.lock.lock();
        try {
            return this.set.iterator();
        } finally {
            this.lock.unlock();
        }
    }
}
