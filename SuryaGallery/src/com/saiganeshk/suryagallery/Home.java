package com.saiganeshk.suryagallery;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.saiganeshk.suryagallery.utils.CheckNetAvailability;
import com.saiganeshk.suryagallery.utils.CommunicationModule;
import com.saiganeshk.suryagallery.utils.MemoryCache;

public class Home extends ActionBarActivity {
	private ProgressBar downloadProgressBar, imageProgressBar;
	private Button refreshButton;
	private GridView gridView;
	private DisplayMetrics dm;
	MemoryCache cache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
		cache = new MemoryCache();
		
		downloadProgressBar = (ProgressBar) findViewById(R.id.downloadProgressBar);
		imageProgressBar = (ProgressBar) findViewById(R.id.imageProgressBar);
		refreshButton = (Button) findViewById(R.id.refreshButton);
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				downloadImages();
			}
		});
		
		loadImageToGrids();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
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
	
	private void loadImageToGrids() {
		try {
			gridView = (GridView) findViewById(R.id.gridView);
			gridView.removeAllViewsInLayout();
			imageProgressBar.setVisibility(View.VISIBLE);
			
			File root = Environment.getExternalStorageDirectory();
			String basePath = root.getAbsolutePath() + "/suryagallery/images/";
			File imageBase = new File(basePath);
			
			if (imageBase.exists() && imageBase.isDirectory()) {
				String[] files = imageBase.list();
				ImageGridAdapter gridAdapter = new ImageGridAdapter(this, files, basePath);
				gridView.setAdapter(gridAdapter);
			}
			else {
				downloadImages();
			}
			
			imageProgressBar.setVisibility(View.GONE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class ImageGridAdapter extends BaseAdapter {
		private String basePath;
		private String[] objects;
		
		public ImageGridAdapter(Context context, String[] objects, String basePath) {
			this.basePath = basePath;
			this.objects = objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			RelativeLayout gridItemLayout = (RelativeLayout) LayoutInflater.from(Home.this).inflate(R.layout.grid_item, null);
			
			try {
				String filePath = this.basePath+this.getItem(position);
				File imageFile = new File(filePath);
				
				if (imageFile.exists()) {
					ImageViewLoadAsyncTask imageLoadTask = new ImageViewLoadAsyncTask();
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						imageLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filePath);
					}
				    else {
				    	imageLoadTask.execute(filePath);
				    }
					
					Bitmap bitmap = imageLoadTask.get();
					ImageView imageView = (ImageView) gridItemLayout.findViewById(R.id.imageView);
					imageView.setImageBitmap(bitmap);
					bitmap = null;
				}
				else {
					gridItemLayout.setVisibility(View.GONE);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			return gridItemLayout;
		}

		@Override
		public int getCount() {
			return this.objects.length;
		}

		@Override
		public Object getItem(int position) {
			return this.objects[position];
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
	
	class ImageViewLoadAsyncTask extends AsyncTask<String, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bitmap = cache.getBitmapFromMemCache(params[0]);
			if (bitmap != null) {
				return bitmap;
			}
			
			try {
				bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(params[0], null), dm.widthPixels/2, dm.heightPixels/4, false);
				cache.addBitmapToMemoryCache(params[0], bitmap);
				
				return bitmap;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
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
				Thread.sleep(100);
				downloadProgressBar.setProgress(0);
				downloadProgressBar.setVisibility(View.GONE);
				refreshButton.setVisibility(View.VISIBLE);
				loadImageToGrids();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			super.onPostExecute(result);
		}
	}
}
