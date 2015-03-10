package wxf.eatdot;

import java.util.Random;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class Eater {
	public static final int READY = 1, GO = 2, POWER = 3, BOUNCE = 4, FULL = 5, DEAD = 6;
	public int state;

	public int pX, pY, mapX, mapY, eaterSize;

	public static final int NONE = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4;
	public int pDir, wantDir;
	final int dx[] = { // Eater������Develop Men��x��������ı仯
	0, 0, 0, -1, 1 };
	final int dy[] = { // Eater������Develop Men��y���ϵ�����ı仯
	0, -1, 1, 0, 0 //
	};
	final int[] speeds = {4, 3, 4};
	
	private int logicCnt, drawCnt, speedCnt, bounceCnt;//, soundCnt;
	private GameView gameView;
	public int dots, scores, lives;	
	private Random rand;

	public Eater(GameView view) {
		gameView = view;
		getBmps();
		scores = 0;
		lives = 2;//3;
		rand = new Random();
		resetParam();
	}
	private int one_second = (int) (1000 / GameView.MILLISPERSECOND);
	private int two_seconds = (int) (2000 / GameView.MILLISPERSECOND);
	public void logic() {
		switch(state){
		case READY:
			//1.5������
			if(logicCnt > one_second){
				stateChangeTo(GO);
//				gameView.changeStateBy(this);
			}
			break;
			//ע��case��˳��
		case BOUNCE:
			if(bounceCnt > two_seconds){
				state = GO;
			}
			bounceCnt++;
			stateGO();
			break;
		case POWER:
//			Log.v("Eater logicCnt:", logicCnt + ""); 
			if(logicCnt > GameView.POWERTIMECNT){
				state = GO;
	    		if(EATDOTActivity.isSoundOn && (EATDOTActivity.pacblueghostId != 0)){
	        		EATDOTActivity.sp.stop(EATDOTActivity.pacblueghostId);
	        		EATDOTActivity.pacblueghostId = 0;
//	        		Log.v("Eater:", "change from power to go!");	        		
	        		//EATDOTActivity.pacblueghost
	    		}
//        		Log.v("Eater:", "change from power to go! + out");
			}
		case GO:
		case FULL:
			stateGO();
			break;
//		case FULL:
//			if(logicCnt > 1000 / GameView.MILLISPERSECOND){
//				stateChangeTo(READY);
//			}
//			drawCnt++;
//			break;
		case DEAD:
//			if(logicCnt > 12){
//				stateChangeTo(READY);
//			}
			break;
		}
        logicCnt++;
//        Log.v("Eater state:" +state, "pX:" + pX + "-" + "pY:" + pY + "-" + "pDir:" + pDir);
	}
	private void stateGO(){
		//�������ѡ���λ��
		if((pX % 11) == 0 && (pY % 11) == 0){
			//ע�⣡��
			mapX = pX / 11 + 1;
			mapY = pY / 11;
			//����ǰλ�õ�����
//			soundCnt++;
			switch(GameView.map[mapY][mapX]){
			case 1://��ͨ������10�֡�
				dots++;
//                scores += 10;
				addScore(10);
                //��עΪ5��˵���Ѿ��߹�
                GameView.map[mapY][mapX] = 0;
                gameView.rePaintMaze();
        		if(EATDOTActivity.isSoundOn && (EATDOTActivity.paceatdotId == 0)){// && ((soundCnt % 2) == 0)
        			EATDOTActivity.paceatdotId = EATDOTActivity.sp.play(EATDOTActivity.paceatdot, 1, 1, 1, -1, 1);
        		}
				break;
			case 2://����������10�֡�
                dots++;
				addScore(10);
                GameView.map[mapY][mapX] = 0;
                //�ı�״̬
                stateChangeTo(POWER);
                //��gameView�иı�Sprite��״̬��	                
                gameView.changeStateBy(this);
                gameView.rePaintMaze();
        		if(EATDOTActivity.isSoundOn && (EATDOTActivity.paceatdotId == 0)){//  && ((soundCnt % 2) == 0)
        			EATDOTActivity.paceatdotId = EATDOTActivity.sp.play(EATDOTActivity.paceatdot, 1, 1, 1, -1, 1);
        		}
				break;
			case 3:
//				Log.v("Eater-logic:come in bounce", mapY + "*" + mapX);
				//�Ե��������������ɼ���
				bounceIndex = rand.nextInt(bounceScores.length);
//				scores += bounceScores[bounceIndex];
				addScore(bounceScores[bounceIndex]);
				//״̬�ı�ΪBOUNCE״̬����״̬������ʾ�ɼ���
				stateChangeTo(BOUNCE);
                GameView.map[mapY][mapX] = 0;
                gameView.rePaintMaze();
        		if(EATDOTActivity.isSoundOn){
        			EATDOTActivity.sp.play(EATDOTActivity.paceatfruit, 1, 1, 1, 0, 1);
        		}
				break;
			case 0:
        		if(EATDOTActivity.isSoundOn && (EATDOTActivity.paceatdotId != 0)){//  && ((soundCnt % 2) == 0)
        			EATDOTActivity.sp.stop(EATDOTActivity.paceatdotId);
        			EATDOTActivity.paceatdotId = 0;
        		}
        		break;
			}
//	        Log.v("Eater", "pX:" + pX + "-" + "pY:" + pY + "-" + "pDir:" + pDir);
//	        Log.v("Eater", "mapX:" + mapX + "-" + "mapY:" + mapY + "-" + "pDir:" + pDir);
	        //��������Ԥ������ǰ��
	        if (GameView.map[mapY + dy[wantDir]][mapX + dx[wantDir]] < 8) 
				pDir = wantDir;
			else if (GameView.map[mapY + dy[pDir]][mapX + dx[pDir]] > 7) // ���ߵ���ǽ���޷�ǰ����
				pDir = NONE;
	        //����ߵ�ǽ�ߣ�ֹͣ����������
    		if(EATDOTActivity.isSoundOn && (EATDOTActivity.paceatdotId != 0) && (pDir == NONE)){//  && ((soundCnt % 2) == 0)
    			EATDOTActivity.sp.stop(EATDOTActivity.paceatdotId);
    			EATDOTActivity.paceatdotId = 0;
    		}
    		
	        //���ݷ���ȥ��eater��ͼ��
	        switch(pDir){
	        case UP:
	        	pacBmps = pacup;        	
	        	break;
	        case DOWN:
	        	pacBmps = pacdown;  
	        	break;
	        case LEFT:
	        	pacBmps = pacleft;  
	        	break;
	        case RIGHT:
	        	pacBmps = pacright;  
	        	break;
	        }
	        //ȫ�����Թ�󣬻��ʤ����
	        if(dots == 244)	//	244								//������ȫ��С����
	        {		        	
	        	stateChangeTo(FULL);
	        	gameView.changeStateBy(this);
	        	dots = 0;
	        }
		}
        pX += speeds[speedCnt % 3] * dx[pDir];			//����ǰ���ķ���ı�Eater������ֵ
        pY += speeds[speedCnt % 3] * dy[pDir];
        //ע�����ͼ�Ķ�Ӧ
        if(pX <= 0)					//��һ�˳�ȥ�����һ�˷���
            pX += 308;
        else
        if(pX >= 308)
            pX -= 308;
        speedCnt++;
		
	}
	public void stateChangeTo(int s){
		switch(s){
		case READY:
			logicCnt = 0;
			drawCnt = 2;
			speedCnt = 2;
			pX = 147;//143
			pY = 253;
			pacBmps = pacleft;
			pacBmpSteps = pacleft.length;
			wantDir = LEFT;
			pDir = LEFT;
			break;
		case GO:
			drawCnt = 2;
			break;
		case POWER:
            logicCnt = 0;
    		if(EATDOTActivity.isSoundOn){
    			if(EATDOTActivity.pacblueghostId == 0){
    				EATDOTActivity.pacblueghostId = EATDOTActivity.sp.play(EATDOTActivity.pacblueghost, 1, 1, 1, -1, 1);
    			}
    		}

//    		Log.v("Eater:", "change to power!" + EATDOTActivity.pacblueghostId);
			break;
		case BOUNCE:
			bounceCnt = 0;
			break;
		case DEAD:
			drawCnt = 0;
//			logicCnt = 0;
			pacBmps = pacDying;
			pacBmpSteps = pacDying.length;
		case FULL:
            logicCnt = 0;
			if((EATDOTActivity.paceatdotId != 0) && EATDOTActivity.isSoundOn){//  && ((soundCnt % 2) == 0)
				EATDOTActivity.sp.stop(EATDOTActivity.paceatdotId);
				EATDOTActivity.paceatdotId = 0;
			}
			break;
		}
		if((EATDOTActivity.pacblueghostId != 0) && (s != POWER) && (s != BOUNCE) && EATDOTActivity.isSoundOn){
    		EATDOTActivity.sp.stop(EATDOTActivity.pacblueghostId);
    		EATDOTActivity.pacblueghostId = 0;
		}

		state = s;
	}
	public void resetParam(){
		stateChangeTo(READY);
		scoreCnt = 1;
		SOCRESPERLIFE = 10000;
	}
	public void myDraw(Canvas canvas, Paint paint){
		switch(state){
		case BOUNCE:
			canvas.drawBitmap(bounceScoreBmp[bounceIndex], GameView.mazeX + 154, GameView.mazeY + 187, paint);
		case READY:
		case GO:
		case POWER:
		case FULL:
			if(pDir != NONE){
			drawCnt++;
			}
			canvas.drawBitmap(pacBmps[drawCnt % pacBmpSteps], GameView.mazeX + pX - 5, GameView.mazeY + pY - 5, paint);
			break;
		case DEAD:
			logicCnt++;
			if(drawCnt < 10){
				canvas.drawBitmap(pacBmps[drawCnt % pacBmpSteps], GameView.mazeX + pX - 5, GameView.mazeY + pY - 5, paint);
			}
			if(logicCnt % 2 == 0){
				drawCnt++;
			}
			break;
		}
	}	
	public void setDir(int dir){
		wantDir = dir;
	}
	private int scoreCnt = 1, SOCRESPERLIFE = 10000;
	public void addScore(int score){
		scores += score;
		if(scores > SOCRESPERLIFE * scoreCnt){
			lives++;
			scoreCnt++;
		}
		gameView.rePaintTipBar();
	}

	public Bitmap[] pacup, pacdown, pacleft, pacright, pacBmps, pacDying, bounceScoreBmp;
	private int pacBmpSteps;
	private int[] bounceScores = {100, 300, 500, 700, 1000, 2000, 3000};
	private int bounceIndex;
	private void getBmps(){
		Resources res = gameView.getResources();
		pacup = new Bitmap[3];
		pacdown = new Bitmap[3];
		pacleft = new Bitmap[3];
		pacright = new Bitmap[3];	
		pacDying = new Bitmap[10];
		
//		BitmapFactory.Options options = null;//new BitmapFactory.Options();
//		options.inDensity = 1;
		Bitmap tmpBmp;
		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.pacup);
		eaterSize = tmpBmp.getHeight();
		pacup[0] = Bitmap.createBitmap(tmpBmp, 0, 0, eaterSize, eaterSize);
		pacup[1] = Bitmap.createBitmap(tmpBmp, eaterSize, 0, eaterSize, eaterSize);
		pacup[2] = Bitmap.createBitmap(tmpBmp, eaterSize * 2, 0, eaterSize, eaterSize);

		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.pacdown);
		pacdown[0] = Bitmap.createBitmap(tmpBmp, 0, 0, eaterSize, eaterSize);
		pacdown[1] = Bitmap.createBitmap(tmpBmp, eaterSize, 0, eaterSize, eaterSize);
		pacdown[2] = Bitmap.createBitmap(tmpBmp, eaterSize * 2, 0, eaterSize, eaterSize);

		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.pacleft);
		pacleft[0] = Bitmap.createBitmap(tmpBmp, 0, 0, eaterSize, eaterSize);
		pacleft[1] = Bitmap.createBitmap(tmpBmp, eaterSize, 0, eaterSize, eaterSize);
		pacleft[2] = Bitmap.createBitmap(tmpBmp, eaterSize * 2, 0, eaterSize, eaterSize);

		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.pacright);
		pacright[0] = Bitmap.createBitmap(tmpBmp, 0, 0, eaterSize, eaterSize);
		pacright[1] = Bitmap.createBitmap(tmpBmp, eaterSize, 0, eaterSize, eaterSize);
		pacright[2] = Bitmap.createBitmap(tmpBmp, eaterSize * 2, 0, eaterSize, eaterSize);	

		tmpBmp = BitmapFactory.decodeResource(res, R.drawable.pacdying);
		pacDying[0] = Bitmap.createBitmap(tmpBmp, 0, 0, eaterSize, eaterSize);
		pacDying[1] = Bitmap.createBitmap(tmpBmp, eaterSize, 0, eaterSize, eaterSize);
		pacDying[2] = Bitmap.createBitmap(tmpBmp, eaterSize * 2, 0, eaterSize, eaterSize);
		pacDying[3] = Bitmap.createBitmap(tmpBmp, eaterSize * 3, 0, eaterSize, eaterSize);
		pacDying[4] = Bitmap.createBitmap(tmpBmp, eaterSize * 4, 0, eaterSize, eaterSize);
		pacDying[5] = Bitmap.createBitmap(tmpBmp, eaterSize * 5, 0, eaterSize, eaterSize);
		pacDying[6] = Bitmap.createBitmap(tmpBmp, eaterSize * 6, 0, eaterSize, eaterSize);
		pacDying[7] = Bitmap.createBitmap(tmpBmp, eaterSize * 7, 0, eaterSize, eaterSize);
		pacDying[8] = Bitmap.createBitmap(tmpBmp, eaterSize * 8, 0, eaterSize, eaterSize);
		pacDying[9] = Bitmap.createBitmap(tmpBmp, eaterSize * 9, 0, eaterSize, eaterSize);
		
		bounceScoreBmp = new Bitmap[bounceScores.length];
		bounceScoreBmp[0] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_100);
		bounceScoreBmp[1] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_300);
		bounceScoreBmp[2] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_500);
		bounceScoreBmp[3] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_700);
		bounceScoreBmp[4] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_1000);
		bounceScoreBmp[5] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_2000);
		bounceScoreBmp[6] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_3000);
//		bounceScoreBmp[7] = BitmapFactory.decodeResource(res, R.drawable.fruit_score_5000);
		
	}
	/**��ͬ�ֱ����ֻ�����
	 * 1.Eater��λ��
	 * 2.���ֵ��11
	 */
}
