package ph.com.strongtechsolutions.bb5.smsreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.AsyncTask;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author  Armande Bayanes
 * @date    9/3/2015
 */

public class IncomingSms extends BroadcastReceiver {

    public String PHP_SCRIPT_URL = "http://192.168.1.4/phpTest/android.php";

    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if(bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for(int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    String sender = currentMessage.getDisplayOriginatingAddress(); // Sender's phone #.
                    Long date = currentMessage.getTimestampMillis(); // Date sent in milliseconds.
                    String message = currentMessage.getDisplayMessageBody(); // Message content.
                    String serviceCenter = currentMessage.getServiceCenterAddress();

                    TalkToPHPServerTask task = new TalkToPHPServerTask();
                    task.execute("sender="+ sender + "&message=" + message +"&date="+ date +"&service="+ serviceCenter);
                }
            }

        } catch(Exception e) {

        }
    }

    // AsyncTask to communicate with the PHP Server script.
    public class TalkToPHPServerTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... parameters) {

            String response = "";

            try {

                URL request_url = new URL(PHP_SCRIPT_URL);
                HttpURLConnection request = (HttpURLConnection) request_url.openConnection();

                request.setDoInput(true);
                request.setDoOutput(true);

                request.setRequestMethod("POST");
                request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                request.setFixedLengthStreamingMode(parameters[0].getBytes().length);
                PrintWriter OUT = new PrintWriter(request.getOutputStream());
                OUT.print(parameters[0]);
                OUT.flush(); OUT.close();

                // Retrieve SERVER response.
                InputStream IN = request.getInputStream();
                StringBuffer buffer = new StringBuffer();
                try {

                    int character;
                    while ((character = IN.read()) != -1) {
                        buffer.append((char) character);
                    }

                    response = buffer.toString();

                } catch(Exception e) {

                    response = "Error: "+ e.toString();

                } finally {

                    IN.close();
                }

            } catch(Exception e) {

                response = "Error: "+ e.toString();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {

            SmsManager.getDefault().sendTextMessage(result, null, "You got 1,000,000 load.", null, null);
        }
    }
}
