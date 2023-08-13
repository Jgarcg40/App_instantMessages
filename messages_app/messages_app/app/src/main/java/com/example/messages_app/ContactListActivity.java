package com.example.messages_app;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactListActivity extends AppCompatActivity {

    private List<String> contactNames = new ArrayList<>();
    private LinearLayout chatListLayout;
    private SharedPreferences sharedPreferences;
    private static final String CONTACTS_KEY = "contacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactlist);

        Intent intent = getIntent();
        final String username = intent.getStringExtra("username");
        final String password = intent.getStringExtra("password");

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        chatListLayout = findViewById(R.id.chat_list_layout);
        Button addContactButton = findViewById(R.id.btn_send_contact);
        Button deleteContactButton = findViewById(R.id.btn_delete_contact);
        final EditText newContactEditText = findViewById(R.id.et_new_contact);
        final EditText removeContactEditText = findViewById(R.id.et_remove_contact);

        // Cargar los contactos guardados desde SharedPreferences para que se guarden los mesnages
        Set<String> savedContacts = sharedPreferences.getStringSet(CONTACTS_KEY, new HashSet<>());
        contactNames.addAll(savedContacts);
        refreshChatList(username, password);

        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newContactName = newContactEditText.getText().toString().trim();
                if (!newContactName.isEmpty()) {
                    addContact(newContactName, username, password);
                    newContactEditText.setText(""); // Limpiar el campo de entrada
                }
            }
        });

        deleteContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contactToRemove = removeContactEditText.getText().toString().trim();
                if (!contactToRemove.isEmpty()) {
                    if (removeContact(contactToRemove, username, password)) {
                        removeContactEditText.setText(""); 
                    } else {
                        Toast.makeText(ContactListActivity.this, "El usuario a eliminar no existe en la lista de contactos", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void addContact(String contactName, String username, String password) {
        contactNames.add(contactName);
        saveContactsToSharedPreferences();
        refreshChatList(username, password);
    }

    private boolean removeContact(String contactName, String username, String password) {
        if (contactNames.contains(contactName)) {
            contactNames.remove(contactName);
            saveContactsToSharedPreferences();
            refreshChatList(username, password);
            return true;
        } else {
            return false;
        }
    }

    private void saveContactsToSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(CONTACTS_KEY, new HashSet<>(contactNames));
        editor.apply();
    }

    private void refreshChatList(String username, String password) {
        chatListLayout.removeAllViews();

        // Itera a través de la lista de contactos y crea vistas de chat
        for (final String contactName : contactNames) {
            TextView chatView = new TextView(this);
            chatView.setText(contactName);
            chatView.setTextSize(32); // Tamaño de texto grande
            chatView.setTextColor(Color.parseColor("#13fc03")); // verde

            chatView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Abre la actividad SendMessageActivity con el nombre de contacto como dato extra
                    Intent intent = new Intent(ContactListActivity.this, SendMessageActivity.class);
                    intent.putExtra("destinatario", contactName); // Envía el nombre de contacto a SendMessageActivity
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);
                    startActivity(intent);
                }
            });
            chatListLayout.addView(chatView);
        }
    }
}
