package es.fjaraba.arduinoled;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/*
    Fernando Jaraba Nieto. Febrero 2016

    App que permite enviar comandos a una ESP8266 conectada a un Arduino.
    La aplicación recibe información del estado del LED del Arduino y muestra una imagen indicándolo.
    Tambien tiene incorpora una pantalla de configuración.

    https://github.com/fjaraba/arduinoled

 */

public class MainActivity extends Activity implements View.OnClickListener {
    //Strings de preferencias
    public final static String PREF_IP       = "PREF_IP_ADDRESS";
    public final static String PREF_PORT     = "PREF_PORT_NUMBER";
    public final static String PREF_LEDS     = "PREF_LEDS";
    public final static String PREF_MIN_PIN  = "PREF_MIN_PIN";
    public final static String PREF_MULTIPLE = "PREF_MULTIPLE";

    //Preferencias
    private SharedPreferences m_prefs;
    private String m_sIP;
    private int m_nPort;
    private int m_nLeds;
    private int m_nPinInicial;
    private boolean m_bMultiplesConex;

    //Constantes
    public final static int MAX_LED = 20;
    public final static int BUTTON_INDEX = 2000;


    private TextView m_txtConf;
    private ImageView m_aImagenes[];

    public MainActivity() {
        //Inicializo las variables miembro
        m_aImagenes = new ImageView[MAX_LED];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Parseo la configuración.
        m_txtConf = (TextView)findViewById(R.id.direccionRemota);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        parseaPreferencias();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Inflo el XML con el menú
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        //Permite mostrar/ocutal elementos del menu
    }
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.about: {
                //ShowAbout();
                return true;
            }
            case R.id.refrescar: {
                //Solicito al ESP8266 el estado de los leds del arduino
                preguntaEstadoGlobal();
                return true;
            }
            case R.id.configuracion: {
                //Muestro el Intent de las preferencias indicando que quiero respuesta.
                Intent intent = new Intent(MainActivity.this, PrefsActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
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

        //Pongo un literal indicando las preferencias seleccionadas
        String strConf = "Dirección remota:" + m_sIP;
        strConf += ":" + Integer.toString(m_nPort);
        //strConf += "\nPin inicial:" + Integer.toString(m_nPinInicial);
        //strConf += ", total leds:" + Integer.toString(m_nLeds);
        m_txtConf.setText(strConf);

        //Compruebo si se ha modificado el número de botones
        if (nLeds!=m_nLeds || nPinInicial!=m_nPinInicial){
            //Elimino los botones anteriores
            LinearLayout ll = (LinearLayout) findViewById(R.id.layout_botones);
            ll.removeAllViews();

            //Añado dinámicamente tantos layout horizontales con botón e imagen
            //como leds se quieran controlar
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

    public void habilitaBotones(boolean bHabilitar){
        if (!m_bMultiplesConex) {
            //Botones de los leds
            for (int n = 0; n < m_nLeds; n++) {
                Button btn = (Button) findViewById(BUTTON_INDEX + m_nPinInicial + n);
                btn.setEnabled(bHabilitar);
                btn.setClickable(bHabilitar);
            }
            //Botón refrescar
            /*Button btnRefrescar = (Button) findViewById(R.id.btn_refrescar);
            btnRefrescar.setEnabled(bHabilitar);*/
        }
    }

    @Override
    public void onClick(View view) {

        synchronized(this) {
            //Si no se permiten múltiple conexiones deshabilito los botones
            habilitaBotones(false);

            // Obtengo el número del PIN
            String parameterValue = Integer.toString(view.getId() - BUTTON_INDEX);

            // Ejecuto la petición
            if (m_sIP.length() > 0 && m_nPort > 0) {
                new HttpRequestAsyncTask(
                    view.getContext(), parameterValue, m_sIP, Integer.toString(m_nPort), "pin"
                ).execute();
            }
        }
    }

    /*Hace una consulta del estado de todos los leds y los actualiza dependiendo la respuesta de la placa.*/
    public void preguntaEstadoGlobal(){
        habilitaBotones(false);

        // Ejecuto la petición
        if (m_sIP.length() > 0 && m_nPort > 0) {
            new HttpRequestAsyncTask(
                    getApplicationContext(), Integer.toString(m_nPinInicial), m_sIP, Integer.toString(m_nPort), "global"
            ).execute();
        }
    }

    public void cambiaImagen(int nLed, String sEstado){
        if ((nLed!=0) && !(sEstado==null)){
            int resImagenLed = getResources().getIdentifier(sEstado, "drawable", getPackageName());
            ImageView imgLed = m_aImagenes[nLed];
            imgLed.setImageResource(resImagenLed);
        }
    }

    void parseaLed(String str){
        //Respuesta a un único Led. Ej: led11:on
        String[] sParts = str.split(":");
        String sPin = sParts[0].substring(3, 5);
        int nLed = Integer.parseInt(sPin);
        String sEstado = (sParts[1].substring(0, 2).equals("ON")) ? "on" : "off";
        cambiaImagen(nLed, sEstado);
    }

    void parseaRespuestaHTTP(String requestReply){
        try {
            if (requestReply.contains("global&")) {
                //Respuesta global. Ej: global&led11:on&led12:off&led13:on
                String[] sParts = requestReply.split("&");
                for (int n=1; n<sParts.length;n++){
                    parseaLed(sParts[n]);
                }
            } else if (requestReply.contains(":")) {
                //Respuesta a un único Led. Ej: led11:on
                parseaLed(requestReply);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            //Habilito los botones
            habilitaBotones(true);
        }

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
                requestReply = sendRequest(parameterValue ,ipAddress, portNumber, parameter);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Name: onPostExecute
         * Description: This function is executed after the HTTP request returns from the ip address.
         * @param aVoid void parameter
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            ponMensaje("Respuesta del servidor: " + requestReply);
            parseaRespuestaHTTP(requestReply);
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

            // Paso los datos del InputStream a un String
            java.util.Scanner s = new java.util.Scanner(is);
            String str = s.useDelimiter("\\A").hasNext() ? s.next(): "";
            s.close();
            return str;

            // Makes sure that the InputStream is closed after the app is finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }
}
