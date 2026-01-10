package com.example.moviebuzz;

public class Booking {
    private String bookingId;
    private String username;
    private String showId;
    private String movieTitle;
    private String showDate;
    private String showTime;
    private double amountPaid;
    private int ticketCount;
    private String status;

    public Booking() {

    }

    public Booking(String username, String showId, String movieTitle, String showDate,
                   String showTime, double amountPaid, int ticketCount, String status) {
        this.username = username;
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.showDate = showDate;
        this.showTime = showTime;
        this.amountPaid = amountPaid;
        this.ticketCount = ticketCount;
        this.status = status;
    }


    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getShowId() { return showId; }
    public void setShowId(String showId) { this.showId = showId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getShowDate() { return showDate; }
    public void setShowDate(String showDate) { this.showDate = showDate; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}