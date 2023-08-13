package com.example.messages_app;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegistroActivity extends AppCompatActivity implements RegistroManager.OnRegistroResultListener {

    private EditText etNombre;
    private EditText etApellido1;
    private EditText etApellido2;
    private EditText etEmail;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnRegistrar;
    private RegistroManager registroManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.et_nombre);
        etApellido1 = findViewById(R.id.et_apellido1);
        etApellido2 = findViewById(R.id.et_apellido2);
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnRegistrar = findViewById(R.id.btn_register);

        registroManager = new RegistroManager();

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = etNombre.getText().toString();
                String apellido1 = etApellido1.getText().toString();
                String apellido2 = etApellido2.getText().toString();
                String email = etEmail.getText().toString();
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                // Validar que todos los campos estén completos
                if (nombre.isEmpty() || apellido1.isEmpty() || apellido2.isEmpty() ||
                        email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegistroActivity.this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Realizar el registro
                registroManager.registrarUsuario(nombre, apellido1, apellido2,username, email, password, RegistroActivity.this);
            }
        });
    }

    @Override
    public void onRegistroResult(boolean exito) {
        if (exito) {
            // El registro fue exitoso, muestra un mensaje y regresa a la actividad anterior
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // El registro falló, muestra un mensaje de error
            Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show();
        }
    }
}
