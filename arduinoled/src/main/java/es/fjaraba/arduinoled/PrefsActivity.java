package es.fjaraba.arduinoled;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by fernando.jaraba on 12/02/2016.
 */
public class PrefsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        setResult(RESULT_OK);
    }
}
