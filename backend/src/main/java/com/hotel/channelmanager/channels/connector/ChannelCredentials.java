package com.hotel.channelmanager.channels.connector;

import java.util.HashMap;
import java.util.Map;

public class ChannelCredentials {
    private String apiKey;
    private String apiSecret;
    private String hotelId;
    private String username;
    private String password;
    private String icalUrl;
    private Map<String, String> additionalSettings = new HashMap<>();

    public ChannelCredentials() {}

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIcalUrl() {
        return icalUrl;
    }

    public void setIcalUrl(String icalUrl) {
        this.icalUrl = icalUrl;
    }

    public Map<String, String> getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(Map<String, String> additionalSettings) {
        this.additionalSettings = additionalSettings;
    }
}

