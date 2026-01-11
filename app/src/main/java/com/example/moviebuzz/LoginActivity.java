package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private Button loginButton, adminToggleButton;
    private TextView errorMessage, createAccountLink, backButton, loginHeader;

    private boolean isAdminMode = false;
    private DatabaseHelper databaseHelper;
    private UserSession userSession;

    // 🔐 HARD-CODED ADMIN CREDENTIALS
    private static final String ADMIN_USERNAME = "bb";
    private static final String ADMIN_PASSWORD = "bb96";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = DatabaseHelper.getInstance(this);
        userSession = UserSession.getInstance(this);

        if (userSession.isLoggedIn()) {
            redirectUser();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        adminToggleButton = findViewById(R.id.adminToggleButton);
        errorMessage = findViewById(R.id.errorMessage);
        createAccountLink = findViewById(R.id.createAccountLink);
        backButton = findViewById(R.id.backButton);
        loginHeader = findViewById(R.id.loginHeader);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        adminToggleButton.setOnClickListener(v -> toggleAdminMode());
        backButton.setOnClickListener(v -> toggleAdminMode());

        createAccountLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private void toggleAdminMode() {
        isAdminMode = !isAdminMode;

        loginHeader.setText(
                isAdminMode
                        ? getString(R.string.admin_portal)
                        : getString(R.string.user_login)
        );

        adminToggleButton.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);
        backButton.setVisibility(isAdminMode ? View.VISIBLE : View.GONE);
        createAccountLink.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);

        usernameField.setText("");
        passwordField.setText("");
        errorMessage.setVisibility(View.GONE);
    }

    private void handleLogin() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fill_fields));
            return;
        }

        // ✅ HARD-CODED ADMIN LOGIN (ONLY THIS PART ADDED)
        if (isAdminMode &&
                username.equals(ADMIN_USERNAME) &&
                password.equals(ADMIN_PASSWORD)) {

            userSession.login(username, "admin");
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        // 🔹 CUSTOMER LOGIN (UNCHANGED)
        databaseHelper.getUser(username,
                new DatabaseHelper.DatabaseCallback<User>() {

                    @Override
                    public void onSuccess(User user) {
                        if (user != null && user.getPassword().equals(password)) {
                            if (!isAdminMode &&
                                    "customer".equalsIgnoreCase(user.getRole())) {

                                loginSuccess(username, user.getRole(),
                                        CustomerMainActivity.class);
                            } else {
                                showError(getString(R.string.error_access_denied));
                            }
                        } else {
                            showError(getString(R.string.error_invalid_credentials));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showError(getString(R.string.error_database) + e.getMessage());
                    }
                });
    }

    private void loginSuccess(String username, String role, Class<?> target) {
        userSession.login(username, role);
        startActivity(new Intent(this, target));
        finish();
    }

    private void redirectUser() {
        startActivity(new Intent(
                this,
                userSession.isAdmin()
                        ? AdminDashboardActivity.class
                        : CustomerMainActivity.class
        ));
        finish();
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }
}