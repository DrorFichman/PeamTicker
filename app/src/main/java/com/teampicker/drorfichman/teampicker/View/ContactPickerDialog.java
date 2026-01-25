package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Dialog for selecting multiple contacts from the device's contact list.
 * Provides search functionality and keeps selected contacts at the top.
 */
public class ContactPickerDialog {

    public interface OnContactsSelectedListener {
        void onContactsSelected(ArrayList<String> selectedContacts);
    }

    private final Context context;
    private final OnContactsSelectedListener listener;

    public ContactPickerDialog(Context context, OnContactsSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Shows the contact picker dialog.
     * Queries contacts and displays a searchable multi-select list.
     */
    public void show() {
        ArrayList<String> contactNames = queryContacts();

        if (contactNames == null) {
            return; // Error already shown
        }

        if (contactNames.isEmpty()) {
            Toast.makeText(context, R.string.import_contacts_no_contacts, Toast.LENGTH_SHORT).show();
            return;
        }

        showSelectionDialog(contactNames);
    }

    private ArrayList<String> queryContacts() {
        ArrayList<String> contactNames = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1",
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC")) {
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        if (!TextUtils.isEmpty(name) && !contactNames.contains(name)) {
                            contactNames.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ContactPickerDialog", "Failed to query contacts", e);
            Toast.makeText(context, context.getString(R.string.toast_error_load_contacts_failed), Toast.LENGTH_SHORT).show();
            return null;
        }
        return contactNames;
    }

    private void showSelectionDialog(ArrayList<String> contactNames) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_contact_picker, null);
        EditText searchEdit = dialogView.findViewById(R.id.contact_search);
        ImageView clearButton = dialogView.findViewById(R.id.contact_search_clear);
        ListView listView = dialogView.findViewById(R.id.contact_list);
        TextView selectionCount = dialogView.findViewById(R.id.contact_selection_count);

        Set<String> selectedContacts = new HashSet<>();

        ContactPickerAdapter adapter = new ContactPickerAdapter(context, contactNames, selectedContacts);
        listView.setAdapter(adapter);

        Runnable updateCount = () -> {
            int count = selectedContacts.size();
            selectionCount.setText(context.getString(R.string.import_contacts_selected_count, count));
        };
        updateCount.run();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String name = adapter.getItem(position);
            if (name != null) {
                if (selectedContacts.contains(name)) {
                    selectedContacts.remove(name);
                } else {
                    selectedContacts.add(name);
                }
                // Just update checkbox state, don't reorder - reordering happens on filter change
                adapter.notifyDataSetChanged();
                updateCount.run();
            }
        });

        // Clear button click
        clearButton.setOnClickListener(v -> {
            searchEdit.setText("");
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
                // Show/hide clear button
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.import_contacts_select_title)
                .setView(dialogView)
                .setPositiveButton(R.string.import_contacts_done, (d, which) -> {
                    if (!selectedContacts.isEmpty() && listener != null) {
                        listener.onContactsSelected(new ArrayList<>(selectedContacts));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        // Handle back press
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                // If search has text, clear it first
                if (searchEdit.getText().length() > 0) {
                    searchEdit.setText("");
                    return true;
                }
                // If contacts are selected, confirm before closing
                if (!selectedContacts.isEmpty()) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.import_contacts_confirm_exit_title)
                            .setMessage(context.getString(R.string.import_contacts_confirm_exit_message, selectedContacts.size()))
                            .setPositiveButton(R.string.import_contacts_discard, (d, which) -> dialog.dismiss())
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    return true;
                }
            }
            return false;
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (screenHeight * 0.7));
            }
        });

        dialog.show();
    }

    /**
     * Adapter for the contact picker list with filtering and selection support.
     * Selected contacts are always shown at the top of the list.
     */
    private static class ContactPickerAdapter extends BaseAdapter implements Filterable {
        private final Context context;
        private final ArrayList<String> allContacts;
        private ArrayList<String> filteredContacts;
        private final Set<String> selectedContacts;
        private final Filter filter;
        private CharSequence currentFilter = "";

        ContactPickerAdapter(Context context, ArrayList<String> contacts, Set<String> selectedContacts) {
            this.context = context;
            this.allContacts = new ArrayList<>(contacts);
            this.filteredContacts = new ArrayList<>(contacts);
            this.selectedContacts = selectedContacts;
            sortWithSelectedFirst(filteredContacts);
            
            this.filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    ArrayList<String> filtered;
                    if (TextUtils.isEmpty(constraint)) {
                        filtered = new ArrayList<>(allContacts);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        filtered = new ArrayList<>();
                        for (String name : allContacts) {
                            if (name.toLowerCase().contains(filterPattern)) {
                                filtered.add(name);
                            }
                        }
                    }
                    sortWithSelectedFirst(filtered);
                    results.values = filtered;
                    results.count = filtered.size();
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    currentFilter = constraint;
                    filteredContacts = (ArrayList<String>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        private void sortWithSelectedFirst(ArrayList<String> list) {
            list.sort((a, b) -> {
                boolean aSelected = selectedContacts.contains(a);
                boolean bSelected = selectedContacts.contains(b);
                if (aSelected && !bSelected) return -1;
                if (!aSelected && bSelected) return 1;
                return a.compareToIgnoreCase(b);
            });
        }

        public void refreshOrder() {
            getFilter().filter(currentFilter);
        }

        @Override
        public int getCount() {
            return filteredContacts.size();
        }

        @Override
        public String getItem(int position) {
            return filteredContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CheckBox checkBox;
            if (convertView == null) {
                checkBox = new CheckBox(context);
                checkBox.setPadding(48, 32, 48, 32);
                checkBox.setTextSize(16);
                checkBox.setClickable(false);
                checkBox.setFocusable(false);
            } else {
                checkBox = (CheckBox) convertView;
            }

            String name = getItem(position);
            checkBox.setText(name);
            checkBox.setChecked(selectedContacts.contains(name));

            return checkBox;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }
    }
}

