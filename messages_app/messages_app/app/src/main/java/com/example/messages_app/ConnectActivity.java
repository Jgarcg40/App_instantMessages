package com.example.messages_app;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ConnectActivity extends AppCompatActivity implements LoginManager.OnLoginResultListener {

    private static final int PERMISSION_REQUEST_CODE =123;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegistro;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar si tienes el permiso de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }

        startService(new Intent(ConnectActivity.this, MyFirebaseMessagingService.class));

        setContentView(R.layout.activity_connect);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_connect);
        btnRegistro = findViewById(R.id.btn_register);

        // Inicializar LoginManager
        loginManager = new LoginManager();

        // Verificar si ya hay credenciales guardadas en SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String storedUsername = sharedPreferences.getString("username", null);
        String storedPassword = sharedPreferences.getString("password", null);

        if (storedUsername != null && storedPassword != null) {
            // Iniciar automáticamente si hay credenciales guardadas
            Intent intent = new Intent(ConnectActivity.this, ContactListActivity.class);
            intent.putExtra("username", storedUsername);
            intent.putExtra("password", storedPassword);
            startActivity(intent);
            finish(); // Cerrar la actividad actual si ya hay inicio de sesión automático
        } else {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = etUsername.getText().toString();
                    String password = etPassword.getText().toString();

                    // Validar que el nombre de usuario no esté vacío
                    if (username.isEmpty()) {
                        Toast.makeText(ConnectActivity.this, "Ingresa el nombre de usuario y la contraseña", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verificar las credenciales con LoginManager
                    loginManager.verificarCredenciales(username, password, ConnectActivity.this);
                }
            });
        }

        // Agregar OnClickListener para el botón de registro
        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Iniciar la actividad RegistroActivity al hacer clic en el botón de registro
                Intent intent = new Intent(ConnectActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });
    }


    // Agrega el método onRequestPermissionsResult para manejar la respuesta de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes continuar con la lógica de tu aplicación
                Toast.makeText(this, "Permiso de notificaciones concedido.", Toast.LENGTH_SHORT).show();
            } else {
                // Permiso denegado, muestra un mensaje o realiza alguna acción necesaria
                Toast.makeText(this, "Permiso de notificaciones denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLoginResult(boolean credencialesCorrectas) {
        // Verificar el resultado y actuar en consecuencia
        if (credencialesCorrectas) {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            // Guardar las credenciales en SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.apply();

            // Crear el intent para la siguiente actividad
            Intent intent = new Intent(ConnectActivity.this, ContactListActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            startActivity(intent);
        } else {
            // Si las credenciales son incorrectas, mostrar un mensaje de error
            Toast.makeText(ConnectActivity.this, "Credenciales incorrectas. Intenta nuevamente.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
