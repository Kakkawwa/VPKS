package com.example.diplom;

public class Zone {
    private String name;
    private String places;
    private String price;
    private String priceType;

    public Zone(String name, String places, String price, String priceType) {
        this.name = name;
        this.places = places;
        this.price = price;
        this.priceType = priceType;
    }

    public String getName() {
        return name;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaces() {
        return places;
    }

    public void setPlaces(String places) {
        this.places = places;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

}
