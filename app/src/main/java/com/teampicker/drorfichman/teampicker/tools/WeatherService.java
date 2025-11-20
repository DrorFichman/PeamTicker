package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.WeatherData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Service for fetching weather data from Open-Meteo API
 */
public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.open-meteo.com/";
    
    // Default Tel Aviv coordinates
    private static final double DEFAULT_LATITUDE = 32.0853;
    private static final double DEFAULT_LONGITUDE = 34.7818;
    private static final String DEFAULT_LOCATION_NAME = "Ginegar, Israel";
    
    // Default game time window (14:00-17:00)
    private static final int DEFAULT_START_HOUR = 14;
    private static final int DEFAULT_START_MINUTE = 0;
    private static final int DEFAULT_END_HOUR = 17;
    private static final int DEFAULT_END_MINUTE = 0;
    private static final int DEFAULT_DAY_OF_WEEK = -1; // -1 means use current day
    
    private final OpenMeteoApi api;
    private final Context context;

    public interface WeatherCallback {
        void onWeatherReceived(WeatherData weatherData);
        void onWeatherError(String error);
    }

    /**
     * Open-Meteo API interface
     */
    interface OpenMeteoApi {
        @GET("v1/forecast")
        Call<WeatherResponse> getWeatherForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("hourly") String hourly,
            @Query("timezone") String timezone,
            @Query("forecast_days") int forecastDays
        );
    }

    /**
     * Weather response from Open-Meteo API
     */
    static class WeatherResponse {
        @SerializedName("hourly")
        HourlyData hourly;
        
        static class HourlyData {
            @SerializedName("time")
            List<String> time;
            
            @SerializedName("temperature_2m")
            List<Float> temperature;
            
            @SerializedName("weather_code")
            List<Integer> weatherCode;
        }
    }

    public WeatherService(Context context) {
        this.context = context;
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        
        api = retrofit.create(OpenMeteoApi.class);
    }

    /**
     * Get weather settings from preferences
     */
    public static int getWeatherStartHour(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getInt(PreferenceHelper.pref_weather_start_hour, DEFAULT_START_HOUR);
    }
    
    public static int getWeatherStartMinute(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getInt(PreferenceHelper.pref_weather_start_minute, DEFAULT_START_MINUTE);
    }

    public static int getWeatherEndHour(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getInt(PreferenceHelper.pref_weather_end_hour, DEFAULT_END_HOUR);
    }
    
    public static int getWeatherEndMinute(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getInt(PreferenceHelper.pref_weather_end_minute, DEFAULT_END_MINUTE);
    }
    
    /**
     * Get the configured day of week for weather (Calendar.SUNDAY to Calendar.SATURDAY)
     * Returns -1 if not set (use current day)
     */
    public static int getWeatherDayOfWeek(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getInt(PreferenceHelper.pref_weather_day_of_week, DEFAULT_DAY_OF_WEEK);
    }

    public static void setWeatherSettings(Context context, int startHour, int startMinute, int endHour, int endMinute, int dayOfWeek) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PreferenceHelper.pref_weather_start_hour, startHour);
        editor.putInt(PreferenceHelper.pref_weather_start_minute, startMinute);
        editor.putInt(PreferenceHelper.pref_weather_end_hour, endHour);
        editor.putInt(PreferenceHelper.pref_weather_end_minute, endMinute);
        editor.putInt(PreferenceHelper.pref_weather_day_of_week, dayOfWeek);
        editor.apply();
    }
    
    /**
     * Get weather location settings
     */
    public static String getWeatherLocationName(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return prefs.getString(PreferenceHelper.pref_weather_location_name, DEFAULT_LOCATION_NAME);
    }
    
    public static double getWeatherLocationLatitude(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return Double.longBitsToDouble(prefs.getLong(PreferenceHelper.pref_weather_location_lat, 
            Double.doubleToLongBits(DEFAULT_LATITUDE)));
    }
    
    public static double getWeatherLocationLongitude(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        return Double.longBitsToDouble(prefs.getLong(PreferenceHelper.pref_weather_location_lng, 
            Double.doubleToLongBits(DEFAULT_LONGITUDE)));
    }
    
    public static void setWeatherLocation(Context context, String locationName, double latitude, double longitude) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceHelper.pref_weather_location_name, locationName);
        editor.putLong(PreferenceHelper.pref_weather_location_lat, Double.doubleToRawLongBits(latitude));
        editor.putLong(PreferenceHelper.pref_weather_location_lng, Double.doubleToRawLongBits(longitude));
        editor.apply();
    }
    
    /**
     * Get the next occurrence of the configured day of week
     * If no day is configured, returns today
     */
    public static Calendar getWeatherDate(Context context) {
        Calendar date = Calendar.getInstance();
        int configuredDay = getWeatherDayOfWeek(context);
        
        // If no day configured, use today
        if (configuredDay == -1) {
            return date;
        }
        
        // Calculate next occurrence of the configured day
        int currentDay = date.get(Calendar.DAY_OF_WEEK);
        int daysUntilTarget;
        
        if (configuredDay >= currentDay) {
            daysUntilTarget = configuredDay - currentDay;
        } else {
            daysUntilTarget = 7 - currentDay + configuredDay;
        }
        
        date.add(Calendar.DAY_OF_YEAR, daysUntilTarget);
        return date;
    }
    
    /**
     * Format date for display (e.g., "Thursday November 20th")
     */
    public static String formatWeatherDate(Calendar date) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE MMMM d", Locale.US);
        String dateStr = dayFormat.format(date.getTime());
        
        // Add ordinal suffix (st, nd, rd, th)
        int day = date.get(Calendar.DAY_OF_MONTH);
        String suffix;
        if (day >= 11 && day <= 13) {
            suffix = "th";
        } else {
            switch (day % 10) {
                case 1: suffix = "st"; break;
                case 2: suffix = "nd"; break;
                case 3: suffix = "rd"; break;
                default: suffix = "th"; break;
            }
        }
        
        return dateStr + suffix;
    }
    
    /**
     * Get day of week name (e.g., "Monday", "Tuesday")
     */
    public static String getDayOfWeekName(int dayOfWeek) {
        SimpleDateFormat format = new SimpleDateFormat("EEEE", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        return format.format(cal.getTime());
    }

    /**
     * Fetch weather for the configured time window on the selected date
     */
    public void fetchWeatherForGameTime(WeatherCallback callback) {
        // Check if weather feature is enabled
        if (!Configurations.isWeatherFeatureEnabled()) {
            callback.onWeatherError("Weather feature is disabled");
            return;
        }
        
        Calendar gameDate = getWeatherDate(context);
        int startHour = getWeatherStartHour(context);
        int startMinute = getWeatherStartMinute(context);
        int endHour = getWeatherEndHour(context);
        int endMinute = getWeatherEndMinute(context);
        
        // Get location from preferences
        double latitude = getWeatherLocationLatitude(context);
        double longitude = getWeatherLocationLongitude(context);
        
        // Request hourly data for temperature and weather code
        Call<WeatherResponse> call = api.getWeatherForecast(
            latitude,
            longitude,
            "temperature_2m,weather_code",
            "auto", // Auto-detect timezone based on location
            3 // Get 3 days of forecast to ensure we have the game day
        );
        
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        WeatherData weatherData = parseWeatherResponse(
                            response.body(), 
                            gameDate, 
                            startHour,
                            startMinute,
                            endHour,
                            endMinute
                        );
                        callback.onWeatherReceived(weatherData);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing weather data", e);
                        callback.onWeatherError("Failed to parse weather data");
                    }
                } else {
                    callback.onWeatherError("Weather service unavailable");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Weather API call failed", t);
                callback.onWeatherError("Network error");
            }
        });
    }

    /**
     * Parse the weather response and extract min/max temperature for the specified time window
     */
    private WeatherData parseWeatherResponse(
        WeatherResponse response, 
        Calendar gameDate, 
        int startHour,
        int startMinute,
        int endHour,
        int endMinute
    ) {
        if (response.hourly == null || 
            response.hourly.time == null || 
            response.hourly.temperature == null ||
            response.hourly.weatherCode == null) {
            return new WeatherData("Invalid weather data");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        String gameDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(gameDate.getTime());
        
        float minTemp = Float.MAX_VALUE;
        float maxTemp = Float.MIN_VALUE;
        int mostCommonWeatherCode = 0;
        int validDataPoints = 0;
        
        // Calculate start and end time in minutes from midnight for comparison
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;
        
        // Find temperatures within the specified time window on the game date
        for (int i = 0; i < response.hourly.time.size(); i++) {
            try {
                String timeStr = response.hourly.time.get(i);
                
                // Check if this is the game date and within time window
                if (timeStr.startsWith(gameDateStr)) {
                    Calendar hourTime = Calendar.getInstance();
                    hourTime.setTime(sdf.parse(timeStr));
                    int hour = hourTime.get(Calendar.HOUR_OF_DAY);
                    int minute = hourTime.get(Calendar.MINUTE);
                    int currentTimeMinutes = hour * 60 + minute;
                    
                    if (currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes) {
                        float temp = response.hourly.temperature.get(i);
                        minTemp = Math.min(minTemp, temp);
                        maxTemp = Math.max(maxTemp, temp);
                        
                        if (validDataPoints == 0) {
                            mostCommonWeatherCode = response.hourly.weatherCode.get(i);
                        }
                        validDataPoints++;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing time entry: " + response.hourly.time.get(i), e);
            }
        }
        
        if (validDataPoints == 0) {
            return new WeatherData("No weather data for selected time");
        }
        
        return new WeatherData(minTemp, maxTemp, mostCommonWeatherCode);
    }
}

