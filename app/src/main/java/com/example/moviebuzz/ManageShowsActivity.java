package com.example.moviebuzz;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;

public class ManageShowsActivity extends AppCompatActivity {
    private RecyclerView showsRecyclerView;
    private MaterialButton viewBookingsButton, cancelShowButton, backButton;
    private DatabaseHelper databaseHelper;
    private List<Show> showList = new ArrayList<>();
    private ShowAdapter showAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_shows);

        databaseHelper = DatabaseHelper.getInstance(this);

        showsRecyclerView = findViewById(R.id.showsRecyclerView);
        viewBookingsButton = findViewById(R.id.viewBookingsButton);
        cancelShowButton = findViewById(R.id.cancelShowButton);
        backButton = findViewById(R.id.backButton);

        showAdapter = new ShowAdapter();
        showsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        showsRecyclerView.setAdapter(showAdapter);

        setupListeners();
        loadShows();
    }

    private void setupListeners() {
        viewBookingsButton.setOnClickListener(v -> {
            Show selected = showAdapter.getSelectedShow();
            if (selected != null) {
                Intent intent = new Intent(this, ShowBookingsActivity.class);
                intent.putExtra("show_id", selected.getShowId());
                intent.putExtra("show_movie", selected.getMovieTitle());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select a show", Toast.LENGTH_SHORT).show();
            }
        });

        cancelShowButton.setOnClickListener(v -> {
            Show selected = showAdapter.getSelectedShow();
            if (selected != null) {
                if (!"Cancelled".equalsIgnoreCase(selected.getStatus())) {
                    confirmCancellation(selected);
                } else {
                    Toast.makeText(this, "Show is already cancelled", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Select a show to cancel", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void loadShows() {
        databaseHelper.getAllShows(new DatabaseHelper.DatabaseCallback<List<Show>>() {
            @Override
            public void onSuccess(List<Show> shows) {
                showList.clear();
                if (shows != null) {
                    showList.addAll(shows);


                    Collections.sort(showList, (s1, s2) ->
                            s1.getMovieTitle().compareToIgnoreCase(s2.getMovieTitle())
                    );
                }
                runOnUiThread(() -> showAdapter.notifyDataSetChanged());
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void confirmCancellation(Show show) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Refund & Cancel")
                .setMessage("Are you sure you want to cancel '" + show.getMovieTitle() + "'? All purchased tickets will be refunded to users' balances.")
                .setPositiveButton("Process Refunds", (dialog, which) -> {
                    databaseHelper.updateShowStatus(show.getShowId(), "Cancelled", new DatabaseHelper.DatabaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            startRefundProcess(show);
                        }
                        @Override public void onFailure(Exception e) {
                            runOnUiThread(() -> Toast.makeText(ManageShowsActivity.this, "Failed to cancel show", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Abort", null)
                .show();
    }

    private void startRefundProcess(Show show) {
        databaseHelper.getBookingsByShow(show.getShowId(), new DatabaseHelper.DatabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                if (bookings == null || bookings.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ManageShowsActivity.this, "Show Cancelled (No bookings to refund)", Toast.LENGTH_SHORT).show();
                        loadShows();
                    });
                    return;
                }

                for (Booking b : bookings) {
                    if (b != null && "Confirmed".equalsIgnoreCase(b.getStatus())) {
                        processSingleRefund(b);
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(ManageShowsActivity.this, "Refunds processed successfully", Toast.LENGTH_LONG).show();
                    loadShows();
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void processSingleRefund(Booking b) {
        databaseHelper.getUser(b.getUsername(), new DatabaseHelper.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    double refundAmt = b.getAmountPaid();
                    double newBalance = user.getBalance() + refundAmt;

                    databaseHelper.updateUserBalance(user.getUsername(), newBalance, new DatabaseHelper.DatabaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void r) {
                            databaseHelper.updateBookingStatus(b.getBookingId(), "Refunded", null);
                        }
                        @Override public void onFailure(Exception e) {}
                    });
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private class ShowAdapter extends RecyclerView.Adapter<ShowAdapter.ShowViewHolder> {
        private int selectedPos = -1;

        @Override
        public ShowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_show_manage, parent, false);
            return new ShowViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ShowViewHolder holder, int position) {
            Show show = showList.get(position);
            holder.title.setText(show.getMovieTitle());
            holder.dateTime.setText(show.getShowDate() + " @ " + show.getShowTime());
            holder.price.setText("BDT " + String.format("%.0f", show.getPrice()));

            String status = show.getStatus() != null ? show.getStatus().toUpperCase() : "ACTIVE";
            holder.status.setText(status);

            if ("CANCELLED".equalsIgnoreCase(status)) {
                holder.status.setTextColor(0xFFD32F2F);
                holder.status.setBackgroundColor(0xFFFFEBEE);
            } else {
                holder.status.setTextColor(0xFF388E3C);
                holder.status.setBackgroundColor(0xFFE8F5E9);
            }

            holder.card.setStrokeWidth(selectedPos == position ? 8 : 2);
            holder.card.setStrokeColor(selectedPos == position ? 0xFF3498DB : 0xFFDDDDDD);

            holder.itemView.setOnClickListener(v -> {
                int previous = selectedPos;
                selectedPos = holder.getAdapterPosition();
                if (previous != -1) notifyItemChanged(previous);
                notifyItemChanged(selectedPos);
            });
        }

        @Override public int getItemCount() { return showList.size(); }
        public Show getSelectedShow() {
            return (selectedPos != -1 && selectedPos < showList.size()) ? showList.get(selectedPos) : null;
        }

        class ShowViewHolder extends RecyclerView.ViewHolder {
            TextView title, dateTime, price, status;
            MaterialCardView card;
            public ShowViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.manageMovieTitle);
                dateTime = v.findViewById(R.id.manageDateTime);
                price = v.findViewById(R.id.managePrice);
                status = v.findViewById(R.id.manageStatus);
                card = (MaterialCardView) v;
            }
        }
    }
}