package es.fjaraba.arduinoled;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class AboutActivity extends Activity implements View.OnClickListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
    }

    @Override
    public void onClick(View view) {
        finish();
    }
}