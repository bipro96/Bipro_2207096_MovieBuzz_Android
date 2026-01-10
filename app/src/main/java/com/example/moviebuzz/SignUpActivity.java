package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {
    private EditText newEmailField, newUsernameField, newPasswordField;
    private Button registerButton;
    private TextView loginLink;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        databaseHelper = DatabaseHelper.getInstance(this);
        initializeViews();
        setupUI();
    }

    private void initializeViews() {
        newEmailField = findViewById(R.id.newEmail);
        newUsernameField = findViewById(R.id.newUsername);
        newPasswordField = findViewById(R.id.newPassword);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
    }

    private void setupUI() {
        registerButton.setOnClickListener(v -> handleSignUp());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSignUp() {
        String email = newEmailField.getText().toString().trim();
        String username = newUsernameField.getText().toString().trim();
        String password = newPasswordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in username and password", Toast.LENGTH_SHORT).show();
            return;
        }


        databaseHelper.getUser(username, new DatabaseHelper.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User existingUser) {
                if (existingUser != null) {
                    Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                } else {

                    User newUser = new User(username, password, "customer", 0.0);
                    databaseHelper.addUser(newUser, new DatabaseHelper.DatabaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(SignUpActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SignUpActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}