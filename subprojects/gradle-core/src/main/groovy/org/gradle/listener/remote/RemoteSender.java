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
package org.gradle.listener.remote;

import org.gradle.listener.dispatch.*;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteSender<T> implements Closeable {
    private final ProxyDispatchAdapter<T> source;
    private final Socket socket;
    private final CloseableDispatch<Message> asyncDispatch;
    private ExecutorService executor;

    public RemoteSender(Class<T> type, int port) throws IOException {
        socket = new Socket((String) null, port);
        OutputStream outstr = new BufferedOutputStream(socket.getOutputStream());
        executor = Executors.newSingleThreadExecutor();
        asyncDispatch = new AsyncDispatch<Message>(executor, new SerializingDispatch(outstr));
        source = new ProxyDispatchAdapter<T>(type, asyncDispatch);
    }

    public T getSource() {
        return source.getSource();
    }

    public void close() throws IOException {
        asyncDispatch.dispatch(new EndOfStream());
        asyncDispatch.close();
        executor.shutdown();
        socket.close();
    }
}

