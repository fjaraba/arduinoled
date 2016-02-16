package es.fjaraba.arduinoled;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/*
    Fernando Jaraba Nieto. Febrero 2016

    App que permite enviar comandos a una ESP8266 conectada a un Arduino.
    La aplicación recibe información del estado del LED del Arduino y muestra una imagen indicándolo.
    Tambien tiene incorpora una pantalla de configuración.

 */

public class MainActivity extends Activity implements View.OnClickListener {

    public final static String PREF_IP       = "PREF_IP_ADDRESS";
    public final static String PREF_PORT     = "PREF_PORT_NUMBER";
    public final static String PREF_LEDS     = "PREF_LEDS";
    public final static String PREF_MIN_PIN  = "PREF_MIN_PIN";
    public final static String PREF_MULTIPLE = "PREF_MULTIPLE";

    public final static int MAX_LED = 20;
    public final static int BUTTON_INDEX = 2000;

    private TextView m_txtConf;
    private SharedPreferences m_prefs;
    private String m_sIP;
    private int m_nPort;
    private int m_nLeds;
    private int m_nPinInicial;
    private boolean m_bMultiplesConex;
    private boolean m_bEjecutando;
    private ImageView m_aImagenes[] = new ImageView[MAX_LED];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_bEjecutando = false;

        //Botón configurar
        Button btnConf = (Button) findViewById(R.id.btn_conf);
        btnConf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Muestro el Intent de las preferencias indicando que quiero respuesta.
                Intent intent = new Intent(MainActivity.this, PrefsActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        //Parseo la configuración.
        m_txtConf = (TextView)findViewById(R.id.direccionRemota);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        parseaPreferencias();
    }

    /*Nos avisa cuando se finalice el intent de preferencias*/
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        parseaPreferencias();
    }


    public void parseaPreferencias() {
        int nLeds = m_nLeds;
        int nPinInicial = m_nPinInicial;

        //Leo las preferencias
        m_sIP = m_prefs.getString(PREF_IP, "");
        m_nPort = Integer.parseInt(m_prefs.getString(PREF_PORT, "80"));
        m_nPinInicial = Integer.parseInt(m_prefs.getString(PREF_MIN_PIN, "11"));
        m_bMultiplesConex = m_prefs.getBoolean(PREF_MULTIPLE, false);
        m_nLeds = Integer.parseInt(m_prefs.getString(PREF_LEDS, "3"));
        if (m_nLeds>=(MAX_LED-m_nPinInicial))
            m_nLeds = MAX_LED-m_nPinInicial-1;

        //Pongo un literal indicando las preferencias
        String strConf = "Dirección remota:" + m_sIP;
        strConf += ":" + Integer.toString(m_nPort);
        strConf += "\nPin inicial:" + Integer.toString(m_nPinInicial);
        strConf += ", total leds:" + Integer.toString(m_nLeds);
        m_txtConf.setText(strConf);

        //Compruebo si se ha modificado el número de botones
        if (nLeds!=m_nLeds || nPinInicial!=m_nPinInicial){
            //Elimino los botones anteriores
            LinearLayout ll = (LinearLayout) findViewById(R.id.layout_botones);
            ll.removeAllViews();

            //Añado dinámicamente un layot horizaontal con un botón y una imagen
            for (int n = 0; n < m_nLeds; n++)
                incorporaBotonLed(m_nPinInicial + n);
        }

        ponMensaje("");
    }

    /* Incorporo un layout con el botón y la imagen */
    public void incorporaBotonLed(int nPin) {
         /*Layout horizontal*/
        LinearLayout lHor = new LinearLayout(this);
        lHor.setOrientation(LinearLayout.HORIZONTAL);
        lHor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout ll = (LinearLayout)findViewById(R.id.layout_botones);
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

        if (!m_bEjecutando) {
            synchronized(this) {
                m_bEjecutando = true;

                //Si no se permiten múltiple conexiones deshabilito los botones
                if (!m_bMultiplesConex) {
                    for (int n = 0; n < m_nLeds; n++) {
                        Button btn = (Button) findViewById(BUTTON_INDEX + m_nPinInicial + n);
                        btn.setEnabled(false);
                        btn.setClickable(false);
                    }
                }

                // get the pin number
                String parameterValue = Integer.toString(view.getId() - BUTTON_INDEX);

                // execute HTTP request
                if (m_sIP.length() > 0 && m_nPort > 0) {
                    new HttpRequestAsyncTask(
                            view.getContext(), parameterValue, m_sIP, Integer.toString(m_nPort), "pin"
                    ).execute();
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Espere a que termine la petición anterior", Toast.LENGTH_SHORT).show();
        }
    }

    public void cambiaImagen(int nLed, String sEstado){
        if (nLed==0 || sEstado==null){
            //ponMensaje("La respuesta del servidor no es adecuada. Debe ser similar a 'pin12:ON'");
        } else {
            int resImagenLed = getResources().getIdentifier(sEstado, "drawable", getPackageName());
            ImageView imgLed = m_aImagenes[nLed];
            imgLed.setImageResource(resImagenLed);
        }

        //Habilito los botones
        if (!m_bMultiplesConex) {
            for (int n = 0; n < m_nLeds; n++) {
                Button btn = (Button) findViewById(BUTTON_INDEX + m_nPinInicial + n);
                btn.setEnabled(true);
                btn.setClickable(true);
            }
        }
        m_bEjecutando = false;
    }

    /**
     * Description: Send an HTTP Get request to a specified ip address and port.
     * Also send a parameter "parameterName" with the value of "parameterValue".
     * @param parameterValue the pin number to toggle
     * @param ipAddress the ip address to send the request to
     * @param portNumber the port number of the ip address
     * @param parameterName Nombre del parametro a enviar
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
            return readIt(is, len);

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
        Reader reader;
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
         * @param voids parámetros
         * @return retorno
         */
        @Override
        protected Void doInBackground(Void... voids) {

            ponMensaje("Datos enviados, esperando respuesta del servidor...");
            try {
                requestReply = sendRequest(parameterValue,ipAddress,portNumber, parameter);

                /*Parseamos la respuesta*/
                if (requestReply.contains(":")) {
                    String[] sParts = requestReply.split(":");
                    try {
                        String sPin = sParts[0].substring(3, 5);
                        m_nLed = Integer.parseInt(sPin);
                        m_sEstado = (sParts[1].substring(0, 2).equals("ON")) ? "on" : "off";
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
