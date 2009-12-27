/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.listener.dispatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncDispatch<T extends Message> implements CloseableDispatch<T> {
    private static final int MAX_QUEUE_SIZE = 200;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final LinkedList<T> queue = new LinkedList<T>();
    private boolean closed;

    public AsyncDispatch(Executor executor, final Dispatch<? super T> dispatch) {
        executor.execute(new Runnable() {
            public void run() {
                pushMessages(dispatch);
            }
        });
    }

    private void pushMessages(Dispatch<? super T> dispatch) {
        while (true) {
            List<T> messages = new ArrayList<T>();
            lock.lock();
            try {
                while (!closed && queue.isEmpty()) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (!queue.isEmpty()) {
                    messages.addAll(queue);
                }
            } finally {
                lock.unlock();
            }

            if (messages.isEmpty()) {
                return;
            }
            for (T message : messages) {
                dispatch.dispatch(message);
            }
            
            lock.lock();
            try {
                queue.subList(0, messages.size()).clear();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public void dispatch(final T message) {
        lock.lock();
        try {
            while (!closed && queue.size() >= MAX_QUEUE_SIZE) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (closed) {
                throw new IllegalStateException("This message dispatch has been closed.");
            }
            queue.add(message);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        lock.lock();
        try {
            closed = true;
            condition.signalAll();
            while (!queue.isEmpty()) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
