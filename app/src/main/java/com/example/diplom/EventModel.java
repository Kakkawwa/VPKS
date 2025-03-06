package com.example.diplom;

public class EventModel {
    private String id;
    private String name;
    private String category;
    private String date;
    private String startTime;
    private String endTime;
    private String description;
    private String coworkingId;
    private String coworkingName;
    private String creatorId;

    public EventModel() {}

    public EventModel(String id, String name, String category, String date, String startTime, String endTime,
                      String description, String coworkingId, String coworkingName, String creatorId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.coworkingId = coworkingId;
        this.coworkingName = coworkingName;
        this.creatorId = creatorId;
    }

    public String getId() { return id; }

    public String getCoworkingId() { return coworkingId; }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public String getCoworkingName() { return coworkingName; }
}
