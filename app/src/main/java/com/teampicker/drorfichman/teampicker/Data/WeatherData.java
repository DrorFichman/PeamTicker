package com.teampicker.drorfichman.teampicker.Data;

/**
 * Weather data model for displaying game-time weather information
 */
public class WeatherData {
    private float minTemperature;
    private float maxTemperature;
    private int weatherCode;
    private String errorMessage;

    public WeatherData(float minTemperature, float maxTemperature, int weatherCode) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.weatherCode = weatherCode;
    }

    public WeatherData(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public float getMinTemperature() {
        return minTemperature;
    }

    public float getMaxTemperature() {
        return maxTemperature;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get weather emoji based on WMO Weather interpretation codes
     * https://open-meteo.com/en/docs
     */
    public String getWeatherEmoji() {
        if (weatherCode == 0) return "☀️"; // Clear sky
        if (weatherCode <= 3) return "⛅"; // Partly cloudy
        if (weatherCode <= 49) return "🌫️"; // Fog
        if (weatherCode <= 59) return "🌧️"; // Drizzle
        if (weatherCode <= 69) return "🌧️"; // Rain
        if (weatherCode <= 79) return "🌨️"; // Snow
        if (weatherCode <= 84) return "🌧️"; // Rain showers
        if (weatherCode <= 99) return "⛈️"; // Thunderstorm
        return "🌤️"; // Default
    }

    /**
     * Get weather description based on WMO Weather interpretation codes
     */
    public String getWeatherDescription() {
        if (weatherCode == 0) return "Clear";
        if (weatherCode <= 3) return "Partly Cloudy";
        if (weatherCode <= 49) return "Foggy";
        if (weatherCode <= 59) return "Drizzle";
        if (weatherCode <= 69) return "Rain";
        if (weatherCode <= 79) return "Snow";
        if (weatherCode <= 84) return "Showers";
        if (weatherCode <= 99) return "Thunderstorm";
        return "Unknown";
    }
}

