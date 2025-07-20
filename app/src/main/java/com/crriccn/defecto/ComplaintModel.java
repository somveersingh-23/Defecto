package com.crriccn.defecto;

import com.google.firebase.Timestamp;
import java.util.Date;

public class ComplaintModel {
    private String title;
    private String description;
    private String status;
    private Object timestamp;
    private String complaintType;
    private String imageUrl;
    private String location;
    private String otherComplaintType;
    private String userId;
    private String documentId;
    private String address;
    private String email;
    private String displayId;
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }


    public String getDisplayId() {
        return displayId;
    }



    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { this.complaintType = complaintType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOtherComplaintType() { return otherComplaintType; }
    public void setOtherComplaintType(String otherComplaintType) { this.otherComplaintType = otherComplaintType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ComplaintModel() {
        // Required for Firebase Firestore deserialization
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public Date getParsedTimestamp() {
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate();
        } else if (timestamp instanceof Long) {
            return new Date((Long) timestamp);
        } else if (timestamp instanceof Date) {
            return (Date) timestamp;
        }
        return null;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}
