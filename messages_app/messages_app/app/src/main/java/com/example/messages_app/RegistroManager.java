package com.example.messages_app;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import android.os.AsyncTask;
import android.util.Log;


public class RegistroManager {

    private static final String SERVER_HOST = "tu ip publica o host";
    private static final int SERVER_PORT = 12345;

    public interface OnRegistroResultListener {
        void onRegistroResult(boolean exito);
    }

    public void registrarUsuario(String nuevoNombre, String nuevoApellido1,String nuevoApellido2, String nuevoUsuario, String nuevoEmail,  String nuevaContrasena, OnRegistroResultListener listener) {
        new RegistroTask(nuevoNombre, nuevoApellido1,nuevoApellido2,nuevoUsuario, nuevoEmail, nuevaContrasena, listener).execute();
    }

    private static class RegistroTask extends AsyncTask<Void, Void, Boolean> {

        private final String nuevoNombre;
        private final String nuevoApellido1;
        private final String nuevoApellido2;
        private final String nuevoEmail;
        private final String nuevoUsuario;
        private final String nuevaContrasena;
        private final OnRegistroResultListener listener;


        public RegistroTask(String nuevoNombre, String nuevoApellido1,String nuevoApellido2, String nuevoUsuario, String nuevoEmail,  String nuevaContrasena, OnRegistroResultListener listener) {
            this.nuevoNombre = nuevoNombre;
            this.nuevoApellido1 = nuevoApellido1;
            this.nuevoApellido2 = nuevoApellido2;
            this.nuevoUsuario = nuevoUsuario;
            this.nuevoEmail = nuevoEmail;
            this.nuevaContrasena = nuevaContrasena;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Conectar al servidor
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                // Configurar el escritor para enviar el nombre de usuario y contraseña al servidor
                PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
                // Configurar el lector para recibir la respuesta del servidor
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String token_null=null;

                // Enviar la solicitud de registro al servidor en el formato: REGISTRAR|usuario|contraseña
                String solicitudRegistro =token_null + "|" + "REGISTRAR:"   + nuevoNombre + ":" + nuevoApellido1 + ":"+ nuevoApellido2 + ":"+ nuevoUsuario + ":" + nuevoEmail + ":" + nuevaContrasena;
                serverWriter.println(solicitudRegistro);

                // Cerrar la conexión con el servidor
                socket.close();

                // Verificar si el registro fue exitoso
                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean exito) {
            if (listener != null) {
                listener.onRegistroResult(exito);
            }
        }
    }
}
