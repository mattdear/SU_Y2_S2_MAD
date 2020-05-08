package com.md.placestostay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class AddNewActivity extends AppCompatActivity implements View.OnClickListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_new);
        AutoCompleteTextView textView = findViewById(R.id.autocomplete_type);
        String[] countries = getResources().getStringArray(R.array.types_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, countries);
        textView.setAdapter(adapter);
        Button addbtn = findViewById(R.id.addbtn);
        addbtn.setOnClickListener(this);
    }

    public void onClick(View v) {
        String name = "";
        String type = "";
        Double price = null;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        EditText inputName = findViewById(R.id.input_name);
        AutoCompleteTextView inputType = findViewById(R.id.autocomplete_type);
        EditText inputPrice = findViewById(R.id.input_price);
        if (inputName.getText().toString().trim().isEmpty()) {
            inputName.setError("Please enter a name");
            inputName.setHint("Raffles Hotel");
        } else {
            name = inputName.getText().toString().trim();
        }
        if (inputType.getText().toString().trim().isEmpty()) {
            inputType.setError("Please enter a type");
            inputType.setHint("Hotel");
        } else {
            type = inputType.getText().toString().trim();
        }
        if (inputPrice.getText().toString().trim().isEmpty()) {
            inputPrice.setError("Please enter a price");
            inputPrice.setHint("425.00");
        } else {
            price = Double.valueOf(inputPrice.getText().toString().trim());
        }
        if (name.equals("") || type.equals("") || price == null) {
            setResult(RESULT_OK, intent);
        } else {
            bundle.putString("name", name);
            bundle.putString("type", type);
            bundle.putDouble("price", price);
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}