package org.academiadecodigo.splicegirls.Jorema.Server;

import org.academiadecodigo.splicegirls.Jorema.Server.Store.PlayerStore;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Server {

    private GameLogic gameLogic;
    private List<ServerWorker> workers = Collections.synchronizedList(new ArrayList<ServerWorker>());
    private PlayerStore playerStore;

    public Server() {


//        this.gameLogic = gameLogic;

    }


    public Server(GameLogic gameLogic, PlayerStore playerStore) {

        this.gameLogic = gameLogic;
        this.playerStore = playerStore;
    }


        private void start(){

        }

    private void setUpPlayer(String name) {

//        gameLogic.addPlayer(name);
//        System.out.println();

    }

    public void startConnection(int port) {

        int connectionCount = 0;

        try {

            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started: " + serverSocket);

            while (true) {

                // Block waiting for client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player client accepted: " + clientSocket);

                try {

                    // Create a new Server Worker
                    connectionCount++;
                    String name = "Player-" + connectionCount;
                    ServerWorker worker = new ServerWorker(name, clientSocket);
                    workers.add(worker);
                    playerStore.addPlayer(name);


                    // Serve the client connection with a new Thread
                    Thread thread = new Thread(worker);
                    thread.setName(name);
                    thread.start();

                } catch (IOException ex) {
                    System.out.println("Error receiving client connection: " + ex.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to start server on port " + port);
        }


        //gameLogic.addPlayer(name);

    }

    /**
     * Broadcast a message to all server connected clients
     *
     * @param origClient name of the client thread that the message originated from
     * @param message    the message to broadcast
     */
    private void sendAll(String origClient, String message){

            // Acquire lock for safe iteration
            synchronized (workers) {

                for (ServerWorker worker : workers) {
                    worker.send(origClient, message);
                }

            }

    }

    /**
     * Handles client connections
     */
    private class ServerWorker implements Runnable {

        // Immutable state, no need to lock
        final private String name; // !!
        final private Socket clientSocket;
        final private BufferedReader in;
        final private BufferedWriter out;

        /**
         * @param name         the name of the thread handling this client connection
         * @param clientSocket the client socket connection
         * @throws IOException upon failure to open socket input and output streams
         */

        private ServerWorker(String name, Socket clientSocket) throws IOException {

            this.name = name;
            this.clientSocket = clientSocket;

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        }

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {

            System.out.println("Thread " + name + " started");

            try {

                while (!clientSocket.isClosed()) {

                    // Blocks waiting for client messages
                    String line = in.readLine();

                    if (line == null) {

                        System.out.println("Player " + name + " closed, exiting...");

                        playerStore.removePlayer(name);
                        in.close();
                        clientSocket.close();
                        continue;

                    } else {

                        // Broadcast message to all other clients
                        sendAll(name, line);
                    }
                }

                workers.remove(this);
                playerStore.removePlayer(name);

            } catch (IOException ex) {
                System.out.println("Receiving error on " + name + " : " + ex.getMessage());
            }

        }

        /**
         * Send a message to the client served by this thread
         *
         * @param origClient the name of the client thread the message originated from
         * @param message    the message to send
         */
        private void send(String origClient, String message) {

            try {

                out.write(origClient + ": " + message);
                out.newLine();
                out.flush();

            } catch (IOException ex) {
                System.out.println("Error sending message to Client " + name + " : " + ex.getMessage());
            }
        }

    }

}
