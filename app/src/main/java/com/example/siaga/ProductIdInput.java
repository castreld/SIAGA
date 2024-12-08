package com.example.siaga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class ProductIdInput extends AppCompatActivity {

    Button submitButton;
    EditText productIdInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_id_input);

        submitButton = findViewById(R.id.submitButton);
        productIdInput = findViewById(R.id.productIdInput);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String productId = productIdInput.getText().toString().trim();
                if (!productId.isEmpty()) {
                    Intent intent = new Intent(ProductIdInput.this, MainActivity.class);
                    intent.putExtra("produkId", productId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ProductIdInput.this, "Please enter a valid Product ID.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}