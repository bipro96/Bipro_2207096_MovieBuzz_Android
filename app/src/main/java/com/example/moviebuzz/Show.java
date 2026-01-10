package com.example.moviebuzz;

public class Show {
    private String showId;
    private String movieTitle;
    private String showDate;
    private String showTime;
    private double price;
    private String status;

    public Show() {

    }

    public Show(String movieTitle, String showDate, String showTime, double price, String status) {
        this.movieTitle = movieTitle;
        this.showDate = showDate;
        this.showTime = showTime;
        this.price = price;
        this.status = status;
    }


    public String getShowId() { return showId; }
    public void setShowId(String showId) { this.showId = showId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getShowDate() { return showDate; }
    public void setShowDate(String showDate) { this.showDate = showDate; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}