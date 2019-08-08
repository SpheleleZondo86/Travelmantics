package com.example.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 42;
    private DatabaseReference databaseReference;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;
    private TravelDeal deal;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        databaseReference = FirebaseUtil.databaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        imageView = findViewById(R.id.image);
        Intent intent = getIntent();
        TravelDeal travelDeal = (TravelDeal)intent.getSerializableExtra("Deal");
        if(travelDeal == null) travelDeal = new TravelDeal();
        this.deal = travelDeal;
        txtTitle.setText(travelDeal.getTitle());
        txtPrice.setText(travelDeal.getPrice());
        txtDescription.setText(travelDeal.getDescription());
        showImage(deal.getImageURL());
        Button button = findViewById(R.id.btnImage);
        button.setEnabled(FirebaseUtil.isAdmin);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), REQUEST_CODE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                clean();
                backToList();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void clean() {
        txtTitle.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        txtTitle.requestFocus();
    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if (deal.getId() == null){
            databaseReference.push().setValue(deal).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(getApplicationContext(), "New deal saved!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else {
            databaseReference.child(deal.getId()).setValue(deal).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(getApplicationContext(), "Deal edited!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void deleteDeal(){
        if (deal.getId() == null){
            Toast.makeText(getApplicationContext(), "No deal to delete!", Toast.LENGTH_SHORT).show();
            return;
        }else{
            databaseReference.child(deal.getId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(getApplicationContext(), "Deal deleted!", Toast.LENGTH_SHORT).show();
                        backToList();
                        if (deal.getImageName() != null && !deal.getImageName().isEmpty()){
                            StorageReference pictureReference = FirebaseUtil.firebaseStorage.getReference().child(deal.getImageName());
                            pictureReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("Delete Image", "Image successfully deleted");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("Delete Image", e.getMessage());
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);
        enableEditTexts(FirebaseUtil.isAdmin);
        menu.findItem(R.id.delete_menu).setVisible(FirebaseUtil.isAdmin);
        menu.findItem(R.id.save_menu).setVisible(FirebaseUtil.isAdmin);
        return true;
    }

    private void enableEditTexts(boolean isEnabled){
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtTitle.setEnabled(isEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            StorageReference storageReference = FirebaseUtil.storageReference.child(imageUri.getLastPathSegment());
            storageReference.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (taskSnapshot.getMetadata() != null) {
                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    deal.setImageURL(imageUrl);
                                    showImage(imageUrl);
                                }
                            });
                            String path = taskSnapshot.getStorage().getPath();
                            deal.setImageName(path);
                        }
                    }
                }
            });
        }
    }

    private void showImage(String url){
        if (url != null && !url.isEmpty()){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, width*2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }
}
