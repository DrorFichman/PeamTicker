package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.View.PlayerDetailsActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Handles contacts shared to the app from external sources.
 * Parses vCard data and creates players.
 */
public class SharedContactHandler {

    private static final String TAG = "SharedContactHandler";

    /**
     * Check if the intent contains a shared contact and handle it.
     * @return true if a shared contact was handled
     */
    public static boolean handleSharedContact(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null) {
            return false;
        }

        if (!type.equals("text/vcard") && !type.equals("text/x-vcard")) {
            return false;
        }

        Uri contactUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (contactUri == null) {
            // Try getting from data
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                String displayName = parseVCardName(text.toString());
                if (displayName != null) {
                    navigateToNewPlayer(context, displayName);
                    return true;
                }
            }
            return false;
        }

        String vCardData = readVCardFromUri(context, contactUri);
        if (vCardData == null) {
            Toast.makeText(context, R.string.shared_contact_read_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        String displayName = parseVCardName(vCardData);
        if (displayName == null) {
            Toast.makeText(context, R.string.shared_contact_parse_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        navigateToNewPlayer(context, displayName);
        return true;
    }

    private static String readVCardFromUri(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading vCard from URI", e);
            return null;
        }
    }

    private static String parseVCardName(String vCardData) {
        // First, unfold wrapped lines (vCard wraps long lines, continuation starts with space/tab)
        String unfoldedData = unfoldVCardLines(vCardData);
        
        // Parse FN (Formatted Name) field from vCard
        // Example: FN:John Doe
        // Example: FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=D7=90=D7=91
        String[] lines = unfoldedData.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("FN:") || upper.startsWith("FN;")) {
                String name = extractVCardValue(trimmed);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
            }
        }

        // Fallback to N (Name) field
        // Example: N:Doe;John;;;
        for (String line : lines) {
            String trimmed = line.trim();
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("N:") || upper.startsWith("N;")) {
                String nameField = extractVCardValue(trimmed);
                if (!TextUtils.isEmpty(nameField)) {
                    String[] parts = nameField.split(";");
                    if (parts.length >= 2) {
                        String firstName = parts.length > 1 ? parts[1].trim() : "";
                        String lastName = parts[0].trim();
                        String fullName = (firstName + " " + lastName).trim();
                        if (!TextUtils.isEmpty(fullName)) {
                            return fullName;
                        }
                    } else if (parts.length == 1 && !TextUtils.isEmpty(parts[0])) {
                        return parts[0].trim();
                    }
                }
            }
        }

        return null;
    }

    private static String extractVCardValue(String line) {
        // Find the colon that separates field name/params from value
        int colonIndex = line.indexOf(':');
        if (colonIndex < 0 || colonIndex >= line.length() - 1) {
            return null;
        }

        String params = line.substring(0, colonIndex).toUpperCase();
        String value = line.substring(colonIndex + 1).trim();

        // Check if QUOTED-PRINTABLE encoding
        if (params.contains("ENCODING=QUOTED-PRINTABLE") || params.contains("QUOTED-PRINTABLE")) {
            // Extract charset if specified (e.g., CHARSET=UTF-8)
            String charset = "UTF-8"; // Default
            for (String param : params.split(";")) {
                if (param.startsWith("CHARSET=")) {
                    charset = param.substring(8).trim();
                    break;
                }
            }
            value = decodeQuotedPrintable(value, charset);
        }

        return value;
    }

    /**
     * Unfold wrapped vCard lines. In vCard format, long lines are wrapped 
     * and continuation lines start with a space or tab character.
     * Also handles QUOTED-PRINTABLE soft line breaks (= at end of line).
     */
    private static String unfoldVCardLines(String vCardData) {
        StringBuilder result = new StringBuilder();
        String[] lines = vCardData.split("\r?\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Remove trailing carriage return if present
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            
            // Check if this line starts with space/tab (continuation of previous line)
            if (line.length() > 0 && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                // Append without the leading whitespace (it's just a fold marker)
                result.append(line.substring(1));
            } else {
                // New logical line
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
            
            // Handle QUOTED-PRINTABLE soft line break (= at end of line)
            // The = at end means the line continues on the next line
            if (result.length() > 0 && result.charAt(result.length() - 1) == '=' 
                    && i + 1 < lines.length) {
                // Check if next line looks like a continuation (not a new field)
                String nextLine = lines[i + 1];
                if (nextLine.length() > 0 && !nextLine.contains(":") 
                        && nextLine.charAt(0) != ' ' && nextLine.charAt(0) != '\t') {
                    // Remove the trailing = and append next line directly
                    result.setLength(result.length() - 1);
                    result.append(nextLine);
                    i++; // Skip the next line since we consumed it
                }
            }
        }
        
        return result.toString();
    }

    private static String decodeQuotedPrintable(String encoded, String charset) {
        try {
            StringBuilder result = new StringBuilder();
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            
            for (int i = 0; i < encoded.length(); i++) {
                char c = encoded.charAt(i);
                if (c == '=' && i + 2 < encoded.length()) {
                    // Check if it's a hex-encoded byte
                    String hex = encoded.substring(i + 1, i + 3);
                    try {
                        int byteValue = Integer.parseInt(hex, 16);
                        bytes.write(byteValue);
                        i += 2; // Skip the two hex chars
                    } catch (NumberFormatException e) {
                        // Soft line break (=\n) or invalid, flush bytes and add char
                        if (bytes.size() > 0) {
                            result.append(bytes.toString(charset));
                            bytes.reset();
                        }
                        result.append(c);
                    }
                } else {
                    // Flush any pending bytes
                    if (bytes.size() > 0) {
                        result.append(bytes.toString(charset));
                        bytes.reset();
                    }
                    result.append(c);
                }
            }
            
            // Flush remaining bytes
            if (bytes.size() > 0) {
                result.append(bytes.toString(charset));
            }
            
            return result.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error decoding quoted-printable with charset " + charset, e);
            return encoded; // Return original if decoding fails
        }
    }

    private static void navigateToNewPlayer(Context context, String displayName) {
        // Check if player already exists
        Player existingPlayer = DbHelper.getPlayer(context, displayName);
        if (existingPlayer != null) {
            String message = existingPlayer.archived 
                    ? context.getString(R.string.shared_contact_exists_archived, displayName)
                    : context.getString(R.string.shared_contact_exists, displayName);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }

        // Navigate to PlayerDetailsActivity with the contact name pre-filled
        Intent intent = PlayerDetailsActivity.getNewPlayerFromIdentifierIntent(context, displayName);
        if (context instanceof Activity) {
            context.startActivity(intent);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        
        Log.i(TAG, "Navigating to new player screen with: " + displayName);
    }
}

