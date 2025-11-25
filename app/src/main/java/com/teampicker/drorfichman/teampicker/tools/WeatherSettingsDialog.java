package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Data.Configurations;

import java.util.Calendar;
import java.util.List;

/**
 * Helper class to display and manage the weather settings dialog
 */
public class WeatherSettingsDialog {
    private static final String TAG = "WeatherSettingsDialog";
    
    /**
     * Callback interface for when settings are updated
     */
    public interface OnSettingsUpdatedListener {
        void onSettingsUpdated();
    }
    
    /**
     * Show the weather settings dialog
     */
    public static void show(Activity activity, OnSettingsUpdatedListener listener) {
        // Check if feature is enabled
        if (!Configurations.isWeatherFeatureEnabled()) {
            Toast.makeText(activity, "Weather feature is currently disabled", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Weather Settings");
        
        LinearLayout layout = createDialogLayout(activity);
        
        // Get UI components
        EditText locationInput = (EditText) layout.findViewWithTag("location_input");
        NumberPicker dayPicker = (NumberPicker) layout.findViewWithTag("day_picker");
        NumberPicker startHourPicker = (NumberPicker) layout.findViewWithTag("start_hour");
        NumberPicker startMinutePicker = (NumberPicker) layout.findViewWithTag("start_minute");
        NumberPicker endHourPicker = (NumberPicker) layout.findViewWithTag("end_hour");
        NumberPicker endMinutePicker = (NumberPicker) layout.findViewWithTag("end_minute");
        
        builder.setView(layout);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            int startHour = startHourPicker.getValue();
            int startMinute = startMinutePicker.getValue();
            int endHour = endHourPicker.getValue();
            int endMinute = endMinutePicker.getValue();
            int dayOfWeek = dayPicker.getValue();
            String locationName = locationInput.getText().toString().trim();
            
            // Compare times as minutes from midnight
            int startTimeMinutes = startHour * 60 + startMinute;
            int endTimeMinutes = endHour * 60 + endMinute;
            
            if (startTimeMinutes >= endTimeMinutes) {
                Toast.makeText(activity, "Start time must be before end time", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // If location changed, geocode it
            if (!locationName.isEmpty() && !locationName.equals(WeatherService.getWeatherLocationName(activity))) {
                geocodeLocation(activity, locationName, () -> {
                    WeatherService.setWeatherSettings(activity, startHour, startMinute, endHour, endMinute, dayOfWeek);
                    Toast.makeText(activity, "Weather settings updated", Toast.LENGTH_SHORT).show();
                    listener.onSettingsUpdated();
                });
            } else {
                WeatherService.setWeatherSettings(activity, startHour, startMinute, endHour, endMinute, dayOfWeek);
                Toast.makeText(activity, "Weather settings updated", Toast.LENGTH_SHORT).show();
                listener.onSettingsUpdated();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Create the dialog layout with all UI components
     */
    private static LinearLayout createDialogLayout(Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        // Location section
        addLocationSection(activity, layout);
        
        // Day of week section
        addDayOfWeekSection(activity, layout);
        
        // Start time section
        addStartTimeSection(activity, layout);
        
        // End time section
        addEndTimeSection(activity, layout);
        
        return layout;
    }
    
    /**
     * Add location input section
     */
    private static void addLocationSection(Activity activity, LinearLayout layout) {
        TextView locationLabel = new TextView(activity);
        locationLabel.setText("Location:");
        locationLabel.setPadding(0, 0, 0, 10);
        locationLabel.setTextSize(16);
        layout.addView(locationLabel);
        
        EditText locationInput = new EditText(activity);
        String currentLocation = WeatherService.getWeatherLocationName(activity);
        locationInput.setText(currentLocation);
        locationInput.setHint("Enter city or address");
        locationInput.setSingleLine(true);
        locationInput.setTag("location_input");
        layout.addView(locationInput);
    }
    
    /**
     * Add day of week selection section
     */
    private static void addDayOfWeekSection(Activity activity, LinearLayout layout) {
        TextView dayLabel = new TextView(activity);
        dayLabel.setText("Game Day:");
        dayLabel.setPadding(0, 30, 0, 10);
        dayLabel.setTextSize(16);
        layout.addView(dayLabel);
        
        NumberPicker dayPicker = new NumberPicker(activity);
        dayPicker.setMinValue(Calendar.SUNDAY);
        dayPicker.setMaxValue(Calendar.SATURDAY);
        dayPicker.setWrapSelectorWheel(true);
        
        String[] dayLabels = new String[7];
        dayLabels[0] = "Sunday";
        dayLabels[1] = "Monday";
        dayLabels[2] = "Tuesday";
        dayLabels[3] = "Wednesday";
        dayLabels[4] = "Thursday";
        dayLabels[5] = "Friday";
        dayLabels[6] = "Saturday";
        dayPicker.setDisplayedValues(dayLabels);
        
        int configuredDay = WeatherService.getWeatherDayOfWeek(activity);
        if (configuredDay == -1) {
            configuredDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        }
        dayPicker.setValue(configuredDay);
        dayPicker.setTag("day_picker");
        
        layout.addView(dayPicker);
    }
    
    /**
     * Add start time selection section
     */
    private static void addStartTimeSection(Activity activity, LinearLayout layout) {
        TextView startLabel = new TextView(activity);
        startLabel.setText("Start Time:");
        startLabel.setPadding(0, 30, 0, 10);
        startLabel.setTextSize(16);
        layout.addView(startLabel);
        
        LinearLayout startTimeLayout = new LinearLayout(activity);
        startTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        startTimeLayout.setGravity(Gravity.CENTER);
        startTimeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        
        NumberPicker startHourPicker = new NumberPicker(activity);
        startHourPicker.setMinValue(0);
        startHourPicker.setMaxValue(23);
        startHourPicker.setValue(WeatherService.getWeatherStartHour(activity));
        startHourPicker.setWrapSelectorWheel(true);
        startHourPicker.setTag("start_hour");
        startTimeLayout.addView(startHourPicker);
        
        TextView startColon = new TextView(activity);
        startColon.setText(":");
        startColon.setTextSize(24);
        startColon.setPadding(10, 0, 10, 0);
        startTimeLayout.addView(startColon);
        
        NumberPicker startMinutePicker = new NumberPicker(activity);
        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(59);
        startMinutePicker.setValue(WeatherService.getWeatherStartMinute(activity));
        startMinutePicker.setWrapSelectorWheel(true);
        startMinutePicker.setFormatter(value -> String.format("%02d", value));
        startMinutePicker.setTag("start_minute");
        startTimeLayout.addView(startMinutePicker);
        
        layout.addView(startTimeLayout);
    }
    
    /**
     * Add end time selection section
     */
    private static void addEndTimeSection(Activity activity, LinearLayout layout) {
        TextView endLabel = new TextView(activity);
        endLabel.setText("End Time:");
        endLabel.setPadding(0, 30, 0, 10);
        endLabel.setTextSize(16);
        layout.addView(endLabel);
        
        LinearLayout endTimeLayout = new LinearLayout(activity);
        endTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        endTimeLayout.setGravity(Gravity.CENTER);
        endTimeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        
        NumberPicker endHourPicker = new NumberPicker(activity);
        endHourPicker.setMinValue(0);
        endHourPicker.setMaxValue(23);
        endHourPicker.setValue(WeatherService.getWeatherEndHour(activity));
        endHourPicker.setWrapSelectorWheel(true);
        endHourPicker.setTag("end_hour");
        endTimeLayout.addView(endHourPicker);
        
        TextView endColon = new TextView(activity);
        endColon.setText(":");
        endColon.setTextSize(24);
        endColon.setPadding(10, 0, 10, 0);
        endTimeLayout.addView(endColon);
        
        NumberPicker endMinutePicker = new NumberPicker(activity);
        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(59);
        endMinutePicker.setValue(WeatherService.getWeatherEndMinute(activity));
        endMinutePicker.setWrapSelectorWheel(true);
        endMinutePicker.setFormatter(value -> String.format("%02d", value));
        endMinutePicker.setTag("end_minute");
        endTimeLayout.addView(endMinutePicker);
        
        layout.addView(endTimeLayout);
    }
    
    /**
     * Geocode a location name to coordinates using Android's Geocoder
     */
    private static void geocodeLocation(Activity activity, String locationName, Runnable onSuccess) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(activity, "Geocoder not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(activity);
                List<Address> addresses = geocoder.getFromLocationName(locationName, 5);
                
                if (addresses != null && !addresses.isEmpty()) {
                    if (addresses.size() == 1) {
                        // Single result - use it directly
                        Address address = addresses.get(0);
                        saveLocationFromAddress(activity, address);
                        activity.runOnUiThread(onSuccess);
                    } else {
                        // Multiple results - show selection dialog
                        activity.runOnUiThread(() -> showLocationSelectionDialog(activity, addresses, onSuccess));
                    }
                } else {
                    activity.runOnUiThread(() -> 
                        Toast.makeText(activity, "Location not found: " + locationName, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Geocoding error", e);
                activity.runOnUiThread(() -> 
                    Toast.makeText(activity, "Error finding location", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
    
    /**
     * Show dialog to select from multiple geocoding results
     */
    private static void showLocationSelectionDialog(Activity activity, List<Address> addresses, Runnable onSuccess) {
        String[] locationNames = new String[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            Address addr = addresses.get(i);
            StringBuilder name = new StringBuilder();
            
            if (addr.getLocality() != null) {
                name.append(addr.getLocality());
            }
            if (addr.getAdminArea() != null) {
                if (name.length() > 0) name.append(", ");
                name.append(addr.getAdminArea());
            }
            if (addr.getCountryName() != null) {
                if (name.length() > 0) name.append(", ");
                name.append(addr.getCountryName());
            }
            
            locationNames[i] = name.length() > 0 ? name.toString() : addr.getAddressLine(0);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Location");
        builder.setItems(locationNames, (dialog, which) -> {
            saveLocationFromAddress(activity, addresses.get(which));
            onSuccess.run();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Save location from geocoded address
     */
    private static void saveLocationFromAddress(Activity activity, Address address) {
        StringBuilder locationName = new StringBuilder();
        
        if (address.getLocality() != null) {
            locationName.append(address.getLocality());
        }
        if (address.getAdminArea() != null) {
            if (locationName.length() > 0) locationName.append(", ");
            locationName.append(address.getAdminArea());
        }
        if (address.getCountryName() != null) {
            if (locationName.length() > 0) locationName.append(", ");
            locationName.append(address.getCountryName());
        }
        
        String finalName = locationName.length() > 0 ? locationName.toString() : address.getAddressLine(0);
        
        WeatherService.setWeatherLocation(
            activity,
            finalName,
            address.getLatitude(),
            address.getLongitude()
        );
        
        Log.d(TAG, String.format("Location saved: %s (%.4f, %.4f)", 
            finalName, address.getLatitude(), address.getLongitude()));
    }
}

