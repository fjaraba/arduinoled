package es.fjaraba.arduinoled;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/*
    Fernando Jaraba Nieto. 2016

 */

public class MainActivity extends Activity implements View.OnClickListener {

    public final static String PREF_IP = "PREF_IP_ADDRESS";
    public final static String PREF_PORT = "PREF_PORT_NUMBER";
    public final static String PREF_LEDS = "PREF_LEDS";

    public final static int MAX_LED = 20;
    public final static int BUTTON_INDEX = 2000;


    public final static String LED_INICIAL = "11";
    public final static String TOTAL_LEDS = "5";

    // declare buttons and text inputs
    private Button buttonPin11,buttonPin12,buttonPin13;
    private EditText editTextIPAddress, editTextPortNumber, editTextLeds;
    // shared preferences objects used to save the IP address and port so that the user doesn't have to
    // type them next time he/she opens the app.
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;

    //Array donde se guardan las imagenes
    private ImageView m_aImagenes[];

    //Total de leds
    private int m_nLeds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("HTTP_HELPER_PREFS",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // assign text inputs
        editTextIPAddress = (EditText)findViewById(R.id.editTextIPAddress);
        editTextPortNumber = (EditText)findViewById(R.id.editTextPortNumber);
        editTextLeds = (EditText)findViewById(R.id.editTextLeds);

        // get the IP address and port number from the last time the user used the app,
        // put an empty string "" is this is the first time.
        editTextIPAddress.setText(sharedPreferences.getString(PREF_IP, ""));
        editTextPortNumber.setText(sharedPreferences.getString(PREF_PORT, ""));
        editTextLeds.setText(sharedPreferences.getString(PREF_LEDS, ""));

        String sLeds = editTextLeds.getText().toString().trim();
        if (sLeds.length()>0)
            m_nLeds = Integer.parseInt(sLeds);
        else
            m_nLeds = 3; //Valor por defecto


        /*Añado dinámicamente un layot horizaontal con un botón y una imagen*/
        m_aImagenes = new ImageView[MAX_LED];
        for (int n = 0; n<m_nLeds; n++)
            incorporaBotonLed(11 + n);

    }


    public void incorporaBotonLed(int nPin) {
         /*Layout horizontal*/
        LinearLayout lHor = new LinearLayout(this);
        lHor.setOrientation(LinearLayout.HORIZONTAL);
        lHor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout ll = (LinearLayout)findViewById(R.id.layout_general);
        ll.addView(lHor);

        /*Boton*/
        Button btnLed = new Button(this);
        btnLed.setText("Pin " + Integer.toString(nPin));
        btnLed.setOnClickListener(this);
        btnLed.setId(BUTTON_INDEX + nPin);
        lHor.addView(btnLed, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        /*Imagen*/
        ImageView imgLed = new ImageView(MainActivity.this);
        imgLed.setImageResource(R.drawable.off);
        lHor.addView(imgLed, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3));
        m_aImagenes[nPin]=imgLed;
    }

    @Override
    public void onClick(View view) {
        // get the pin number
        String parameterValue = "";
        // get the ip address
        String ipAddress = editTextIPAddress.getText().toString().trim();
        // get the port number
        String portNumber = editTextPortNumber.getText().toString().trim();
        //Número de leds
        String leds = editTextLeds.getText().toString().trim();


        // save the IP address and port for the next time the app is used
        editor.putString(PREF_IP, ipAddress); // set the ip address value to save
        editor.putString(PREF_PORT, portNumber); // set the port number to save
        editor.putString(PREF_LEDS, leds); // Pongo el numero de leds
        editor.commit(); // save the IP and PORT

        parameterValue = Integer.toString(view.getId()-BUTTON_INDEX);
        ponMensaje(parameterValue);

        // execute HTTP request
        if(ipAddress.length()>0 && portNumber.length()>0) {
            new HttpRequestAsyncTask(
                    view.getContext(), parameterValue, ipAddress, portNumber, "pin"
            ).execute();
        }
    }

    public void cambiaImagen(int nLed, String sEstado){
        if (nLed==0 || sEstado==null){
            ponMensaje("La respuesta del servidor no es adecuada. Debe ser similar a 'pin12:ON'");
        } else {
            int resImagenLed = getResources().getIdentifier(sEstado, "drawable", getPackageName());
            ImageView imgLed = m_aImagenes[nLed];
            imgLed.setImageResource(resImagenLed);
        }

    }

    /**
     * Description: Send an HTTP Get request to a specified ip address and port.
     * Also send a parameter "parameterName" with the value of "parameterValue".
     * @param parameterValue the pin number to toggle
     * @param ipAddress the ip address to send the request to
     * @param portNumber the port number of the ip address
     * @param parameterName
     * @return The ip address' reply text, or an ERROR message is it fails to receive one
     */
    public String sendRequest(String parameterValue, String ipAddress, String portNumber, String parameterName) throws IOException {
        InputStream is =null;

        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL("http://"+ipAddress+":"+portNumber+"/?"+parameterName+"="+parameterValue);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }


    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    public void ponMensaje(String sMensaje) {
        try {
            TextView t = (TextView) findViewById(R.id.info);
            t.setText(sMensaje);
        } catch (Exception e) {
            e.printStackTrace();
            //02-10 10:59:49.217  29509-31503/es.fjaraba.arduinoled W/System.err﹕ android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
        }
    }


    /**
     * An AsyncTask is needed to execute HTTP requests in the background so that they do not
     * block the user interface.
     */
    private class HttpRequestAsyncTask extends AsyncTask< Void, Void, Void> {

        // declare variables needed
        private String requestReply,ipAddress, portNumber;
        private Context context;
        private String parameter;
        private String parameterValue;
        private int    m_nLed;
        private String m_sEstado;

        /**
         * Description: The asyncTask class constructor. Assigns the values used in its other methods.
         * @param context the application context, needed to create the dialog
         * @param parameterValue the pin number to toggle
         * @param ipAddress the ip address to send the request to
         * @param portNumber the port number of the ip address
         */
        public HttpRequestAsyncTask(Context context, String parameterValue, String ipAddress, String portNumber, String parameter)
        {
            this.context = context;
            this.ipAddress = ipAddress;
            this.parameterValue = parameterValue;
            this.portNumber = portNumber;
            this.parameter = parameter;
        }


        /**
         * Name: doInBackground
         * Description: Sends the request to the ip address
         * @param voids
         * @return
         */
        @Override
        protected Void doInBackground(Void... voids) {

            ponMensaje("Datos enviados, esperando respuesta del servidor...");
            try {
                requestReply = sendRequest(parameterValue,ipAddress,portNumber, parameter);

                /*Parseamos la respuesta*/
                if (requestReply.contains(":")) {
                    String[] sParts = requestReply.split(":");
                    String sPin = sParts[0].substring(3, 5);
                    m_nLed = Integer.parseInt(sPin);
                    m_sEstado = (sParts[1].substring(0,2).equals("ON"))?"on":"off";
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Name: onPostExecute
         * Description: This function is executed after the HTTP request returns from the ip address.
         * The function sets the dialog's message with the reply text from the server and display the dialog
         * if it's not displayed already (in case it was closed by accident);
         * @param aVoid void parameter
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            ponMensaje("Respuesta del servidor: " + requestReply);

            cambiaImagen(m_nLed, m_sEstado);
        }

        /**
         * Name: onPreExecute
         * Description: This function is executed before the HTTP request is sent to ip address.
         * The function will set the dialog's message and display the dialog.
         */
        @Override
        protected void onPreExecute() {
            ponMensaje("Enviando datos al servidor...");
        }

    }
}
