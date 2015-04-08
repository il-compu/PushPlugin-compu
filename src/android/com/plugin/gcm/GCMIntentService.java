package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;  
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.MalformedURLException;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			// if we are in the foreground, just surface the payload, else post it to the statusbar
			//xxx
			Log.d(TAG, "COMPU: MESSAGE RECEVIED "+extras.getString("msgid"));
			Log.d(TAG, "COMPU: " + PushPlugin.getDeliveryReceiptURL());
			//xxx
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
			}
			else {
				extras.putBoolean("foreground", false);

 
   HttpURLConnection urlConnection = null;
    try {
        // create connection
//        URL urlToRequest = new URL(PushPlugin.getDeliveryReceiptURL()+"?i="+extras.getString("msgid"));
        URL urlToRequest = new URLextras.getString("ConfirmURL"));
        urlConnection = (HttpURLConnection) urlToRequest.openConnection();
        //urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        //urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);
         
        // handle issues
        int statusCode = urlConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            // handle unauthorized (if service requires user login)
		Log.d(TAG, "COMPU: HTTP_UNAUTH");
        } else if (statusCode != HttpURLConnection.HTTP_OK) {
            // handle any other errors, like 404, 500,..
		Log.d(TAG, "COMPU: HTTP_ERROR");
        }
 
BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 1024 * 16);
StringBuffer builder = new StringBuffer();
String line;
while ((line = reader.readLine()) != null) {
  builder.append(line).append("\n");
}

Log.d(TAG, "COMPU: RECEIPT "+builder.toString());
extras.putString("deliveryReceipt", builder.toString());
			
 /*        
        // create JSON object from content
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        return new JSONObject(getResponseText(in));
*/

    } catch (MalformedURLException e) {
        // URL is invalid
	Log.d(TAG, "COMPU: URL invalid");
    } catch (SocketTimeoutException e) {
        // data retrieval or connection timed out
	Log.d(TAG, "COMPU: Socket Timeout");
    } catch (IOException e) {
        // could not read response body 
        // (could not create input stream)
	Log.d(TAG, "COMPU: IO Exception");
/*
    } catch (JSONException e) {
        // response body is no valid JSON string
	Log.d(TAG, "COMPU: URL invalid");
*/
    } finally {
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
    }       

				PushPlugin.sendExtras(extras);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(context, extras);
                }
            }
        }
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}
		
		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
		int notId = 0;
		
		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}
		
		mNotificationManager.notify((String) appName, notId, mBuilder.build());
	}
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
