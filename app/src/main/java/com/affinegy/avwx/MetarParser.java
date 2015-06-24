package com.affinegy.avwx;
/**
 * Created by jhoward on 6/23/2015.
 */

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class MetarParser {
    private static final String TAG = "MetarParser";
    private XmlPullParserFactory xmlFactoryObject = null;
    private XmlPullParser myparser = null;
    private DownloadManager dm;
    Context mContext = null;
    private long mEnqueue = 0;


    public MetarParser(Context context) {
        mContext = context;
        trustEveryone();
        try {
            xmlFactoryObject = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e1) {
            Log.wtf(TAG, "Parser newInstance Exception: " + e1.getMessage());
            e1.printStackTrace();
        }
        try {
            myparser = xmlFactoryObject.newPullParser();
        } catch (XmlPullParserException e1) {
            Log.wtf(TAG, "Parser newPullParser Exception: " + e1.getMessage());
            e1.printStackTrace();
        }
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(DownloadFilesTask.FILE_PARSE_COMPLETE));
    }

    private String ParseMetarToJson(String xml) {
        JSONObject jsonObj = null;
        try {
            jsonObj = XML.toJSONObject(xml);
        } catch (JSONException e) {
            Log.e("JSON exception", e.getMessage());
            e.printStackTrace();
        }
        Log.d("JSON", jsonObj.toString());
        return jsonObj.toString();
    }

    private String ParseMetar(String xml) {
        try {
            myparser.setInput(new StringReader(xml));
            int eventType = myparser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(TAG, "Start document");
                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    Log.d(TAG, "End document");
                } else if (eventType == XmlPullParser.START_TAG) {
                    Log.d(TAG, "Start tag " + myparser.getName());
                } else if (eventType == XmlPullParser.END_TAG) {
                    Log.d(TAG, "End tag " + myparser.getName());
                } else if (eventType == XmlPullParser.TEXT) {
                    Log.d(TAG, "Text " + myparser.getText());
                }
                eventType = myparser.next();
            }
        } catch(Exception e) {
            return "";
        }
        return "";
    }

    public String GetMetar(String url) {
        String result = "";
        try {
            Log.i(TAG, "calling DownloadFilesTask");
            DownloadFilesTask task = new DownloadFilesTask(mContext);
            task.execute(new URL("https://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=KDEN%20KSEA,%20PHNL&hoursBeforeNow=2") );
            result = task.html;
        }   catch (MalformedURLException e) {
            Log.wtf(TAG, "Malformed URL: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
        return result;
    }


    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            Log.wtf(TAG, "trust exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String xml = intent.getStringExtra(DownloadFilesTask.DOWNLOADED_HTML);
            Log.d("receiver", "Got message: " + xml);  //broadcast to caller, then destroy LBM
          //  ParseMetar(xml);
           String result = ParseMetarToJson(xml);
            Log.d(TAG, result);
        }
    };

}
