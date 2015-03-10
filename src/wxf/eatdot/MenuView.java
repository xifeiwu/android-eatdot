package wxf.eatdot;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MenuView extends SurfaceView implements Callback{
	private SurfaceHolder sfh;
	private Canvas canvas;
	private Paint paint;

	private Bitmap menubg;
	private Bitmap[] btnBmps;
	private int[][] btnPos;
	private int btnW, btnH;
	public MenuView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		sfh = this.getHolder();
		sfh.addCallback(this);
		paint = new Paint();
		
		Resources res = this.getResources();
		menubg = BitmapFactory.decodeResource(res, R.drawable.menubg);
		btnBmps = new Bitmap[8];
		btnPos = new int[4][2];
		btnBmps[0] = BitmapFactory.decodeResource(res, R.drawable.start);
		btnBmps[1] = BitmapFactory.decodeResource(res, R.drawable.set);
		btnBmps[2] = BitmapFactory.decodeResource(res, R.drawable.score);
		btnBmps[3] = BitmapFactory.decodeResource(res, R.drawable.about);
		btnBmps[4] = BitmapFactory.decodeResource(res, R.drawable.start_pressed);
		btnBmps[5] = BitmapFactory.decodeResource(res, R.drawable.set_pressed);
		btnBmps[6] = BitmapFactory.decodeResource(res, R.drawable.score_pressed);
		btnBmps[7] = BitmapFactory.decodeResource(res, R.drawable.about_pressed);
		btnW = btnBmps[0].getWidth();
		btnH = btnBmps[0].getHeight();		
	}
	
	private int di;
	public void myDraw(){
		canvas = sfh.lockCanvas();
//		canvas.drawColor(Color.BLACK);
		canvas.drawBitmap(menubg, 0, 0, paint);
		for(di = 0; di < btnPos.length; di++){
			if(selectedKey == di){
				canvas.drawBitmap(btnBmps[di + btnPos.length], btnPos[di][0], btnPos[di][1], paint);
			}else{
				canvas.drawBitmap(btnBmps[di], btnPos[di][0], btnPos[di][1], paint);
			}
		}
		sfh.unlockCanvasAndPost(canvas);
	}

	private final byte NONE = -1, START = 0, SET = 1, SCORE = 2, ABOUT = 3;
	private byte selectedKey = NONE;
	private float eventX, eventY;
	private int eventAction;
	private byte ti;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		eventX = event.getX();
		eventY = event.getY();
		eventAction = event.getAction();
		switch(eventAction){
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			selectedKey = NONE;
			for(ti = 0; ti < btnPos.length; ti++){
				if((eventX > btnPos[ti][0]) && (eventX < (btnPos[ti][0] + btnW)) 
				&& (eventY > btnPos[ti][1]) && (eventY < (btnPos[ti][1] + btnH))){
					selectedKey = ti;
				}
			}
			myDraw();
			break;
		case MotionEvent.ACTION_UP:
			for(ti = 0; ti < btnPos.length; ti++){
				if((eventX > btnPos[ti][0]) && (eventX < (btnPos[ti][0] + btnW)) 
				&& (eventY > btnPos[ti][1]) && (eventY < (btnPos[ti][1] + btnH))){
					if(selectedKey == ti){
						switch(selectedKey){
						case START:
							EATDOTActivity.instance.showGameView();
							break;
						case SET:
							EATDOTActivity.instance.showSetView();
//							EATDOTActivity.instance.onDestroy();
//							System.exit(0);
							break;			
						case SCORE:
							EATDOTActivity.instance.showScoreView();
							break;
						case ABOUT:
							EATDOTActivity.instance.showAboutView();
							break;				
						}
					}
					break;
				}else{
					myDraw();
				}
			}
			selectedKey = NONE;
			break;
		}
		return true;
	}
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}
	private int screenW, screenH;
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		screenW = this.getWidth();
		screenH = this.getHeight();
		EATDOTActivity.screenW = screenW;
		EATDOTActivity.screenH = screenH;
		int interLen;
		if((screenW > 360) && (screenH > 640)){
			EATDOTActivity.screenType = EATDOTActivity.WVGA;
			interLen = 10;
		}else{
			EATDOTActivity.screenType = EATDOTActivity.HVGA;
			interLen = 10;
		}
//		EATDOTActivity.screenType = EATDOTActivity.HVGA;
		
		btnPos[0][0] = (screenW - btnW) / 2;
		btnPos[0][1] = screenH / 2 + interLen * 2;
		btnPos[1][0] = (screenW - btnW) / 2;
		btnPos[1][1] = btnPos[0][1] + btnH + interLen;
		btnPos[2][0] = (screenW - btnW) / 2;
		btnPos[2][1] = btnPos[1][1] + btnH + interLen;
		btnPos[3][0] = (screenW - btnW) / 2;
		btnPos[3][1] = btnPos[2][1] + btnH + interLen;
		
		myDraw();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

}
