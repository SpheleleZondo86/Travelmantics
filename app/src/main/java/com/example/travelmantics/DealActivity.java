package com.example.travelmantics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

public class DealActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;
    private TravelDeal deal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);
        databaseReference = FirebaseUtil.databaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        Intent intent = getIntent();
        TravelDeal travelDeal = (TravelDeal)intent.getSerializableExtra("Deal");
        if(travelDeal == null) travelDeal = new TravelDeal();
        this.deal = travelDeal;
        txtTitle.setText(travelDeal.getTitle());
        txtPrice.setText(travelDeal.getPrice());
        txtDescription.setText(travelDeal.getDescription());
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
}
