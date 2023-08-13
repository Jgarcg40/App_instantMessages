package com.example.messages_app;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginManager {

    private static final String SERVER_HOST = "tu ip publica o host";
    private static final int SERVER_PORT = 12345;

    public void verificarCredenciales(String username, String password, OnLoginResultListener listener) {
        new VerifyCredentialsTask(listener).execute(username, password);
    }

    private class VerifyCredentialsTask extends AsyncTask<String, Void, Boolean> {

        private OnLoginResultListener listener;

        public VerifyCredentialsTask(OnLoginResultListener listener) {
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            try {
                // Conectar al servidor
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                // Configurar el escritor para enviar el nombre de usuario y contraseña al servidor
                PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
                // Configurar el lector para recibir el resultado de la verificación del servidor
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String token = TokenManager.getInstance().getToken();
                Log.d("LoginManager", "Mostrando token: " + token);
                // Enviar el nombre de usuario y contraseña al servidor en el formato: usuario|contraseña
                serverWriter.println(token + "|" + password + ";" + username);

                // Esperar el resultado de la verificación de credenciales del servidor
                char[] result = new char[2];
                serverReader.read(result);

                // Cerrar la conexión con el servidor
                socket.close();

                // Verificar si el resultado es "OK"
                return new String(result).equals("OK");

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean credencialesCorrectas) {
            if (listener != null) {
                listener.onLoginResult(credencialesCorrectas);
            }
        }
    }

    public interface OnLoginResultListener {
        void onLoginResult(boolean credencialesCorrectas);
    }
}
