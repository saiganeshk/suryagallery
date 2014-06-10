package com.saiganeshk.suryagallery.utils;

import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.saiganeshk.suryagallery.R;

public class CheckNetAvailability {

	private static boolean availabilityStatus; 

	public static boolean isAvailable(final Context context) {

		AsyncTask<Void, Void, Boolean> checkStatus = new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				Boolean status = false;
				try {
					String urlString = context.getResources().getString(R.string.api);
					URL url = new URL(urlString);
					final HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
					urlc.setRequestProperty("User-Agent", "Android Application");
					urlc.setRequestProperty("Connection", "close");
					urlc.setConnectTimeout(3000);
					urlc.connect();

					if (urlc.getResponseCode() == 200) {
						status = true;
					}
					else { 
						status = false;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				return status;
			}
		};
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			checkStatus.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	    else {
	    	checkStatus.execute();
	    }

		try{
			availabilityStatus = checkStatus.get();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return availabilityStatus;
	}
}
