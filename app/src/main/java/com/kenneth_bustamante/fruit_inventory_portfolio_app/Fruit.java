package com.kenneth_bustamante.fruit_inventory_portfolio_app;

public class Fruit {
    // Define variables
    private String documentId;
    private String name;
    private int amount;
    private String image_url;

    // Constructor
    public Fruit(String documentId, String name, int amount, String image_url) {
        this.documentId = documentId;
        this.name = name;
        this.amount = amount;
        this.image_url = image_url;
    }

    // Getters and setters

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getImageUrl() {
        return image_url;
    }

    public void setImageUrl(String image) {
        this.image_url = image;
    }
}

// Copyright © 2023 Kenneth Bustamante Zuluaga