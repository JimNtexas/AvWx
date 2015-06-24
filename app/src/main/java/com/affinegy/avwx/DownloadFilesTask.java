package com.affinegy.avwx;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by jhoward on 6/23/2015.
 */
public class DownloadFilesTask extends AsyncTask<URL, Integer, Long>{
    static final String TAG = "DownloadFilesTask";
    public final static String FILE_PARSE_COMPLETE = "file_parse_complete";
    public final static String DOWNLOADED_HTML = "downloaded_html";
    public static String html = "";
    private Context mContext = null;
        protected Long doInBackground (URL...urls){
        int count = urls.length;
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            totalSize += DownloadFilesTask.downloadFile(urls[0]);
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return totalSize;
    }

    DownloadFilesTask(Context context) {
        mContext = context;
    }

    protected void onProgressUpdate(Integer... progress) {
        Log.i(TAG, "progress");
    }

    protected void onPostExecute(Long result) {
        Log.d(TAG, "Broadcasting result");
        Intent intent = new Intent(FILE_PARSE_COMPLETE);
        // You can also include some extra data.
        intent.putExtra(DOWNLOADED_HTML, html);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static Long downloadFile(URL url) {

        Long cnt = Long.valueOf(0);
        if (url == null) {
            Log.wtf(TAG, "could not download null url");
            return Long.valueOf(-1);
        }

        try {
            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                //Log.i(TAG, line);
                cnt += Long.valueOf(line.length());
                html += line;
            }
            in.close();
        } catch (IOException e) {
            Log.wtf(TAG, "IO excepetion downloading file");
            e.printStackTrace();
            return Long.valueOf(-1);
        } catch (Exception e) {
            Log.wtf(TAG, "unknown exception: " + e.getMessage());
        }
        return Long.valueOf(cnt);
    }
}