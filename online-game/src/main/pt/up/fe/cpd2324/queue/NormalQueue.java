package pt.up.fe.cpd2324.queue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class NormalQueue<T> extends Queue<T> {
    private final List<T> queue;

    public NormalQueue() {
        super();
        this.queue = new LinkedList<>();
    }

    @Override
    public boolean add(T x) {
        this.lock.lock();
        try {
            if (this.contains(x)) {
                return false;
            }
            return this.queue.add(x);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean remove(T x) {
        this.lock.lock();
        try {
            return this.queue.remove(x);
        } finally {
            this.lock.unlock();
        }
    }

    public T pop() {
        this.lock.lock();
        try {
            return this.queue.remove(0);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean contains(T x) {
        this.lock.lock();
        try {
            return this.queue.contains(x);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int getSize() {
        this.lock.lock();
        try {
            return this.queue.size();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean canStartGame(int numPlayers) {
        this.lock.lock();
        try {
            return this.queue.size() >= numPlayers;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public List<T> getPlayers(int numPlayers) {
        this.lock.lock();
        try {
            List<T> players = new LinkedList<>();
            for (int i = 0; i < numPlayers; i++) {
                players.add(this.queue.remove(0));
            }
            return players;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return this.queue.iterator();
    }
}
