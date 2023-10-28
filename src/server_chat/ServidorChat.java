/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_chat;

/**
 *
 * @author guama
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorChat {

    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Iniciando servidor...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para manejar la conexión de cada cliente.
     */
    private static class ClientHandler extends Thread {

        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                OutputStream output = socket.getOutputStream();
                out = new PrintWriter(output, true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Recibido (encriptado): " + message);
                    String decryptedMessage = CryptoHelper.decrypt(message);
                    System.out.println("Decodificado: " + decryptedMessage);

                    // Reenviar mensaje a todos los clientes
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }
    }
}
