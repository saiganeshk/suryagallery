package com.saiganeshk.suryagallery.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

public class CommunicationModule {
	public static final int GET = 1, POST = 2, FILE = 1, URL = 2;
	
	/**
	 * 
	 * @param url
	 * @param method
	 * @param paramList
	 * @return response
	 */
	public static String load(final String url, final int method, final String paramList) {
		String response = null;

		AsyncTask<Void, Void, String> APIResponse = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				InputStream is = null;
				String responseString = null;
				
				try {
					DefaultHttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = null;
					
					if (method == GET) {
						HttpGet httpGet = new HttpGet(url);
						httpResponse = httpClient.execute(httpGet);
					}
					else if (method == POST) {
						HttpPost httpPost = new HttpPost(url);
						JSONObject obj = new JSONObject(paramList);
						
						@SuppressWarnings("unchecked")
						Iterator<Object> keys = obj.keys();
						List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
						
						while (keys.hasNext()) {
							String key = keys.next().toString();
							nameValuePairs.add(new BasicNameValuePair(key, obj.getString(key)));
						}
						
						UrlEncodedFormEntity encoded = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
						System.out.println("Params:"+nameValuePairs.toString());
						
						httpPost.setEntity(encoded);
						
						httpResponse = httpClient.execute(httpPost);
					}
					
					HttpEntity httpEntity = httpResponse.getEntity();
					is = httpEntity.getContent();
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"), 8);
					StringBuilder sb = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
					is.close();
					
					responseString = sb.toString();

				} catch (Exception e) {
					e.printStackTrace();
				}

				return responseString;
			}

		};
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			APIResponse.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	    else {
	    	APIResponse.execute();
	    }

		try {
			response = APIResponse.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("**** Response for request URL: '"+url+"' is ****\n"+response);
		return response;
	}
	
	public static boolean saveToFile(final String content, final String fileName, final Context context, final int type) {
		boolean status = false;
		
		try {
			if (type == FILE) {
				File root = Environment.getExternalStorageDirectory();
				String path = root.getAbsolutePath() + "/suryagallery/cache/";
				
				File dir = new File(path);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				
				File file = new File(dir, fileName);
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(content.getBytes());
				fos.flush();
				fos.close();
				
				System.out.println("File created with path: "+path+fileName);
				status = true;
			}
			else {
				URL url = new URL(content);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
	            urlConnection.setDoOutput(true);
	            
	            urlConnection.connect();
	            
				File root = Environment.getExternalStorageDirectory();
				String path = root.getAbsolutePath() + "/suryagallery/images/";
				
				File dir = new File(path);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				
				File file = new File(dir, fileName);
				if (!file.exists()) {
					FileOutputStream fos = new FileOutputStream(file);
					
					InputStream inputStream = urlConnection.getInputStream();
					
					byte[] buffer = new byte[1024];
	                int bufferLength = 0;
	                
	                while ((bufferLength = inputStream.read(buffer)) > 0) {
	                    fos.write(buffer, 0, bufferLength);
	                }
					
					fos.flush();
					fos.close();
					
					System.out.println("File created with path: "+path+fileName);
					status = true;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return status;
	}
	
	public static String getFromFile(final String fileName, final Context context) {
		String response = null;
		
		AsyncTask<Void, Void, String> fileTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String result = null;
				
				try {
					File root = Environment.getExternalStorageDirectory();
					String localPath = root.getAbsolutePath() + "/suryagallery/cache/";
					
					File myFile = new File(localPath+fileName);
					if (!myFile.exists()) {
						return null;
					}
					
					FileInputStream fIn = new FileInputStream(myFile);
					BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
					String aDataRow = "";
					String aBuffer = "";
					
					while ((aDataRow = myReader.readLine()) != null) {
						aBuffer += aDataRow + "\n";
					}
					
					myReader.close();
					myReader = null;
					result = aBuffer.trim();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				return result;
			}
		};
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			fileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	    else {
	    	fileTask.execute();
	    }
		
		try {
			response = fileTask.get();
			System.out.println("File content:"+response);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	public static void deleteFile(final String fileName, final Context context) {
		AsyncTask<Void, Void, Void> fileTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				File root = Environment.getExternalStorageDirectory();
				String localPath = root.getAbsolutePath() + "/suryagallery/cache/";
				
				try {
					File myFile = new File(localPath+fileName);
					if (myFile.exists() && myFile.isFile()) {
						myFile.delete();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				return null;
			}
		};
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			fileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	    else {
	    	fileTask.execute();
	    }
	}
}
