package com.saiganeshk.suryagallery;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.truba.touchgallery.GalleryWidget.GalleryViewPager;
import ru.truba.touchgallery.GalleryWidget.UrlPagerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.saiganeshk.suryagallery.utils.CheckNetAvailability;
import com.saiganeshk.suryagallery.utils.CommunicationModule;

public class Home extends Activity {
	private ProgressBar downloadProgressBar;
	private Button refreshButton;
	private GalleryViewPager galleryPager;
	public static ArrayList<String> imageUrlList = new ArrayList<String>();
	private final String validationKeyFileName = "keyfile.txt";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		validateKey();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	private void validateKey() {
		try {
			String key = CommunicationModule.getFromFile(validationKeyFileName, this);
			
			if (key != null && key.equals(getResources().getString(R.string.key))) {
				renderView();
			}
			else {
				RelativeLayout activationLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activation, null);
				
				final AlertDialog.Builder activationPopup = new AlertDialog.Builder(Home.this);
				activationPopup.setView(activationLayout);
				
				final AlertDialog alertDialog = activationPopup.create();
				alertDialog.setTitle("Activation");
				alertDialog.show();

				alertDialog.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {
		                    dialog.dismiss();
		                    finish();
		                    
		                    return true;
		                }
						
						return false;
					}
				});
				
				final EditText codeInput = (EditText) activationLayout.findViewById(R.id.codeInput);
				Button submitButton = (Button) activationLayout.findViewById(R.id.submit);
				
				submitButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (codeInput.getText() != null) {
							String code = codeInput.getText().toString();
							boolean isValid = false;
							
							try {
								if (code.equals(getResources().getString(R.string.key))) {
									isValid = true;
									renderView();
								}
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							
							if (isValid) {
								CommunicationModule.saveToFile(code, validationKeyFileName, Home.this, CommunicationModule.FILE);
								
								alertDialog.dismiss();
								renderView();
							}
							else {
								Toast.makeText(Home.this, "Invalid key", Toast.LENGTH_LONG).show();
							}
						}
					}
				});
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		catch (Error e) {
			e.printStackTrace();
		}
	}
	
	private void renderView() {
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
				galleryPager.setOffscreenPageLimit(1);
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
					CommunicationModule.saveToFile(fileUrl, fileName, Home.this, CommunicationModule.URL);
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
