package com.example.diplom;

import java.util.List;
import java.util.Map;

public class CoworkingModel {
    private String documentId; // Идентификатор документа в Firestore
    private String name, address, places, description, price;
    private List<String> images; // Ссылки на изображения коворкинга
    private List<String> imagesPublicIds; // Список public_id изображений в Cloudinary

    // Новые поля для данных создателя коворкинга
    private String creatorId;
    private String creatorName;
    private String creatorSurname;
    private String creatorAvatarUrl; // URL аватарки пользователя

    // Новое поле для зон (каждая зона представлена как Map с ключами, например, "name" и "places")
    private List<Map<String, Object>> zones;

    public CoworkingModel() {} // Пустой конструктор для Firestore

    public CoworkingModel(String documentId, String name, String address, String places, String description, String price,
                          List<String> imageUrls, List<String> imagesPublicIds,
                          String creatorId, String creatorName, String creatorSurname, String creatorAvatarUrl,
                          List<Map<String, Object>> zones) {
        this.documentId = documentId;
        this.name = name;
        this.address = address;
        this.places = places;
        this.description = description;
        this.price = price;
        this.images = imageUrls;
        this.imagesPublicIds = imagesPublicIds;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.creatorSurname = creatorSurname;
        this.creatorAvatarUrl = creatorAvatarUrl;
        this.zones = zones;
    }

    // Геттеры
    public String getDocumentId() { return documentId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPlaces() { return places; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
    public List<String> getImages() { return images; }
    public List<String> getImagesPublicIds() { return imagesPublicIds; }
    public String getCreatorId() { return creatorId; }
    public String getCreatorName() { return creatorName; }
    public String getCreatorSurname() { return creatorSurname; }
    public String getCreatorAvatarUrl() { return creatorAvatarUrl; }
    public List<Map<String, Object>> getZones() { return zones; }

    // Сеттеры
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPlaces(String places) { this.places = places; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(String price) { this.price = price; }
    public void setImages(List<String> images) { this.images = images; }
    public void setImagesPublicIds(List<String> imagesPublicIds) { this.imagesPublicIds = imagesPublicIds; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public void setCreatorSurname(String creatorSurname) { this.creatorSurname = creatorSurname; }
    public void setCreatorAvatarUrl(String creatorAvatarUrl) { this.creatorAvatarUrl = creatorAvatarUrl; }
    public void setZones(List<Map<String, Object>> zones) { this.zones = zones; }
}
