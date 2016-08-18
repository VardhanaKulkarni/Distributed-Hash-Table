package edu.buffalo.cse.cse486586.simpledht;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;
import android.telephony.TelephonyManager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import android.util.Log;
import android.os.AsyncTask;


public class SimpleDhtActivity extends Activity {

    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    SimpleDhtProvider contProv = new SimpleDhtProvider();
    final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        //String portStr = "11108";
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e("oncreate", "port " + myPort);
        Context context = getApplicationContext();
        
        contProv.socketCreate(myPort,context);
        contProv.initHashMap();
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPort);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
        String strReceived = strings[0].trim();
        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        remoteTextView.append(strReceived+"\n");

        return;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            String myP = msgs[0];
            if (!myP.equals(REMOTE_PORT[0])) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[0])));
                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.write("JOIN"+"%"+myP);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in Activity"+ " "+e.toString());
                }
            }
            return null;
        }
    }
}
