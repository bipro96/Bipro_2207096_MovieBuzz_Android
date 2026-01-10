package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextInputEditText apiSearchField;
    private TextView statusLabel;
    private RecyclerView movieRecyclerView;
    private MaterialButton searchButton, scheduleButton, viewShowsButton, deleteButton, logoutButton;

    private DatabaseHelper databaseHelper;
    private UserSession userSession;

    private final List<Movie> movieList = new ArrayList<>();
    private MovieAdapter movieAdapter;

    private static final String API_KEY = "48e4feec";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        databaseHelper = DatabaseHelper.getInstance(this);
        userSession = UserSession.getInstance(this);


        if (!userSession.isLoggedIn() || !userSession.isAdmin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupRecycler();
        setupListeners();
        loadMovies();
    }

    private void initViews() {
        apiSearchField = findViewById(R.id.apiSearchField);
        statusLabel = findViewById(R.id.statusLabel);
        movieRecyclerView = findViewById(R.id.movieRecyclerView);
        searchButton = findViewById(R.id.searchButton);
        scheduleButton = findViewById(R.id.scheduleButton);
        viewShowsButton = findViewById(R.id.viewShowsButton);
        deleteButton = findViewById(R.id.deleteButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupRecycler() {
        movieAdapter = new MovieAdapter();
        movieRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        movieRecyclerView.setAdapter(movieAdapter);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> searchMovieFromApi());

        scheduleButton.setOnClickListener(v -> {
            Movie selected = movieAdapter.getSelectedMovie();
            if (selected != null) {
                Intent intent = new Intent(this, ScheduleMovieActivity.class);
                intent.putExtra("movie_title", selected.getTitle());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Select a movie first", Toast.LENGTH_SHORT).show();
            }
        });

        viewShowsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ManageShowsActivity.class)));


        deleteButton.setOnClickListener(v -> {
            Movie selected = movieAdapter.getSelectedMovie();
            if (selected != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Movie")
                        .setMessage("Are you sure you want to delete " + selected.getTitle() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteMovie(selected))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(this, "Please select a movie from the list", Toast.LENGTH_SHORT).show();
            }
        });

        logoutButton.setOnClickListener(v -> {
            userSession.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void searchMovieFromApi() {
        String query = apiSearchField.getText().toString().trim();
        if (query.isEmpty()) {
            apiSearchField.setError("Enter title");
            return;
        }

        statusLabel.setText("Searching OMDb...");
        String urlStr = "https://www.omdbapi.com/?t=" + query.replace(" ", "+") + "&apikey=" + API_KEY;

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder json = new StringBuilder();
                while (scanner.hasNext()) json.append(scanner.nextLine());
                scanner.close();
                runOnUiThread(() -> parseAndSaveMovie(json.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> statusLabel.setText("Network Error"));
            }
        }).start();
    }

    private void parseAndSaveMovie(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Movie movie = mapper.readValue(json, Movie.class);

            if (movie.getTitle() == null) {
                statusLabel.setText("Movie not found");
                return;
            }

            databaseHelper.getAllMovies(new DatabaseHelper.DatabaseCallback<List<Movie>>() {
                @Override
                public void onSuccess(List<Movie> existingMovies) {
                    for (Movie m : existingMovies) {
                        if (m.getTitle().equalsIgnoreCase(movie.getTitle())) {
                            runOnUiThread(() -> statusLabel.setText("Already exists!"));
                            return;
                        }
                    }

                    databaseHelper.addMovie(movie, new DatabaseHelper.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            runOnUiThread(() -> {
                                statusLabel.setText("Added: " + movie.getTitle());
                                apiSearchField.setText("");
                                loadMovies();
                            });
                        }
                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() -> statusLabel.setText("Save failed"));
                        }
                    });
                }
                @Override
                public void onFailure(Exception e) { runOnUiThread(() -> statusLabel.setText("Sync error")); }
            });
        } catch (Exception e) { statusLabel.setText("Parsing error"); }
    }

    private void loadMovies() {
        databaseHelper.getAllMovies(new DatabaseHelper.DatabaseCallback<List<Movie>>() {
            @Override
            public void onSuccess(List<Movie> movies) {
                movieList.clear();
                movieList.addAll(movies);
                runOnUiThread(() -> {
                    movieAdapter.resetSelection(); // Reset selection on refresh
                    movieAdapter.notifyDataSetChanged();
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> statusLabel.setText("Error loading movies"));
            }
        });
    }

    private void deleteMovie(Movie movie) {
        statusLabel.setText("Deleting...");
        databaseHelper.deleteMovie(movie.getId(), new DatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    statusLabel.setText("Movie Deleted");
                    loadMovies(); // Refresh list after successful delete
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> statusLabel.setText("Delete failed: " + e.getMessage()));
            }
        });
    }


    private class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.Holder> {
        private int selectedPos = -1;

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_admin, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            Movie m = movieList.get(pos);
            h.title.setText(m.getTitle());
            h.genre.setText(m.getGenre());
            h.duration.setText(m.getDuration());


            h.card.setStrokeColor(selectedPos == pos ? 0xFF1A73E8 : 0x00000000);
            h.card.setStrokeWidth(selectedPos == pos ? 6 : 0);

            h.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPos;
                selectedPos = h.getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPos);
            });
        }

        @Override
        public int getItemCount() { return movieList.size(); }

        void resetSelection() { selectedPos = -1; }

        Movie getSelectedMovie() {
            return (selectedPos >= 0 && selectedPos < movieList.size()) ? movieList.get(selectedPos) : null;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, genre, duration;
            com.google.android.material.card.MaterialCardView card;
            Holder(View v) {
                super(v);
                card = (com.google.android.material.card.MaterialCardView) v;
                title = v.findViewById(R.id.movieTitle);
                genre = v.findViewById(R.id.movieGenre);
                duration = v.findViewById(R.id.movieDuration);
            }
        }
    }
}
