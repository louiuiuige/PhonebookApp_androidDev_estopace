package com.example.phonebookapp;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.phonebookapp.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    binding.contactImage.setImageURI(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean writeGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_CONTACTS));
                boolean readGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_CONTACTS));
                if (writeGranted && readGranted) {
                    saveContact();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.contactImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        binding.button.setOnClickListener(v -> {
            if (validateInput()) {
                if (checkPermissions()) {
                    saveContact();
                } else {
                    requestPermissions();
                }
            }
        });
    }

    private boolean validateInput() {
        String first = binding.firstName.getText().toString().trim();
        String last = binding.lastName.getText().toString().trim();
        String mobile = binding.phoneMobile.getText().toString().trim();
        String home = binding.phoneHome.getText().toString().trim();

        if (first.isEmpty() && last.isEmpty()) {
            Toast.makeText(this, "Please enter at least one name (First or Last)", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mobile.isEmpty() && home.isEmpty()) {
            Toast.makeText(this, "Please enter at least one phone number (Mobile or Home)", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        permissionLauncher.launch(new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS});
    }

    private void saveContact() {
        String first = binding.firstName.getText().toString().trim();
        String last = binding.lastName.getText().toString().trim();
        String mobile = binding.phoneMobile.getText().toString().trim();
        String home = binding.phoneHome.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String address = binding.address.getText().toString().trim();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // Name
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, first)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, last)
                .build());

        // Mobile Phone
        if (!mobile.isEmpty()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobile)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
        }

        // Home Phone
        if (!home.isEmpty()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, home)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                    .build());
        }

        // Email
        if (!email.isEmpty()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());
        }

        // Address
        if (!address.isEmpty()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                    .build());
        }

        // Image
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                            .build());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving contact image", e);
            }
        }

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Toast.makeText(this, "Saved...", Toast.LENGTH_SHORT).show();
            clearFields();
        } catch (Exception e) {
            Log.e(TAG, "Error saving contact", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        binding.firstName.setText("");
        binding.lastName.setText("");
        binding.phoneMobile.setText("");
        binding.phoneHome.setText("");
        binding.email.setText("");
        binding.address.setText("");
        binding.contactImage.setImageResource(android.R.drawable.ic_menu_gallery);
        imageUri = null;
    }
}