/*
 Copyright (c) 2012 Roman Truba

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ru.truba.touchgallery.TouchView;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import ru.truba.touchgallery.R;
import ru.truba.touchgallery.TouchView.InputStreamWrapper.InputStreamProgressListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UrlTouchImageView extends RelativeLayout {
    protected ProgressBar mProgressBar;
    protected TouchImageView mImageView;
    protected TextView mTitleTextView;
    protected TextView mBottomTextView;

    protected Context mContext;

    public UrlTouchImageView(Context ctx)
    {
        super(ctx);
        mContext = ctx;
        init();

    }
    public UrlTouchImageView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        mContext = ctx;
        init();
    }
    public TouchImageView getImageView() { return mImageView; }

    @SuppressWarnings("deprecation")
    protected void init() {
        mImageView = new TouchImageView(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        mImageView.setLayoutParams(params);
        this.addView(mImageView);
        mImageView.setVisibility(GONE);

        mProgressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleHorizontal);
        params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.setMargins(30, 0, 30, 0);
        mProgressBar.setLayoutParams(params);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setMax(100);
        this.addView(mProgressBar);
        
        mTitleTextView = new TextView(mContext);
        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mTitleTextView.setLayoutParams(params);
        mTitleTextView.setTextColor(Color.WHITE);
        this.addView(mTitleTextView);
        
        mBottomTextView = new TextView(mContext);
        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mBottomTextView.setLayoutParams(params);
        mBottomTextView.setTextColor(Color.WHITE);
        this.addView(mBottomTextView);
    }

    public void setScaleType(ImageView.ScaleType scaleType)
    {
    	mImageView.setScaleType(scaleType);
    }
    
    public void setTitleText(String titleText)
    {
    	mTitleTextView.setText(titleText);
    }
    
    public void setBottomText(String bottomText)
    {
    	mBottomTextView.setText(bottomText);
    }
    
    public void setUrl(String imageUrl)
    {
        new ImageLoadTask().execute(imageUrl);
    }
    //No caching load
    public class ImageLoadTask extends AsyncTask<String, Integer, Bitmap>
    {
        @Override
        protected Bitmap doInBackground(String... strings) {
            String url = strings[0];
            InputStreamWrapper bis = null;
            Bitmap bitmap = null;
            
            try {
                URL aURL = new URL(url);
                URLConnection conn = aURL.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                int totalLen = conn.getContentLength();
                bis = new InputStreamWrapper(is, 8192, totalLen);
                bis.setProgressListener(new InputStreamProgressListener()
				{					
					@Override
					public void onProgress(float progressValue, long bytesLoaded, long bytesTotal)
					{
						publishProgress((int)(progressValue * 100));
					}
				});
                
                bitmap = generateBitmap(bis, 2);
                bis.close();
                is.close();
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
            catch (Error e) {
            	System.gc();
				e.printStackTrace();
			}
            
            return bitmap;
        }
        
        private Bitmap generateBitmap(InputStreamWrapper bis, int sampleSize) {
        	Bitmap bitmap = null;
        	
        	try {
        		BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sampleSize;
                options.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
                options.inScaled = true;
                bitmap = BitmapFactory.decodeStream(bis, null, options);
                bis.close();
        	}
        	catch (Exception e) {
				e.printStackTrace();
				if (bitmap != null && bitmap.isRecycled()) {
					bitmap.recycle();
					bitmap = null;
				}
				
				return generateBitmap(bis, sampleSize*2);
			}
        	catch (Error e) {
        		System.gc();
				e.printStackTrace();
				if (bitmap != null && bitmap.isRecycled()) {
					bitmap.recycle();
					bitmap = null;
				}
				
				return generateBitmap(bis, sampleSize*2);
			}
        	
        	return bitmap;
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
        	try {
	        	if (bitmap == null) {
        			mImageView.setScaleType(ScaleType.CENTER);
            		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.no_photo);
	        	}
	        	else {
	        		mImageView.setScaleType(ScaleType.MATRIX);
	        	}
	        	
	            mImageView.setImageBitmap(bitmap);
        	}
        	catch (Exception e) {
				e.printStackTrace();
				if (bitmap != null && bitmap.isRecycled()) {
					bitmap.recycle();
					bitmap = null;
				}
			}
        	catch (Error e) {
        		System.gc();
				e.printStackTrace();
				if (bitmap != null && bitmap.isRecycled()) {
					bitmap.recycle();
					bitmap = null;
				}
			}
        	
        	mImageView.setVisibility(VISIBLE);
            mProgressBar.setVisibility(GONE);
        }

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			mProgressBar.setProgress(values[0]);
		}
    }
}
