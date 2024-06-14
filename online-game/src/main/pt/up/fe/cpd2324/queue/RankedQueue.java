package pt.up.fe.cpd2324.queue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class RankedQueue<T extends Rateable> extends Queue<T> {
    private static final int WAIT_TIME = 10000; // Increase bucket size every 10 seconds
    
    private List<Bucket<T>> buckets;
    
    private int numBuckets;
    private int step;

    public RankedQueue() {
        super();

        this.defaultBuckets();

        Thread.ofVirtual().start(this::updateBuckets);
    }

    private void updateBuckets() {
        while (true) {
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                System.out.println("Error updating buckets: " + e.getMessage());
            }

            if (this.getSize() <= 1 || this.step >= 1000) {
                System.out.println("Skipping bucket update [size=" + this.getSize() + ", step=" + this.step + "]");
                continue;
            }

            System.out.println("Updating buckets [size=" + this.getSize() + ", step=" + this.step + "]"); 

            this.step += this.step / 4;

            List<Bucket<T>> newBuckets = this.makeBuckets();
            this.distributePlayers(this.buckets, newBuckets);

            this.buckets = newBuckets;
        }
    }

    private void defaultBuckets() {
        this.numBuckets = 10;
        this.step = 200;
        this.buckets = makeBuckets();
    }

    private List<Bucket<T>> makeBuckets() {
        List<Bucket<T>> b = new ArrayList<>();

        for (int i = 0; i < this.numBuckets; i++) {
            if (i < this.numBuckets - 1) {
                b.add(new Bucket<>(i * this.step, (i + 1) * this.step - 1));
            } else {
                b.add(new Bucket<>((this.numBuckets - 1) * this.step, Integer.MAX_VALUE));
            }
        }

        return b;
    }

    private void distributePlayers(List<Bucket<T>> from, List<Bucket<T>> to) {
        for (Bucket<T> bucket : from) {
            for (T player : bucket.players) {
                for (Bucket<T> newBucket : to) {
                    if (newBucket.add(player)) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean add(T x) {
        this.lock.lock();
        try {
            return this.buckets.stream().anyMatch(bucket -> bucket.add(x));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean remove(T x) {
        this.lock.lock();
        try {
            return this.buckets.stream().anyMatch(bucket -> bucket.remove(x));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean contains(T x) {
        this.lock.lock();
        try {
            return this.buckets.stream().anyMatch(bucket -> bucket.contains(x));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int getSize() {
        this.lock.lock();
        try {
            return this.buckets.stream().mapToInt(Bucket::getSize).sum();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean canStartGame(int players) {
        this.lock.lock();
        try {
            return this.buckets.stream().anyMatch(bucket -> bucket.canStartGame(players));
        } finally {
            this.lock.unlock();
        }
    }

    public List<T> getPlayers(int players) {
        this.lock.lock();
        try {
            List<T> gamePlayers = new ArrayList<>();
            for (Bucket<T> bucket : this.buckets) {
                if (bucket.getSize() < players) {
                    continue;
                }

                for (T player : bucket.players) {
                    gamePlayers.add(player);
                    if (gamePlayers.size() == players) {
                        break;
                    }
                }

                for (T player : gamePlayers) {
                    bucket.remove(player);
                }

                List<Bucket<T>> bucketsCopy = new ArrayList<>(this.buckets);
                this.defaultBuckets();
                this.distributePlayers(bucketsCopy, this.buckets);

                break;
            }
            return gamePlayers;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        this.lock.lock();
        try {
            List<T> players = new ArrayList<>();
            for (Bucket<T> bucket : this.buckets) {
                players.addAll(bucket.players);
            }
            return players.iterator();
        } finally {
            this.lock.unlock();
        }
    }

    private class Bucket<TT extends Rateable> {
        private final HashSet<TT> players;
        private final int lower;
        private final int upper;

        public Bucket(int lower, int upper) {
            this.players = new HashSet<>();
            this.lower = lower;
            this.upper = upper;
        }

        public boolean add(TT x) {
            int rating = x.getRating();
            if (rating < this.lower || rating > this.upper) {
                return false;
            }
            return this.players.add(x);
        }

        public boolean remove(TT x) {
            return this.players.remove(x);
        }

        public boolean contains(TT x) {
            return this.players.contains(x);
        }

        public int getSize() {
            return this.players.size();
        }

        public boolean canStartGame(int players) {
            return this.players.size() >= players;
        }
    }
}
