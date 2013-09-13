package com.honey.cc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PaintActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		graphicobjects = new ArrayList<Shape>();
	}

	@Override
	public void onResume() {
		super.onResume();
		Display display = getWindowManager().getDefaultDisplay();
		Rect rectDisplay = new Rect();
		display.getRectSize(rectDisplay);
		display_width= rectDisplay.width();
		display_height = rectDisplay.height();
		
		mBitmap = Bitmap.createBitmap(display_width, display_height,
				Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		myView = new MyView(this);
		setContentView(myView);
	}
	
	@Override
    public void onStop(){
    	super.onStop();
    	myView.surfaceDestroyed(myView.getHolder());
    }
    
	@Override
    public void onDestroy(){
    	super.onDestroy();
    	myView.surfaceDestroyed(myView.getHolder());
		graphicobjects.clear();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.clear();
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemReset: {
			myView.clean();
			break;
		}
		case R.id.itemSave: {
			if(mBitmap != null)
				saveImage(Environment.getExternalStorageDirectory() + "/Paint.png", mBitmap);
			break;
		}
		}

		return true;
	}

	private MyView myView;
	private Canvas mCanvas;
	private Bitmap mBitmap;
	private Paint mBitmapPaint;
	private int display_width;
	private int display_height;

	private ArrayList<Shape> graphicobjects;
	private Shape currentDraw;

	class MyView extends SurfaceView implements SurfaceHolder.Callback {

		public DrawThread _thread;
		private Path mPath;
		private Paint mPaint;
		private Path mPath1;
		private Paint mPaint1;
		boolean clean = false;

		public void clean() {
			clean = true;
			
			graphicobjects.clear();
			mBitmap = Bitmap.createBitmap(display_width, display_height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);			
		}


		public MyView(Context context) {
			super(context);
			getHolder().addCallback(this);
			_thread = new DrawThread(getHolder(), this);
			setFocusable(true);
			setDrawingCacheEnabled(true);

			mPath = new Path();

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setColor(0xFFFF0000);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(2);

			mPath1 = new Path();
			mPaint1 = new Paint();
			mPaint1.setAntiAlias(true);
			mPaint1.setDither(true);
			mPaint1.setColor(0xFFFF1111);
			mPaint1.setStyle(Paint.Style.STROKE);
			mPaint1.setStrokeJoin(Paint.Join.ROUND);
			mPaint1.setStrokeCap(Paint.Cap.ROUND);
			mPaint1.setStrokeWidth(2);
		}

		private float mX, mY;
		private float mX1, mY1;

		private static final float TOUCH_TOLERANCE = 4;

		private float Rx(float x) {
			if (display_width / 2 > x)
				return x + (display_width / 2 - x) * 2;
			return x - (x - display_width / 2) * 2;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			synchronized (_thread.getSurfaceHolder()) {
				float x = event.getX();
				float y = event.getY();

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				{
					mPath.reset();
					mPath.moveTo(x, y);
					mX = x;
					mY = y;
					
					mPath1.reset();
					mPath1.moveTo(Rx(x), y);
					mX1 = Rx(x);
					mY1 = y;
				}
					break;
				case MotionEvent.ACTION_MOVE:
				{
					float dx = Math.abs(x - mX);
					float dy = Math.abs(y - mY);
					if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
						mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
						mX = x;
						mY = y;
					}
					
					float dx1 = Math.abs(Rx(x) - mX1);
					float dy1 = Math.abs(y - mY1);
					if (dx1 >= TOUCH_TOLERANCE || dy1 >= TOUCH_TOLERANCE) {
						mPath1.quadTo(mX1, mY1, (Rx(x) + mX1) / 2, (y + mY1) / 2);
						mX1 = Rx(x);
						mY1 = y;
					}
				}
					break;
				case MotionEvent.ACTION_UP:
				{
					mPath.lineTo(mX, mY);
					mCanvas.drawPath(mPath, mPaint);
					mPath.reset();

					mPath1.lineTo(mX1, mY1);
					mCanvas.drawPath(mPath1, mPaint1);
					mPath1.reset();

					currentDraw = new Shape();
					currentDraw.getGraphicsPath().add(mPath);
					currentDraw.getGraphicsPath().add(mPath1);
					graphicobjects.add(currentDraw);
				}
					break;
				}
				return true;
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			_thread.setRunning(true);
			_thread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// simply copied from sample application LunarLander:
			// we have to tell thread to shut down & wait for it to finish, or
			// else
			// it might touch the Surface after we return and explode
			boolean retry = true;
			_thread.setRunning(false);
			while (retry) {
				try {
					_thread.join();
					retry = false;
				} catch (InterruptedException e) {
					// we will try it again and again...
				}
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			canvas.drawColor(0xFFFFFFFF);
			for (int i = 0; i < graphicobjects.size(); i++) {

				Shape currentGraphicObject = graphicobjects.get(i);

				for (Path path : currentGraphicObject.getGraphicsPath()) {
					canvas.drawPath(path, mPaint);
					canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
					mCanvas.drawPath(path, mPaint);
					currentGraphicObject = null;
				}
			}		
		}

	}

	class DrawThread extends Thread {
		private SurfaceHolder _surfaceHolder;
		private MyView _panel;
		private boolean _run = false;

		public DrawThread(SurfaceHolder surfaceHolder, MyView panel) {
			_surfaceHolder = surfaceHolder;
			_panel = panel;
		}

		public void setRunning(boolean run) {
			_run = run;
		}

		public SurfaceHolder getSurfaceHolder() {
			return _surfaceHolder;
		}

		@Override
		public void run() {
			Canvas c;
			while (_run) {
				c = null;
				try {
					sleep(30);
					// c = _surfaceHolder.lockCanvas(null);
					// /Som+++
					c = _surfaceHolder.lockCanvas();
					// /++Som
					synchronized (_surfaceHolder) {
						_panel.onDraw(c);

					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						_surfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}
	}

	/**
	 * save images
     */
    public static void saveImage(String imagePath, Bitmap bm) {

        if (bm == null || imagePath == null || "".equals(imagePath)) {
            return;
        }

        File f = new File(imagePath);
        if (f.exists()) {
            return;
        } else {
            try {
                File parentFile = f.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                f.createNewFile();
                FileOutputStream fos;
                fos = new FileOutputStream(f);
                bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                f.delete();
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                f.delete();
            }
        }
    }
}
