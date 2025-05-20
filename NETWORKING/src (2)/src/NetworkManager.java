import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class NetworkManager {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final AtomicBoolean isServer = new AtomicBoolean(false);
    private final BlockingQueue<Object> incomingQueue = new LinkedBlockingQueue<>();
    private final List<ObjectOutputStream> clientOutputStreams = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object connectionLock = new Object();

    public boolean isServer() { return isServer.get(); }
    public boolean isConnected() { return running.get(); }
    public boolean isRunning() { return running.get(); }

    public void startServer(TodoList initialList) throws IOException {
        synchronized (connectionLock) {
            if (running.get()) {
                throw new IllegalStateException("Server is already running");
            }
            
            try {
                serverSocket = new ServerSocket(8080);
                isServer.set(true);
                running.set(true);
                
                new Thread(() -> {
                    try {
                        while (running.get()) {
                            Socket socket = serverSocket.accept();
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            synchronized (clientOutputStreams) {
                                clientOutputStreams.add(oos);
                            }
                            oos.writeObject(initialList);
                            oos.flush();
                            new ClientHandler(socket, initialList).start();

                        }
                    } catch (SocketException e) {
                        if (running.get()) {
                            System.err.println("Server socket closed: " + e.getMessage());
                        }
                    } catch (IOException e) {
                        if (running.get()) {
                            System.err.println("Server error: " + e.getMessage());
                        }
                    }
                }).start();
            } catch (IOException e) {
                close();
                throw e;
            }
        }
    }

    public void connectToServer(String ip) throws IOException {
        synchronized (connectionLock) {
            if (running.get()) {
                throw new IllegalStateException("Already connected");
            }
            
            try {
                clientSocket = new Socket(ip, 8080);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                running.set(true);
                
                new Thread(() -> {
                    try {
                        while (running.get()) {
                            Object obj = in.readObject();
                            incomingQueue.put(obj);
                        }
                    } catch (EOFException e) {
                        // Normal connection close
                    } catch (Exception e) {
                        if (running.get()) {
                            System.err.println("Client receive error: " + e.getMessage());
                        }
                    } finally {
                        close();
                    }
                }).start();
            } catch (IOException e) {
                close();
                throw e;
            }
        }
    }

    public void sendObject(Object obj) throws IOException {
        if (!running.get()) {
            throw new IllegalStateException("NetworkManager is not running");
        }
        
        synchronized (connectionLock) {
            if (isServer.get()) {
                broadcast(obj);
            } else {
                try {
                    out.writeObject(obj);
                    out.flush();
                    out.reset(); // Reset to avoid caching issues
                } catch (IOException e) {
                    close();
                    throw e;
                }
            }
        }
    }

    private void broadcast(Object obj) {
        synchronized (clientOutputStreams) {
            Iterator<ObjectOutputStream> iterator = clientOutputStreams.iterator();
            while (iterator.hasNext()) {
                ObjectOutputStream oos = iterator.next();
                try {
                    synchronized (oos) {
                        oos.writeObject(obj);
                        oos.flush();
                        oos.reset(); // Reset to avoid caching issues
                    }
                } catch (IOException e) {
                    iterator.remove();
                }
            }
        }
    }

    public Object getUpdate() throws InterruptedException {
        return incomingQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    public void close() {
        synchronized (connectionLock) {
            if (!running.getAndSet(false)) return;
            
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                
                synchronized (clientOutputStreams) {
                    for (ObjectOutputStream oos : clientOutputStreams) {
                        try {
                            oos.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    clientOutputStreams.clear();
                }
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private final TodoList todoList; // ✅ Shared list from server
    
        public ClientHandler(Socket socket, TodoList todoList) {
            this.socket = socket;
            this.todoList = todoList;
        }
    
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (running.get()) {
                    Object input = in.readObject();
    
                    if (input instanceof JoinGroupRequest) {
                        JoinGroupRequest req = (JoinGroupRequest) input;
                        todoList.joinGroup(req.groupId, req.userId);  // ✅ FIXED: uses passed todoList
                    } else if (input instanceof RefreshRequest) {
                        broadcast(todoList);  // ✅ FIXED: uses passed todoList
                    } else {
                        broadcast(input);
                    }
    
                    incomingQueue.put(input);
                }
            } catch (EOFException e) {
                // Normal connection close
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("Client handler error: " + e.getMessage());
                }
            }
        }
    }
}
    