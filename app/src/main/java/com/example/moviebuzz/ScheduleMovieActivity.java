package com.example.moviebuzz;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.*;

public class ScheduleMovieActivity extends AppCompatActivity {
    private TextView titleField;
    private TextInputEditText priceField, timeField, dateText;
    private MaterialButton saveButton, backButton, dateButton;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_movie);


        databaseHelper = DatabaseHelper.getInstance(this);

        initializeViews();
        setupUI();


        String movieTitle = getIntent().getStringExtra("movie_title");
        if (movieTitle != null) {
            titleField.setText(movieTitle);
        }
    }

    private void initializeViews() {
        titleField = findViewById(R.id.titleField);
        priceField = findViewById(R.id.priceField);
        timeField = findViewById(R.id.timeField);
        dateText = findViewById(R.id.dateText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        dateButton = findViewById(R.id.dateButton);
    }

    private void setupUI() {
        saveButton.setOnClickListener(v -> handleSaveShow());
        backButton.setOnClickListener(v -> finish());
        dateButton.setOnClickListener(v -> showDatePicker());


        dateText.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {

                    String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                            year, (monthOfYear + 1), dayOfMonth);
                    dateText.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));


        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void handleSaveShow() {
        String title = titleField.getText().toString().trim();
        String date = (dateText.getText() != null) ? dateText.getText().toString().trim() : "";
        String time = (timeField.getText() != null) ? timeField.getText().toString().trim() : "";
        String price = (priceField.getText() != null) ? priceField.getText().toString().trim() : "";


        if (date.isEmpty()) {
            dateText.setError("Select a date");
            return;
        }
        if (time.isEmpty()) {
            timeField.setError("Enter time (e.g., 7:30 PM)");
            return;
        }
        if (price.isEmpty()) {
            priceField.setError("Enter ticket price");
            return;
        }

        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue <= 0) {
                priceField.setError("Price must be greater than 0");
                return;
            }


            Show show = new Show(title, date, time, priceValue, "Active");

            databaseHelper.addShow(show, new DatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String showId) {
                    runOnUiThread(() -> {
                        Toast.makeText(ScheduleMovieActivity.this,
                                "Show scheduled for " + title, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(ScheduleMovieActivity.this,
                                    "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            });

        } catch (NumberFormatException e) {
            priceField.setError("Invalid number format");
        }
    }
}