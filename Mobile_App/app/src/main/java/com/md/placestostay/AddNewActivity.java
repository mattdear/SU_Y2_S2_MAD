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

        //get a reference to the auto complete text view in the layout.
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.autocomplete_type);

        //get the string array
        String[] countries = getResources().getStringArray(R.array.types_array);

        //create the adapter and set it to the auto complete text view.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, countries);
        textView.setAdapter(adapter);

        Button addbtn = (Button) findViewById(R.id.addbtn);
        addbtn.setOnClickListener(this);
    }

    public void onClick(View v) {

        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        EditText inputName = (EditText) findViewById(R.id.input_name);
        String name = inputName.getText().toString();

        AutoCompleteTextView inputType = (AutoCompleteTextView) findViewById(R.id.autocomplete_type);
        String type = inputType.getText().toString();

        EditText inputPrice = (EditText) findViewById(R.id.input_price);
        Double price = Double.parseDouble(inputPrice.getText().toString());

        bundle.putString("name", name);
        bundle.putString("type", type);
        bundle.putDouble("price", price);
        intent.putExtras(bundle);

        if (intent.getExtras().getString("name") != null && intent.getExtras().getString("type") != null && intent.getExtras().getDouble("price") != 0.0) {
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
        finish();
    }
}