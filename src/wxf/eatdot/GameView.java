package wxf.eatdot;

import java.util.Calendar;

import android.app.Service;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.GestureDetector.OnGestureListener;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Callback, Runnable, OnGestureListener, SensorEventListener{
	private SurfaceHolder sfh;
	private Canvas canvas;
	private Paint paint;
	
	private Eater eater;
	private Sprite[] sprites;
	private GestureDetector detector;	

	private SensorManager sm;
	private Sensor sensor;
	
	//eater处在power状态多长时间，放在这里是为了在eater和sprite中统一时间。
	public static int POWERTIMECNT = (int) (15000 / GameView.MILLISPERSECOND);
	public GameView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		sfh = this.getHolder();
		sfh.addCallback(this);
		paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		paint.setTextSize(15);
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		switch(EATDOTActivity.controlType){
		case EATDOTActivity.TOUCH:
			detector = new GestureDetector(this);
			break;
		case EATDOTActivity.SENSOR:
			sm = (SensorManager) EATDOTActivity.instance.getSystemService(Service.SENSOR_SERVICE);
			//实例一个重力传感器实例  
			sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			//为传感器注册监听器
			sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
			break;
		}
		
		//取得程序中需要使用的bitmap和canvas
		getBmps();
		newGame();
	}
	private int spritesNum = 3;
	public void newGame(){
		resetMap();
		rePaintMaze();
		
		eater = new Eater(this);
		rePaintTipBar();//需要在eater初始化之后调用。
		Sprite.getBmps();
		sprites = new Sprite[4];
		sprites[0] = new Sprite(this, Sprite.Blinky);
		sprites[1] = new Sprite(this, Sprite.Clyde);
		sprites[2] = new Sprite(this, Sprite.Inky);
		sprites[3] = new Sprite(this, Sprite.Pinky);
		spritesNum = 3;
//		gameState = READY;
		stateChangedTo(READY);
		if(EATDOTActivity.isSoundOn){
			EATDOTActivity.sp.play(EATDOTActivity.pacstartgame, 1, 1, 1, 0, 1);
		}
	}
	/**
	 * 地图的绘制与Eater和Sprite的绘制分开，减少地图绘制是次数；
	 * 豆子比较多，绘制比较耗费资源，所以maze图像改变的时候（在Eater吃掉豆子之后）才会调用rePaintMaze()方法绘制。
	 * 
	 */
	private int di, stickPadCnt = 0;
//	private boolean isNeedDrawStickPad = true;
	private int[][] power_pellet_pos = {{1, 3}, {26, 3}, {1, 23}, {26, 23}};
	private String gameOverTip;
	private Matrix matrix;
	public void myDraw(){
		canvas = sfh.lockCanvas();
		gameCanvas.drawColor(Color.BLACK);
		//绘制成绩栏。
		if((gameState == GAMEOVER) || (gameState == TOUCHTOMENUVIEW)){
			gameCanvas.drawText(gameOverTip, mazeX, mazeY - 5, paint);
		}else{
			gameCanvas.drawText("成绩：" + eater.scores, mazeX, mazeY - 5, paint);
			gameCanvas.drawText("第" + (levelCnt + 1) + "关", mazeW - 80, mazeY - 5, paint);
//			gameCanvas.drawText(during + "", mazeW / 2, mazeY - 5, paint);
		}
		//绘制maze，包括豆豆和提示。
		gameCanvas.drawBitmap(maze_blue_mutable, mazeX, mazeY, paint);
		if(logicCnt % 2 == 0){
			pelletCnt++;
		}
		for(di = 0; di < 4; di++){
			if((map[power_pellet_pos[di][1]][power_pellet_pos[di][0] + 1] == 2)){
				mazeCanvas.drawBitmap(attract_power_pellet[pelletCnt % 6], power_pellet_pos[di][0] * 11, power_pellet_pos[di][1] * 11, paint);
			}
		}
		switch(gameState){
		case READY:
			gameCanvas.drawBitmap(readyBmp, (mazeW - readyBmp.getWidth()) / 2, 207, paint);
			break;
		case GAMEOVER:
		case TOUCHTOMENUVIEW:
			gameCanvas.drawBitmap(gameOverBmp, (mazeW - gameOverBmp.getWidth()) / 2, 207, paint);
		case EATERDIE:
			eater.myDraw(gameCanvas, paint);
			break;
		case NEXTLEVEL:
			gameCanvas.drawBitmap(nextLevelBmp, (mazeW - nextLevelBmp.getWidth()) / 2, 207, paint);
			eater.myDraw(gameCanvas, paint);
			break;
		case PAUSE:
			gameCanvas.drawBitmap(pauseBmp, (mazeW - pauseBmp.getWidth()) / 2, 207, paint);
		case PLAYING:
		case BOUNCE:
			//绘制eater和sprites。
			eater.myDraw(gameCanvas, paint);
			for(di = 0; di < spritesNum; di++){
				sprites[di].myDraw(gameCanvas, paint);
			}
			break;
		}
		//提示栏：剩余生命和当前关卡对应的fruit
		gameCanvas.drawBitmap(tipBarBmp, 0, 20 + mazeH, paint);
		
		//双缓冲。
		if(EATDOTActivity.screenType == EATDOTActivity.HVGA){
			canvas.drawBitmap(gameBmp, gameX, gameY, paint);
		}else{//720
//			canvas.drawBitmap(Bitmap.createScaledBitmap(gameBmp, WVGA_W, WVGA_H, true), gameX, gameY, paint);
			canvas.drawBitmap(gameBmp, matrix, paint);
		}
		
		//绘制方向面板。
		switch(EATDOTActivity.controlType){
		case EATDOTActivity.TOUCH:
			if(stickPadCnt < one_second){
				canvas.drawBitmap(stickPad, stickpadX, stickdirY, paint);
				canvas.drawBitmap(stickdir[eater.wantDir], stickdirX, stickdirY, paint);
			}else{
				canvas.drawBitmap(stickPad, stickpadX, stickdirY, paint);
				canvas.drawBitmap(stickdir[0], stickdirX, stickdirY, paint);
			}
			stickPadCnt++;
//			Log.v("drawStickPad", "stickPadCnt:" + stickPadCnt);
			break;
		case EATDOTActivity.SENSOR:
			canvas.drawBitmap(stickPad, stickpadX, stickdirY, paint);
			canvas.drawBitmap(stickdir[eater.wantDir], stickdirX, stickdirY, paint);
			break;
		}
		sfh.unlockCanvasAndPost(canvas);
	}
	private int li, logicCnt, bounceCnt = 0, levelCnt = 0, pelletCnt;
	private int one_second = (int) (1000 / MILLISPERSECOND);
	private int three_seconds = (int) (3000 / MILLISPERSECOND);
	private int thirty_seconds = (int) (30000 / MILLISPERSECOND);
	private int fifty_seconds = (int) (50000 / MILLISPERSECOND);
	private void logic(){
		switch(gameState){
		case READY:
//			Log.v("GameView-logic", "state:PLAYING");
//			eater.logic();		
//			for(di = 0; di < sprites.length; di++){
//				sprites[di].logic(eater);
//			}
			if(logicCnt > three_seconds){
				stateChangedTo(PLAYING);
			}
			break;			
		case PLAYING:
		case BOUNCE:
//			Log.v("GameView-logic", "state:PLAYING");
			eater.logic();		
			for(di = 0; di < spritesNum; di++){
				sprites[di].logic(eater);
				if(sprites[di].isCollisionWith(eater)){
					if((sprites[di].state == Sprite.WEAK)){//(eater.state == Eater.POWER) && 
						eater.addScore(Sprite.scoreValue[Sprite.scoreCnt]);
						sprites[di].stateChangeTo(Sprite.DEAD);
						if(EATDOTActivity.isSoundOn){
							EATDOTActivity.sp.play(EATDOTActivity.paceatghost, 1, 1, 1, 0, 1);
						}
//						Log.v("GameView-Logic", "di:" + di + "---" + sprites[di].state);
					}else if(sprites[di].state == Sprite.GO && (gameState != NEXTLEVEL)){
						//gameState != NEXTLEVEL是为了防止豆子全吃完后在改变状态。
						eater.stateChangeTo(Eater.DEAD);
						eater.lives--;
						if(eater.lives < 0){
//							gameState = ;
							stateChangedTo(GAMEOVER);
						}else{
							rePaintTipBar();
							stateChangedTo(EATERDIE);
//							gameState = ;
						}
						logicCnt = 0;
//						eater.resetParam();
					}
					break;
				}
			}
			bounceCnt++;
			if((gameState == PLAYING) && (bounceCnt > thirty_seconds)){//20000logicCnt
				gameState = BOUNCE;
				map[17][14] = 3;
				rePaintMaze();
//				Log.v("gameState:" + gameState, "state changge to" + gameState);
			}else if((gameState == BOUNCE) && (bounceCnt > fifty_seconds)){//25000
				gameState = PLAYING;
				map[17][14] = 5;
				bounceCnt = 0;
				rePaintMaze();
//				Log.v("gameState:" + gameState, "state changge to" + gameState);
			}
//			Log.v("gameState:" + gameState, "bounceCnt:" + bounceCnt);
			break;
		case NEXTLEVEL:
			if(logicCnt > three_seconds){
				eater.stateChangeTo(Eater.READY);
				for(li = 0; li < spritesNum; li++){
					sprites[li].stateChangeTo(Sprite.READY);
				}
				resetMap();
				rePaintMaze();
				stateChangedTo(READY);
//				gameState = READY;
//				Log.v("GameView-logic", "state:" + gameState);
			}
			break;
		case EATERDIE:
			if(logicCnt > three_seconds){
				for(li = 0; li < spritesNum; li++){
					sprites[li].stateChangeTo(Sprite.READY);
				}
				eater.stateChangeTo(Eater.READY);
				stateChangedTo(READY);
//				gameState = READY;
			}
			break;
		case PAUSE:
			break;
		case GAMEOVER:
//			eater.logic();		
//			for(di = 0; di < sprites.length; di++){
//				sprites[di].logic(eater);
//			}	
			if(logicCnt > three_seconds){
				gameState = TOUCHTOMENUVIEW;
//				gameState = PAUSE;
//				Calendar calendar = Calendar.getInstance();
//				int year = calendar.get(Calendar.YEAR);
//				int month = calendar.get(Calendar.MONTH);
//				int day = calendar.get(Calendar.DAY_OF_MONTH);
//				int hour = calendar.get(Calendar.HOUR_OF_DAY);
//				int minutes = calendar.get(Calendar.MINUTE);
//				String fileName = year + "年" + (month + 1) + "月" + day + "日" + hour
//						+ "点" + minutes + "分";
//				int rank = EATDOTActivity.instance.setScore(eater.scores, fileName);
//				gameState = GAMEOVER;
//				String dialogMsg = "共耗时：" + eater.scores + "秒，成绩榜排名为：第" + rank + "名，去看看成绩榜？";
				
//				Builder builder = new Builder(EATDOTActivity.instance);
//				builder.setIcon(android.R.drawable.ic_dialog_info);
//				builder.setTitle("游戏结束！");
//				builder.setMessage(dialogMsg);
//				builder.setPositiveButton("确定",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int which) {
//								// TODO Auto-generated method stub
//								EATDOTActivity.instance.showScoreView();
//							}
//
//						});
//				builder.setNegativeButton("再来一局",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int which) {
//								// TODO Auto-generated method stub
//								newGame();
//							}
//						});
//				builder.show();
//				EATDOTActivity.instance.showMenuView();
			}
			break;
		}
		logicCnt++;
	}
	public static final int READY = 0, PLAYING = 1, BOUNCE = 2, EATERDIE = 3, 
			PAUSE = 4, NEXTLEVEL = 5, GAMEOVER = 6, TOUCHTOMENUVIEW = 7;
	public int gameState;
	private int csi;
	public void changeStateBy(Eater eater){
		switch(eater.state){
//		case Eater.GO:
//			gameState = PLAYING;
//			break;
		case Eater.POWER:
			for(csi = 0; csi < spritesNum; csi++){
				//在死亡后或还未出屋之前，不能改变状态。
				if((sprites[csi].state != Sprite.READY) && (sprites[csi].state != Sprite.DEAD)
						 && (sprites[csi].state != Sprite.COMEOUT)){
					sprites[csi].stateChangeTo(Sprite.WEAK);
				}
			}
			break;
		case Eater.FULL:
			stateChangedTo(NEXTLEVEL);
			EATDOTActivity.stopSoundPool();
			levelCnt++;
			if(levelCnt > 3){
				spritesNum = 4;
			}
			logicCnt = 0;
			bounceCnt = 0;
			break;
//			Log.v("GameView-changeState:", "come here");
		}
	}
	public void stateChangedTo(int s){
		switch(s){
		case EATERDIE:
    		if(EATDOTActivity.isSoundOn){
    			EATDOTActivity.sp.play(EATDOTActivity.pacdie, 1, 1, 1, 0, 1);
    		}
		case READY:
			logicCnt = 0;
//			break;
		case PLAYING:
		case NEXTLEVEL:
			map[17][14] = 5;
			bounceCnt = 0;
			rePaintMaze();
			rePaintTipBar();
			break;
		case GAMEOVER:
		case TOUCHTOMENUVIEW:
    		if(EATDOTActivity.isSoundOn){
    			EATDOTActivity.sp.play(EATDOTActivity.pacdie, 1, 1, 1, 0, 1);
    		}    		
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH);
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minutes = calendar.get(Calendar.MINUTE);
			String fileName = year + "年" + (month + 1) + "月" + day + "日" + hour
					+ "点" + minutes + "分";
			int rank = EATDOTActivity.instance.setScore(eater.scores, fileName);
			gameOverTip = "您的成绩是：" + eater.scores + "分，成绩排名第" + rank +"名。";
			map[17][14] = 5;
			bounceCnt = 0;
			rePaintMaze();
			break;
		}
		gameState = s;
	}

	private int eaterDir;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		switch(gameState){
		case PAUSE:
			stateChangedTo(PLAYING);
			break;
		case TOUCHTOMENUVIEW:
			EATDOTActivity.instance.showMenuView();
			break;
		}
		if(EATDOTActivity.controlType == EATDOTActivity.TOUCH){
			return detector.onTouchEvent(event);
		}else{
			return true;
		}
	}
//	private int screenW, screenH;
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
//		screenW = this.getWidth();
//		screenH = this.getHeight();
//		EATDOTActivity.screenW = screenW;
//		EATDOTActivity.screenH = screenH;		
		flag = true;
		th = new Thread(this);
		th.start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		flag = false;
		EATDOTActivity.stopSoundPool();
		if((gameState != GAMEOVER) && (gameState != TOUCHTOMENUVIEW)){    		
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH);
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minutes = calendar.get(Calendar.MINUTE);
			String fileName = year + "年" + (month + 1) + "月" + day + "日" + hour
					+ "点" + minutes + "分";
			EATDOTActivity.instance.setScore(eater.scores, fileName);
		}
		//防止声音无限循环
//		eater.stateChangeTo(Eater.DEAD);
//		for(di = 0; di < sprites.length; di++){
//			sprites[di].stateChangeTo(Sprite.READY);
//		}
	}
	private Thread th;
	public boolean flag;
	private long start, during;
	public static final long MILLISPERSECOND = 80;
	public void run() {
		// TODO Auto-generated method stub
		while (flag) {
			start = System.currentTimeMillis();
			logic();
			myDraw();
			during = System.currentTimeMillis() - start;
			if (during < MILLISPERSECOND) {
				try {
					Thread.sleep(MILLISPERSECOND - during);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	private float WVGA_W = 462, WVGA_H = 570;//462,570;385,475
	//地图及地图上的豆子
	private Bitmap maze_blue, maze_blue_mutable, pellet, stickPad, gameBmp;
	private Bitmap readyBmp, nextLevelBmp, gameOverBmp, pauseBmp;
	private Bitmap[] attract_power_pellet, stickdir, fruits, fruits_big;
	private Canvas mazeCanvas, gameCanvas, tipBarCanvas;
	public static int mazeX, mazeY, mazeW, mazeH;
	public int gameX, gameY, gameW, gameH, stickpadX, stickdirX, stickdirY;
	private Bitmap extralLifeBmp, tipBarBmp;
	private void getBmps(){
		Resources res = this.getResources();
		maze_blue = BitmapFactory.decodeResource(res, R.drawable.maze_blue);
		mazeW = maze_blue.getWidth();
		mazeH = maze_blue.getHeight();
		maze_blue_mutable = Bitmap.createBitmap(mazeW, mazeH, Bitmap.Config.ARGB_8888);
		mazeCanvas = new Canvas(maze_blue_mutable);
		mazeX = 0;//(EATDOTActivity.screenW - mazeW) / 2;
		mazeY = 20;//(int) (paint.getTextSize() + 10);
		pellet = BitmapFactory.decodeResource(res, R.drawable.pellet);
		Bitmap tmpBmp;
		int h;
		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.attract_power_pellet);
		h = tmpBmp.getHeight();
		attract_power_pellet = new Bitmap[6];
		attract_power_pellet[0] = Bitmap.createBitmap(tmpBmp, 0, 0, h, h);
		attract_power_pellet[1] = Bitmap.createBitmap(tmpBmp, h, 0, h, h);
		attract_power_pellet[2] = Bitmap.createBitmap(tmpBmp, h * 2, 0, h, h);
		attract_power_pellet[3] = Bitmap.createBitmap(tmpBmp, h * 3, 0, h, h);
		attract_power_pellet[4] = Bitmap.createBitmap(tmpBmp, h * 4, 0, h, h);
		attract_power_pellet[5] = Bitmap.createBitmap(tmpBmp, h * 5, 0, h, h);
		
		stickPad = BitmapFactory.decodeResource(res, R.drawable.gad_img_hud_bg);
		stickdir = new Bitmap[5];
		stickdir[0] = BitmapFactory.decodeResource(res, R.drawable.joystickreg);
		stickdir[1] = BitmapFactory.decodeResource(res, R.drawable.joystickup);
		stickdir[2] = BitmapFactory.decodeResource(res, R.drawable.joystickdown);
		stickdir[3] = BitmapFactory.decodeResource(res, R.drawable.joystickleft);
		stickdir[4] = BitmapFactory.decodeResource(res, R.drawable.joystickright);
		
		tipBarBmp = Bitmap.createBitmap(mazeW, 20, Bitmap.Config.ARGB_8888);
		tipBarCanvas = new Canvas(tipBarBmp);
		extralLifeBmp = BitmapFactory.decodeResource(res, R.drawable.extral_life);
		fruits_big = new Bitmap[4];
		fruits_big[0] = BitmapFactory.decodeResource(res, R.drawable.cherry_big);
		fruits_big[1] = BitmapFactory.decodeResource(res, R.drawable.strawberry_big);
		fruits_big[2] = BitmapFactory.decodeResource(res, R.drawable.grapes_big);
		fruits_big[3] = BitmapFactory.decodeResource(res, R.drawable.peach_big);
		
		gameBmp = Bitmap.createBitmap(mazeW, 20 + mazeH + 20, Bitmap.Config.ARGB_8888);
		// + stickPad.getHeight()
		gameCanvas = new Canvas(gameBmp);
		switch(EATDOTActivity.screenType){
		case EATDOTActivity.HVGA:
			gameX = (EATDOTActivity.screenW - mazeW) / 2;
			gameY = 0;//(int) (paint.getTextSize() + 10);
			stickpadX = (EATDOTActivity.screenW - stickPad.getWidth()) / 2;
			stickdirX = (EATDOTActivity.screenW - stickdir[0].getWidth()) / 2;
			stickdirY = 20 + mazeH + 20;//20 + mazeH + 20;
			break;
		case EATDOTActivity.WVGA:
			gameX = (int) ((EATDOTActivity.screenW - WVGA_W) / 2);
			gameY = 0;//(int) (paint.getTextSize() + 10);
			stickpadX = (EATDOTActivity.screenW - stickPad.getWidth()) / 2;
			stickdirX = (EATDOTActivity.screenW - stickdir[0].getWidth()) / 2;
			stickdirY = (int) WVGA_H;//20 + mazeH + 20;
			matrix = new Matrix();
			matrix.postScale(WVGA_W / mazeW, WVGA_H / (20 + mazeH + 20));//2, 2
			matrix.postTranslate(gameX, gameY);
			break;
		}
		
		readyBmp = BitmapFactory.decodeResource(res, R.drawable.ready);
		nextLevelBmp = BitmapFactory.decodeResource(res, R.drawable.nextlevel);
		gameOverBmp = BitmapFactory.decodeResource(res, R.drawable.gameover);
		pauseBmp = BitmapFactory.decodeResource(res, R.drawable.pause);
		
		fruits = new Bitmap[4];
		fruits[0] = BitmapFactory.decodeResource(res, R.drawable.cherry);
		fruits[1] = BitmapFactory.decodeResource(res, R.drawable.strawberry);
		fruits[2] = BitmapFactory.decodeResource(res, R.drawable.grapes);
		fruits[3] = BitmapFactory.decodeResource(res, R.drawable.peach);
	}
	private int rex, rey;	
	public void rePaintMaze(){
		mazeCanvas.drawBitmap(maze_blue, 0, 0, paint);
		for(rey = 0; rey < map.length; rey++){
			for(rex = 0; rex < map[0].length - 2; rex++){
				switch(map[rey][rex + 1]){
				case 1:
					mazeCanvas.drawBitmap(pellet, rex * 11 + 5, rey * 11 + 5, paint);
					break;
//				case 2:
//					mazeCanvas.drawBitmap(power_pellet, rex * 11, rey * 11, paint);
//					break;
				case 3://pos:17*14
					mazeCanvas.drawBitmap(fruits[levelCnt % fruits.length], rex * 11, rey * 11, paint);
//					mazeCanvas.drawBitmap(power_pellet, rex * 11, rey * 11, paint);
					break;
				}
			}
		}
	}
	private int ri;
	public void rePaintTipBar(){
		tipBarCanvas.drawColor(Color.BLACK);
		for(ri = 0; ri < eater.lives; ri++){
			tipBarCanvas.drawBitmap(extralLifeBmp, ri * extralLifeBmp.getWidth() , 2, paint);
		}
		tipBarCanvas.drawBitmap(fruits_big[levelCnt % fruits_big.length], mazeW - fruits_big[0].getWidth(), 0, paint);
	}
	public void resetMap(){
		map = new int[map_data.length][map_data[0].length];
		for(int ry = 0; ry < map_data.length; ry++){
			for(int rx = 0; rx < map_data[0].length; rx++){
				map[ry][rx] = map_data[ry][rx];
			}
		}
	}
	//1：普通豆；2：超级豆；3：奖励；4：；5：为空
	public static int[][] map;
    public static int[][] map_data = {//31*30，新增加两列。29 - 26
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8},
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8},
            {8, 8, 2, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 2, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 1, 1, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 0, 8, 8, 0, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 0, 8, 8, 0, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8},
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 0, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 1, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 2, 1, 1, 8, 8, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 2, 8, 8},
            {8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8}, 
            {8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 8, 8, 1, 1, 1, 1, 1, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8, 1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1, 8, 8}, 
            {8, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8}, 
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8}
        };
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub		
	}
	/**不同分辨率手机适配
	 * 1.pellet的位置
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
//		Log.v("GameView", "onFling");
		if(Math.abs(velocityY) > Math.abs(velocityX)){
			if(velocityY > 0){
				eaterDir = Eater.DOWN;
			}else{
				eaterDir = Eater.UP;
			}
		}else{
			if(velocityX > 0){
				eaterDir = Eater.RIGHT;
			}else{
				eaterDir = Eater.LEFT;
			}			
		}
		eater.setDir(eaterDir);	
		stickPadCnt = 0;			
		return false;
	}
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	private float sx, sy;
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		sx = event.values[0]; 
		//x>0 说明当前手机左翻 x<0右翻       
		sy = event.values[1];
		//y>0 说明当前手机下翻 y<0上翻  
		if(Math.abs(sy) > Math.abs(sx)){
			if(sy > 0){
				eaterDir = Eater.DOWN;
			}else{
				eaterDir = Eater.UP;
			}
		}else{
			if(sx > 0){
				eaterDir = Eater.LEFT;
			}else{
				eaterDir = Eater.RIGHT;
			}			
		}
		eater.setDir(eaterDir);
	}

//	//实例传感器监听器
//	mySensorListener = new SensorEventListener() {
//		@Override
//		//传感器获取值发生改变时在响应此函数  
//		public void onSensorChanged(SensorEvent event) {
//			float x, y;
//			x = event.values[0]; 
//			//x>0 说明当前手机左翻 x<0右翻       
//			y = event.values[1];
//			//y>0 说明当前手机下翻 y<0上翻  
//		}
//		@Override
//		//传感器的精度发生改变时响应此函数  
//		public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		}
//	};
}
