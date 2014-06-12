package com.saiganeshk.suryagallery.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.saiganeshk.suryagallery.R;

public class ImageDownloadService extends IntentService {
	private final static String TAG = "com.saiganeshk.suryagallery.utils.ImageDownloadService";
    public static final String BROADCAST_ACTION = TAG + ".BROADCAST";
    public static final String STATUS = TAG + ".STATUS";
    public static final String SUCCESS = "success", FAIL = "fail", NO_NETWORK = "no network", EXCEPTION = "exception", DONE = "done";
    public static int progress = 0;
    
	public ImageDownloadService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String statusString = EXCEPTION;
		Log.d(TAG, "Starting image download service");
		
		try {
			if (CheckNetAvailability.isAvailable(this)) {
				download();
				statusString = DONE;
			}
			else {
				statusString = NO_NETWORK;
			}
		}
		catch (Exception e) {
			Log.e(TAG, e.getMessage());
			statusString = EXCEPTION;
		}
		

		Log.d(TAG, "Download status:"+statusString);
		
		Intent responseIntent = new Intent(BROADCAST_ACTION).putExtra(STATUS, statusString);
		LocalBroadcastManager.getInstance(this).sendBroadcastSync(responseIntent);
	}
	
	private void download() {
		String metaApi = getResources().getString(R.string.api) + "?meta=true";
		String metaResponse = CommunicationModule.load(metaApi, CommunicationModule.GET, null);
		
		try {
			JSONObject metaJson = new JSONObject(metaResponse);
			
			JSONArray files = metaJson.getJSONObject("contents").getJSONArray("files");
			int count = files.length();
			int step = (int) Math.ceil(100.0/count);
			
			for (int index=0; index<count; index++) {
				String fileName = files.getString(index);
				String fileUrl = getResources().getString(R.string.api) + "?file=" + fileName;
				Log.i(TAG, "Downloading from: "+fileUrl);
				
				boolean status = CommunicationModule.saveToFile(fileUrl, fileName, this, CommunicationModule.URL);
				progress = step;
				String statusString = (status) ? SUCCESS : FAIL;
				Intent responseIntent = new Intent(BROADCAST_ACTION).putExtra(STATUS, statusString);
				LocalBroadcastManager.getInstance(this).sendBroadcastSync(responseIntent);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
