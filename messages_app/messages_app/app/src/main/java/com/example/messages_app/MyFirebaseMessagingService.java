package com.example.messages_app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyFirebaseMessagingService", "Servicio iniciado");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        //log para verificar si el método se ejecuta
        Log.d("MyFirebaseMessagingService", "onNewToken llamado");

        // Mostrar el token en los logs
        Log.d("MyFirebaseMessagingService", "Mostrando token: " + token);
        // Guardar el token en TokenManager
        TokenManager.getInstance().setToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
          
            String sender = remoteMessage.getData().get("sender");
            String message = remoteMessage.getData().get("message");

            // Muestra una notificación
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mostrarNotificacion(sender, message);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void mostrarNotificacion(String sender, String message) {
        // Crea una notificación usando NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nuevo mensaje de " + sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        //definir una acción para abrir la actividad correspondiente
        Intent intent = new Intent(this, ConnectActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        // Muestra la notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        } else {
            Toast.makeText(this, "Permission to send notifications is required.", Toast.LENGTH_SHORT).show();
        }
    }
}
