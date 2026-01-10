package com.example.moviebuzz;

import android.content.Context;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static DatabaseHelper instance;
    private final DatabaseReference databaseRef;

    private DatabaseHelper(Context context) {

        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public void getUser(String username, DatabaseCallback<User> callback) {
        databaseRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onSuccess(snapshot.getValue(User.class));
            }
            @Override
            public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
        });
    }

    public void addUser(User user, DatabaseCallback<Void> callback) {
        databaseRef.child("users").child(user.getUsername()).setValue(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateUserBalance(String username, double newBalance, DatabaseCallback<Void> callback) {
        databaseRef.child("users").child(username).child("balance").setValue(newBalance)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void addMovie(Movie movie, DatabaseCallback<String> callback) {
        String movieId = databaseRef.child("movies").push().getKey();
        if (movieId != null) {
            movie.setId(movieId);
            databaseRef.child("movies").child(movieId).setValue(movie)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(movieId))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void getAllMovies(DatabaseCallback<List<Movie>> callback) {
        databaseRef.child("movies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Movie> movies = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Movie movie = snap.getValue(Movie.class);
                    if (movie != null) {
                        movie.setId(snap.getKey());
                        movies.add(movie);
                    }
                }
                callback.onSuccess(movies);
            }
            @Override
            public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
        });
    }

    public void deleteMovie(String movieId, DatabaseCallback<Void> callback) {
        databaseRef.child("movies").child(movieId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void addShow(Show show, DatabaseCallback<String> callback) {
        String showId = databaseRef.child("shows").push().getKey();
        if (showId != null) {
            show.setShowId(showId);
            databaseRef.child("shows").child(showId).setValue(show)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(showId))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void getAllShows(DatabaseCallback<List<Show>> callback) {
        databaseRef.child("shows").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Show> shows = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Show show = snap.getValue(Show.class);
                    if (show != null) {
                        show.setShowId(snap.getKey());
                        shows.add(show);
                    }
                }
                callback.onSuccess(shows);
            }
            @Override
            public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
        });
    }

    public void getShowsByMovie(String movieTitle, DatabaseCallback<List<Show>> callback) {
        databaseRef.child("shows").orderByChild("movieTitle").equalTo(movieTitle)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Show> list = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Show s = snap.getValue(Show.class);
                            if (s != null && "Active".equals(s.getStatus())) {
                                s.setShowId(snap.getKey());
                                list.add(s);
                            }
                        }
                        callback.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
                });
    }

    public void updateShowStatus(String showId, String status, DatabaseCallback<Void> callback) {
        databaseRef.child("shows").child(showId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }


    public void getBookingsByUser(String username, DatabaseCallback<List<Booking>> callback) {
        databaseRef.child("bookings").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Booking> bookings = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Booking b = snap.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(snap.getKey());
                                bookings.add(b);
                            }
                        }
                        callback.onSuccess(bookings);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
                });
    }

    public void addBooking(Booking booking, DatabaseCallback<String> callback) {
        String bookingId = databaseRef.child("bookings").push().getKey();
        if (bookingId != null) {
            booking.setBookingId(bookingId);
            databaseRef.child("bookings").child(bookingId).setValue(booking)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(bookingId))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void getBookingsByShow(String showId, DatabaseCallback<List<Booking>> callback) {
        databaseRef.child("bookings").orderByChild("showId").equalTo(showId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Booking> bookings = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Booking b = snap.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(snap.getKey());
                                bookings.add(b);
                            }
                        }
                        callback.onSuccess(bookings);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { callback.onFailure(error.toException()); }
                });
    }

    public void updateBookingStatus(String bookingId, String status, DatabaseCallback<Void> callback) {
        databaseRef.child("bookings").child(bookingId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}