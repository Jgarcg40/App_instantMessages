package com.example.messages_app;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class ClientManager {
    private static final String SERVER_HOST = "tu host o ip publica";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    private String username;
    private String password;

    private Queue<String> messageQueue = new LinkedList<>();

    // Interface to notify the activity when a message is received
    public interface ReceiveListener {
        void onMessageReceived(String remitente, String mensaje);
    }

    private ReceiveListener receiveListener;

    public ClientManager(String username, String password) {
        this.username = username;
        this.password = password;

    }

    public void setReceiveListener(ReceiveListener listener) {
        this.receiveListener = listener;
    }

    public void connect() {
        ConnectTask connectTask = new ConnectTask();
        connectTask.execute();

    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enqueueMessage(String destinatario, String mensaje) {
        String message = destinatario + "|" + mensaje;
        messageQueue.add(message);
    }


    private class ConnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Conectar al servidor
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                Log.d("ClientManager", "Conexión al servidor establecida.");

                // Configurar el lector para recibir mensajes del servidor
                serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Configurar el escritor para enviar mensajes al servidor
                serverWriter = new PrintWriter(socket.getOutputStream(), true);


                String token = TokenManager.getInstance().getToken();
                Log.d("LoginManager", "Mostrando token: " + token);
                // Enviar el nombre de usuario y contraseña al servidor en el formato: token|usuario;contraseña
                serverWriter.println(token + "|" + password + ";" + username);

                // Iniciar el hilo para recibir mensajes del servidor
                startReceiveThread();

                // Procesar mensajes encolados
                Log.d("ClientManager", "Mensajes encolados antes del bucle: " + messageQueue.toString());

                while (!messageQueue.isEmpty()) {
                    String message = messageQueue.poll();
                    if (message != null) {
                        Log.d("ClientManager", "Enviando mensaje al servidor: " + message);
                        // Enviar el mensaje al servidor en el formato: username|mensaje
                        serverWriter.println(username + "|" + message);
                        Log.d("ClientManager", "Mensaje enviado al servidor: " + message);
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void startReceiveThread() {
        Thread receiveThread = new Thread(() -> {
            try {
                while (true) {
                    // Verificar si el serverReader no es nulo antes de leer una línea
                    if (serverReader != null) {
                        // Leer el mensaje recibido del servidor
                        String receivedMessage = serverReader.readLine();
                        // Extraer el mensaje del formato "remitente|destinatario|mensaje"
                        String[] parts = receivedMessage.split("\\|");
                        if (parts.length == 3) {
                            String remitente = parts[0];
                            String mensaje = parts[2];
                            Log.d("ClientManager", "Mensaje recibido de " + remitente + ": " + mensaje);

                            // Notificar al listener en la actividad cuando se recibe un mensaje
                            if (receiveListener != null) {
                                receiveListener.onMessageReceived(remitente, mensaje);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }
}
