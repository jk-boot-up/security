package com.jk.explore.security.failclosed;

/** A small pool of database connections. A connection that is never given back is gone, and when all are gone the bank stops. */
final class ConnectionPool {

    final class Connection implements AutoCloseable {

        private boolean open = true;

        void update(boolean fail) {
            if (fail) {
                throw new IllegalStateException("update failed");
            }
        }

        @Override
        public void close() {
            if (open) {
                open = false;
                inUse--;
            }
        }
    }

    private final int size;
    private int inUse;

    ConnectionPool(int size) {
        this.size = size;
    }

    Connection take() {
        if (inUse >= size) {
            throw new IllegalStateException("no connection available");
        }
        inUse++;
        return new Connection();
    }

    int free() {
        return size - inUse;
    }

    /** Takes a connection and gives it back only when the update works: an error leaks the connection. */
    static void naiveUpdate(ConnectionPool pool, boolean fail) {
        Connection connection = pool.take();
        connection.update(fail);
        connection.close();
    }

    /** try-with-resources gives the connection back on every path, error or not. */
    static void safeUpdate(ConnectionPool pool, boolean fail) {
        try (Connection connection = pool.take()) {
            connection.update(fail);
        }
    }
}
