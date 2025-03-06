package com.example.diplom;

public class BookingModel {
    private String bookingId;
    private String coworkingId;
    private String coworkingName;
    private String userId;
    private String date;
    private int hours;
    private int people;
    private String comment;

    public BookingModel() {}

    // Геттеры и сеттеры
    public String getBookingId() {
        return bookingId;
    }
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    public String getCoworkingId() {
        return coworkingId;
    }
    public void setCoworkingId(String coworkingId) {
        this.coworkingId = coworkingId;
    }
    public String getCoworkingName() {
        return coworkingName;
    }
    public void setCoworkingName(String coworkingName) {
        this.coworkingName = coworkingName;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public int getHours() {
        return hours;
    }
    public void setHours(int hours) {
        this.hours = hours;
    }
    public int getPeople() {
        return people;
    }
    public void setPeople(int people) {
        this.people = people;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
}
