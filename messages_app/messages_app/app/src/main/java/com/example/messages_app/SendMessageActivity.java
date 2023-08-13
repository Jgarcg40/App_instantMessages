package com.example.messages_app;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


public class SendMessageActivity extends AppCompatActivity {

    private EditText etMensaje;
    private Button btnEnviar;
    private TextView tvMensajesRecibidos;
    private ScrollView scrollView;

    private ClientManager clientManager;
    private String destinatario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        //traigo de contact los datos
        Intent intent = getIntent();
        destinatario = intent.getStringExtra("destinatario");
        TextView tvDestinatario = findViewById(R.id.tv_destinatario);
        tvDestinatario.setText(destinatario);

        etMensaje = findViewById(R.id.et_mensaje);
        btnEnviar = findViewById(R.id.btn_enviar);
        tvMensajesRecibidos = findViewById(R.id.tv_mensajes);
        scrollView = findViewById(R.id.scroll_view);

        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        clientManager = new ClientManager(username, password);
        //inicio la recepcion de mensages
        clientManager.connect();
        clientManager.startReceiveThread();

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientManager.connect();

                String mensaje = etMensaje.getText().toString();
                clientManager.enqueueMessage(destinatario, mensaje);
                mostrarMensajeEnTextView("Tú", mensaje);
                etMensaje.setText("");
            }
        });

        clientManager.setReceiveListener(new ClientManager.ReceiveListener() {
            @Override
            public void onMessageReceived(String remitente, String mensaje) {
                runOnUiThread(() -> {
                    mostrarMensajeEnTextView(remitente, mensaje);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences(destinatario, MODE_PRIVATE);
        String mensajesGuardados = sharedPreferences.getString("mensajes", "");
        tvMensajesRecibidos.setText(mensajesGuardados);

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void guardarTextoEnSharedPreferences(String nuevoMensaje) {
        SharedPreferences sharedPreferences = getSharedPreferences(destinatario, MODE_PRIVATE);
        String mensajesAnteriores = sharedPreferences.getString("mensajes", "");
        mensajesAnteriores += nuevoMensaje + "\n";
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("mensajes", mensajesAnteriores);
        editor.apply();
    }
    
    private void mostrarMensajeEnTextView(String remitente, String mensaje) {
        Log.d("SendMessageActivity", "Mostrando mensaje en TextView: " + remitente + "--> " + mensaje);
        if (remitente.startsWith("OK")) {
            remitente = remitente.substring(2);
        }
        String mensajeCompleto = remitente + " --> " + mensaje + "\n";

        SpannableString spannableString = new SpannableString(mensajeCompleto);

        int color;
        if (remitente.equals("Tú")) {
            color = Color.BLUE;
        } else {
            color = Color.RED;
        }

        spannableString.setSpan(new ForegroundColorSpan(color), 0, remitente.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        guardarTextoEnSharedPreferences(mensajeCompleto);

        tvMensajesRecibidos.append(spannableString);
    }
}
