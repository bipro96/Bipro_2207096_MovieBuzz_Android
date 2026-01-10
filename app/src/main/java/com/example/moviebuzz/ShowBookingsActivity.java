package com.example.moviebuzz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShowBookingsActivity extends AppCompatActivity {

    private TextView showInfoLabel, totalSoldLabel;
    private RecyclerView bookingsRecyclerView;
    private Button closeButton;

    private DatabaseHelper databaseHelper;
    private final List<Booking> bookingList = new ArrayList<>();
    private BookingAdapter bookingAdapter;

    private String showId, showMovie, showDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_bookings);


        databaseHelper = DatabaseHelper.getInstance(this);


        showId = getIntent().getStringExtra("show_id");
        showMovie = getIntent().getStringExtra("show_movie");
        showDate = getIntent().getStringExtra("show_date");

        initViews();
        setupRecyclerView();
        loadBookings();
    }

    private void initViews() {
        showInfoLabel = findViewById(R.id.showInfoLabel);
        totalSoldLabel = findViewById(R.id.totalSoldLabel);
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
        closeButton = findViewById(R.id.closeButton);


        String header = (showMovie != null ? showMovie : "Show") + " @ " + (showDate != null ? showDate : "");
        showInfoLabel.setText(header);

        closeButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter();
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingsRecyclerView.setAdapter(bookingAdapter);
    }

    private void loadBookings() {
        databaseHelper.getBookingsByShow(showId,
                new DatabaseHelper.DatabaseCallback<List<Booking>>() {

                    @Override
                    public void onSuccess(List<Booking> bookings) {
                        bookingList.clear();
                        bookingList.addAll(bookings);

                        int activeTickets = 0;
                        for (Booking booking : bookings) {
                            // Sum up only active/confirmed bookings
                            if ("Confirmed".equalsIgnoreCase(booking.getStatus())) {
                                activeTickets += booking.getTicketCount();
                            }
                        }

                        final int total = activeTickets;
                        runOnUiThread(() -> {
                            bookingAdapter.notifyDataSetChanged();
                            totalSoldLabel.setText("Total Tickets Sold: " + total);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(ShowBookingsActivity.this,
                                        "Error loading bookings", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }



    private class BookingAdapter
            extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

        @Override
        public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_booking, parent, false);
            return new BookingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BookingViewHolder holder, int position) {
            Booking booking = bookingList.get(position);

            holder.usernameText.setText(booking.getUsername());
            holder.quantityText.setText(String.valueOf(booking.getTicketCount()));


            holder.paidText.setText(String.format("BDT %.2f", booking.getAmountPaid()));

            holder.statusText.setText(booking.getStatus());


            int color;
            if ("Confirmed".equalsIgnoreCase(booking.getStatus())) {
                color = android.R.color.holo_green_dark;
            } else if ("Refunded".equalsIgnoreCase(booking.getStatus())) {
                color = android.R.color.holo_orange_dark;
            } else {
                color = android.R.color.holo_red_dark;
            }

            holder.statusText.setTextColor(getResources().getColor(color));
        }

        @Override
        public int getItemCount() {
            return bookingList.size();
        }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            TextView usernameText, quantityText, paidText, statusText;

            BookingViewHolder(View itemView) {
                super(itemView);
                usernameText = itemView.findViewById(R.id.bookingUsername);
                quantityText = itemView.findViewById(R.id.bookingQuantity);
                paidText = itemView.findViewById(R.id.bookingPaid);
                statusText = itemView.findViewById(R.id.bookingStatus);
            }
        }
    }
}