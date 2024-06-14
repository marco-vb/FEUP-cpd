package pt.up.fe.cpd2324.queue;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Queue<T> implements Iterable<T> {
    protected ReentrantLock lock;

    public Queue() {
        this.lock = new ReentrantLock();
    }

    public abstract boolean add(T user);

    public abstract boolean remove(T user);

    public abstract boolean contains(T user);

    public abstract int getSize();  

    public abstract boolean canStartGame(int numPlayers);

    public abstract List<T> getPlayers(int numPlayers);
}
