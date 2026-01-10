package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import java.util.*;

public class CustomerMainActivity extends AppCompatActivity {

    private RecyclerView movieRecyclerView;
    private TextView balanceText;
    private MaterialButton rechargeButton, logoutButton;

    private DatabaseHelper databaseHelper;
    private UserSession userSession;

    private final List<Movie> movieList = new ArrayList<>();
    private MovieAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        databaseHelper = DatabaseHelper.getInstance(this);
        userSession = UserSession.getInstance(this);

        if (!userSession.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        setupRecycler();
        setupButtons();
        loadMovies();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadBalance();
    }

    private void bindViews() {
        movieRecyclerView = findViewById(R.id.movieRecyclerView);
        balanceText = findViewById(R.id.balanceText);
        rechargeButton = findViewById(R.id.rechargeButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupRecycler() {
        int span = getResources().getConfiguration().screenWidthDp > 600 ? 4 : 2;
        movieRecyclerView.setLayoutManager(new GridLayoutManager(this, span));
        adapter = new MovieAdapter();
        movieRecyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        logoutButton.setOnClickListener(v -> {
            userSession.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        rechargeButton.setOnClickListener(v -> showRechargeDialog());
    }

    private void showRechargeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recharge Account");
        builder.setMessage("Enter amount in BDT:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("e.g. 500");

        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = marginPx;
        params.rightMargin = marginPx;
        params.topMargin = marginPx / 2;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Add Balance", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amountToAdd = Double.parseDouble(amountStr);
                    if (amountToAdd > 0) {
                        performRecharge(amountToAdd);
                    } else {
                        Toast.makeText(this, "Enter a positive amount", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performRecharge(double amountToAdd) {
        float currentBal = userSession.getBalance();
        double newTotalBalance = (double) currentBal + amountToAdd;

        databaseHelper.updateUserBalance(userSession.getUsername(), newTotalBalance, new DatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerMainActivity.this,
                            "Successfully recharged BDT " + String.format("%.2f", amountToAdd),
                            Toast.LENGTH_SHORT).show();
                    loadBalance();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(CustomerMainActivity.this, "Recharge failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadMovies() {
        databaseHelper.getAllMovies(new DatabaseHelper.DatabaseCallback<List<Movie>>() {
            @Override
            public void onSuccess(List<Movie> movies) {
                movieList.clear();
                movieList.addAll(movies);
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(CustomerMainActivity.this, "Failed to load movies", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadBalance() {
        databaseHelper.getUser(userSession.getUsername(), new DatabaseHelper.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    userSession.setBalance((float) user.getBalance());
                    runOnUiThread(() -> balanceText.setText(String.format("BDT %.2f", user.getBalance())));
                }
            }
            @Override public void onFailure(Exception e) {
                runOnUiThread(() -> balanceText.setText("BDT 0.00"));
            }
        });
    }


    private class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.Holder> {
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_customer, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            Movie m = movieList.get(position);
            h.title.setText(m.getTitle());
            if (m.getGenre() != null) h.genre.setText(m.getGenre());

            Glide.with(CustomerMainActivity.this)
                    .load(m.getPosterPath())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_movie)
                    .into(h.poster);



            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, MovieDetailsActivity.class);

                intent.putExtra("movie_id", m.getId());
                intent.putExtra("movie_title", m.getTitle());
                intent.putExtra("movie_genre", m.getGenre());      // Fixed: Send Genre
                intent.putExtra("movie_poster", m.getPosterPath()); // Fixed: Send Poster URL

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return movieList.size(); }

        class Holder extends RecyclerView.ViewHolder {
            ImageView poster;
            TextView title, genre;
            Holder(View v) {
                super(v);
                poster = v.findViewById(R.id.moviePoster);
                title = v.findViewById(R.id.movieTitle);
                genre = v.findViewById(R.id.movieGenre);
            }
        }
    }
}