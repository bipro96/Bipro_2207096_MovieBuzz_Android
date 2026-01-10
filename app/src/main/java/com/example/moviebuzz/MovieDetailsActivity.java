package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;

public class MovieDetailsActivity extends AppCompatActivity {
    private ImageView posterImage;
    private TextView titleText, genreText, balanceText, statusText;
    private Spinner ticketSpinner;
    private RecyclerView showsRecyclerView;
    private MaterialButton confirmButton, backButton;

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private Movie currentMovie;
    private final List<Show> showList = new ArrayList<>();
    private final Map<String, List<Booking>> showBookingsMap = new HashMap<>();
    private ShowAdapter showAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        databaseHelper = DatabaseHelper.getInstance(this);
        userSession = UserSession.getInstance(this);

        initializeViews();
        setupUI();
        loadMovieDetails();
        loadBalance();
    }

    private void initializeViews() {
        posterImage = findViewById(R.id.detailPoster);
        titleText = findViewById(R.id.detailTitle);
        genreText = findViewById(R.id.detailGenre);
        balanceText = findViewById(R.id.balanceLabel);
        statusText = findViewById(R.id.statusLabel);
        ticketSpinner = findViewById(R.id.ticketSpinner);
        showsRecyclerView = findViewById(R.id.showsRecyclerView);
        confirmButton = findViewById(R.id.btnConfirm);
        backButton = findViewById(R.id.backButton);
    }

    private void setupUI() {
        String[] ticketCounts = new String[10];
        for (int i = 0; i < 10; i++) ticketCounts[i] = String.valueOf(i + 1);
        ticketSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ticketCounts));

        showAdapter = new ShowAdapter();
        showsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        showsRecyclerView.setAdapter(showAdapter);

        confirmButton.setOnClickListener(v -> handleConfirmBooking());
        backButton.setOnClickListener(v -> finish());
    }

    private void loadMovieDetails() {
        Intent intent = getIntent();


        String title = intent.getStringExtra("movie_title");
        String genre = intent.getStringExtra("movie_genre");
        String posterUrl = intent.getStringExtra("movie_poster"); // API URL string
        String movieId = intent.getStringExtra("movie_id");

        currentMovie = new Movie(title, genre, "", posterUrl);
        currentMovie.setId(movieId);


        titleText.setText(title != null ? title : "Unknown Movie");
        genreText.setText(genre != null && !genre.equals("null") ? genre : "Action/Drama");


        if (posterUrl != null && !posterUrl.isEmpty() && !posterUrl.equalsIgnoreCase("null")) {
            Glide.with(this)
                    .load(posterUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache API images for speed
                    .placeholder(R.drawable.placeholder_movie) // Show while loading
                    .error(R.drawable.placeholder_movie)       // Show if URL fails
                    .centerCrop()
                    .into(posterImage);
        } else {
            posterImage.setImageResource(R.drawable.placeholder_movie);
        }

        loadUserBookings();
    }



    private void loadUserBookings() {
        databaseHelper.getBookingsByUser(userSession.getUsername(), new DatabaseHelper.DatabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                showBookingsMap.clear();
                if (bookings != null) {
                    for (Booking b : bookings) {
                        if (b.getMovieTitle().equalsIgnoreCase(currentMovie.getTitle())) {
                            if (!showBookingsMap.containsKey(b.getShowId()))
                                showBookingsMap.put(b.getShowId(), new ArrayList<>());
                            showBookingsMap.get(b.getShowId()).add(b);
                        }
                    }
                }
                loadAvailableShows();
            }
            @Override public void onFailure(Exception e) { loadAvailableShows(); }
        });
    }

    private void loadAvailableShows() {
        databaseHelper.getShowsByMovie(currentMovie.getTitle(), new DatabaseHelper.DatabaseCallback<List<Show>>() {
            @Override
            public void onSuccess(List<Show> shows) {
                showList.clear();
                if (shows != null) showList.addAll(shows);
                runOnUiThread(() -> showAdapter.notifyDataSetChanged());
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void loadBalance() {
        databaseHelper.getUser(userSession.getUsername(), new DatabaseHelper.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    runOnUiThread(() -> balanceText.setText("Balance: BDT " + String.format("%.2f", user.getBalance())));
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void handleConfirmBooking() {
        Show selected = showAdapter.getSelectedShow();
        if (selected == null) {
            statusText.setText("Please select a showtime.");
            return;
        }
        int qty = Integer.parseInt(ticketSpinner.getSelectedItem().toString());
        double total = selected.getPrice() * qty;
        databaseHelper.getUser(userSession.getUsername(), new DatabaseHelper.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getBalance() >= total) {
                    databaseHelper.updateUserBalance(userSession.getUsername(), user.getBalance() - total, new DatabaseHelper.DatabaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void r) {
                            Booking b = new Booking(userSession.getUsername(), selected.getShowId(), currentMovie.getTitle(),
                                    selected.getShowDate(), selected.getShowTime(), total, qty, "Confirmed");
                            databaseHelper.addBooking(b, new DatabaseHelper.DatabaseCallback<String>() {
                                @Override
                                public void onSuccess(String id) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(MovieDetailsActivity.this, "Booked Successfully!", Toast.LENGTH_SHORT).show();
                                        loadBalance();
                                        loadUserBookings();
                                    });
                                }
                                @Override public void onFailure(Exception e) {}
                            });
                        }
                        @Override public void onFailure(Exception e) {}
                    });
                } else {
                    runOnUiThread(() -> statusText.setText("Insufficient Balance!"));
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private class ShowAdapter extends RecyclerView.Adapter<ShowAdapter.ShowViewHolder> {
        private int selectedPos = -1;
        @NonNull @Override public ShowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_show, parent, false);
            return new ShowViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ShowViewHolder holder, int position) {
            Show show = showList.get(position);
            holder.movieName.setText(currentMovie.getTitle());
            holder.date.setText(show.getShowDate());
            holder.time.setText(show.getShowTime());
            holder.price.setText("BDT " + (int)show.getPrice());
            List<Booking> bookings = showBookingsMap.get(show.getShowId());
            if (bookings != null && !bookings.isEmpty()) {
                int t = 0; double s = 0;
                for (Booking b : bookings) { t += b.getTicketCount(); s += b.getAmountPaid(); }
                holder.ownedLayout.setVisibility(View.VISIBLE);
                holder.txtOwned.setText("Tickets Owned: " + t);
                holder.txtSpent.setText("Total Spent: BDT " + String.format("%.2f", s));
            } else { holder.ownedLayout.setVisibility(View.GONE); }
            holder.cardView.setStrokeColor(selectedPos == position ? 0xFF00D2FF : 0xFF333333);
            holder.cardView.setStrokeWidth(selectedPos == position ? 4 : 2);
            holder.itemView.setOnClickListener(v -> {
                int old = selectedPos;
                selectedPos = holder.getAdapterPosition();
                notifyItemChanged(old);
                notifyItemChanged(selectedPos);
            });
        }
        @Override public int getItemCount() { return showList.size(); }
        public Show getSelectedShow() { return selectedPos != -1 ? showList.get(selectedPos) : null; }
        class ShowViewHolder extends RecyclerView.ViewHolder {
            TextView movieName, date, time, price, txtOwned, txtSpent;
            LinearLayout ownedLayout;
            MaterialCardView cardView;
            public ShowViewHolder(View v) {
                super(v);
                movieName = v.findViewById(R.id.showMovie);
                date = v.findViewById(R.id.showDate);
                time = v.findViewById(R.id.showTime);
                price = v.findViewById(R.id.showPrice);
                txtOwned = v.findViewById(R.id.txtTicketsOwned);
                txtSpent = v.findViewById(R.id.txtTotalSpent);
                ownedLayout = v.findViewById(R.id.ownedStatusLayout);
                cardView = (MaterialCardView) v;
            }
        }
    }
}