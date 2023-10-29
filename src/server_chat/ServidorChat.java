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

    // Puerto en el que escucha el servidor.
    private static final int PORT = 12345;

    // Mapa para asociar cada PrintWriter (representando a un cliente) con el nombre del cliente.
    private static Map<PrintWriter, String> clientNames = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Iniciando servidor...");

        // Intenta iniciar el ServerSocket en el puerto definido.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Esperando conexiones...");

            // El servidor sigue escuchando indefinidamente para aceptar conexiones de clientes.
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para manejar la comunicación con cada cliente individualmente.
    private static class ClientHandler extends Thread {
        
        // Socket que representa la conexión con el cliente.
        private Socket socket;
        private PrintWriter out;  // Para enviar mensajes al cliente.
        private String clientName = null;  // Nombre del cliente, se asigna después de que el cliente envíe su primer mensaje.

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Preparativos para leer datos del cliente.
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                // Preparativos para enviar datos al cliente.
                OutputStream output = socket.getOutputStream();
                out = new PrintWriter(output, true);

                // Sincronizamos el acceso al mapa de nombres de clientes para evitar problemas de concurrencia.
                synchronized (clientNames) {
                    clientNames.put(out, "");  // Añadimos al cliente al mapa sin nombre por ahora.
                }

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Recibido (encriptado): " + message);
                    String decryptedMessage = CryptoHelper.decrypt(message);
                    System.out.println("Decodificado: " + decryptedMessage);

                    // Si es el primer mensaje del cliente, extraemos el nombre del cliente.
                    if (clientName == null) {
                        clientName = decryptedMessage.split(" ")[0];
                        synchronized (clientNames) {
                            clientNames.put(out, clientName);  // Actualizamos el mapa con el nombre del cliente.
                        }
                    }

                    // Reenviar el mensaje a todos los clientes.
                    synchronized (clientNames) {
                        for (PrintWriter writer : clientNames.keySet()) {
                            writer.println(message);
                        }
                    }
                }
            } catch (SocketException se) {
                // Esta excepción se lanza cuando un cliente cierra abruptamente la conexión.
                System.out.println(clientName + " se ha desconectado.");
                String disconnectedMessage = CryptoHelper.encrypt(clientName + " se ha desconectado.");

                // Informar a todos los clientes que un cliente se ha desconectado.
                synchronized (clientNames) {
                    clientNames.remove(out);  // Eliminamos al cliente del mapa.
                    for (PrintWriter writer : clientNames.keySet()) {
                        writer.println(disconnectedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Intenta cerrar el socket al finalizar.
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
