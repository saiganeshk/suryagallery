package com.saiganeshk.suryagallery;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.truba.touchgallery.GalleryWidget.GalleryViewPager;
import ru.truba.touchgallery.GalleryWidget.UrlPagerAdapter;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.saiganeshk.suryagallery.utils.CheckNetAvailability;
import com.saiganeshk.suryagallery.utils.CommunicationModule;

public class Home extends Activity {
	private ProgressBar downloadProgressBar;
	private Button refreshButton;
	private GalleryViewPager galleryPager;
	public static ArrayList<String> imageUrlList = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		galleryPager = (GalleryViewPager) findViewById(R.id.galleryPager);
		downloadProgressBar = (ProgressBar) findViewById(R.id.downloadProgressBar);
		refreshButton = (Button) findViewById(R.id.refreshButton);
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				downloadImages();
			}
		});
		
		loadImageGallery();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	private void downloadImages() {
		if (CheckNetAvailability.isAvailable(this)) {
			downloadProgressBar.setProgress(0);
			downloadProgressBar.setVisibility(View.VISIBLE);
			refreshButton.setVisibility(View.GONE);
			
			ImageDownloadAsyncTask imageTask = new ImageDownloadAsyncTask();
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		    else {
		    	imageTask.execute();
		    }
		}
		else {
			Toast.makeText(this, "We are experiencing limited connectivity to our servers. Please check your internet connection", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void loadImageGallery() {
		try {
			galleryPager.removeAllViewsInLayout();
			imageUrlList.clear();
			
			File root = Environment.getExternalStorageDirectory();
			String basePath = root.getAbsolutePath() + "/suryagallery/images/";
			File imageBase = new File(basePath);
			
			if (imageBase.exists() && imageBase.isDirectory()) {
				String[] files = imageBase.list();
				for (String fileName : files) {
					String url = "file://" + new File(basePath+fileName).getAbsolutePath();
					imageUrlList.add(url);
				}
				
				UrlPagerAdapter pagerAdapter = new UrlPagerAdapter(this, Home.imageUrlList);
				galleryPager.setOffscreenPageLimit(3);
				galleryPager.setAdapter(pagerAdapter);
			}
			else {
				downloadImages();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class ImageDownloadAsyncTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
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
					CommunicationModule.saveToFile(fileUrl, fileName, Home.this);
					publishProgress(step);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			downloadProgressBar.incrementProgressBy(values[0]);
			
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			try {
				downloadProgressBar.setProgress(0);
				downloadProgressBar.setVisibility(View.GONE);
				refreshButton.setVisibility(View.VISIBLE);
				loadImageGallery();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			super.onPostExecute(result);
		}
	}
}
