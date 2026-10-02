package com.eventbooking.model;

public class Event {

    private long id;
    private long organizerId;
    private String title;
    private String description;
    private String venue;
    private String startTime;
    private String endTime;
    private int totalSeats;
    private int availableSeats;
    private String status;

    public Event() {
    }

    public Event(
            long id,
            long organizerId,
            String title,
            String description,
            String venue,
            String startTime,
            String endTime,
            int totalSeats,
            int availableSeats,
            String status
    ) {
        this.id = id;
        this.organizerId = organizerId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public long getOrganizerId() {
        return organizerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVenue() {
        return venue;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public String getStatus() {
        return status;
    }
}