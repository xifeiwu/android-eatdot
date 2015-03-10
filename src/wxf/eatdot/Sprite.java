package wxf.eatdot;

import java.util.Random;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class Sprite {
	public static final int READY = 1, COMEOUT = 2, GO = 3, WEAK = 4, DEAD = 5;
	public int state;
	/**
	 * 状态转换： READY时间到且X坐标到位变为COMEOUT； COMEOUT后Y坐标到位变为GO；
	 * Eater吃掉大豆后，从GO状态变为WEAK状态； 相遇后从WEAK状态变为DEAD状态； DEAD状态在Y坐标到位后变为COMEOUT状态
	 */
//	private GameView gameView;

	public static final int NONE = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4;
	public int pX, pY, pDir, wantDir;
	final int dx[] = { // Eater、三个Develop Men在x轴上坐标的变化
	0, 0, 0, -1, 1 };
	final int dy[] = { // Eater、三个Develop Men在y轴上的坐标的变化
	0, -1, 1, 0, 0 //
	};
	final int[] speeds_fast = {11, 11};
	final int[] speeds_mid = {4, 4, 3};
	final int[] speeds_slow = {2, 2, 2, 2, 2, 1};
	private int[] speeds;

	private Bitmap[] spriteBmp;
	private int role;
	private static Random rand;

	public Sprite(GameView view, int role) {
//		gameView = view;
		this.role = role;
		rand = new Random();
		stateChangeTo(READY);
	}

	private int logicCnt, bestVal, bestDir;
	private int mapX, mapY, targetX, targetY;
	private int downValue, leftValue, rightValue;

	public void logic(Eater eater) {
		// 到达可以选择的位置
		switch (state) {
		case READY:// Sprite在屋内运动
			pX += 8 * dx[pDir]; // speeds[speedCnt % 3]
			pX = (pX > 171) ? 171 : pX;// 176
			pX = (pX < 126) ? 126 : pX;// 121
			if (pX == 171 || pX == 126) {
				pDir = 7 - pDir;
			}
			logicCnt++;
			// 可以出发了
			if (logicCnt > comeOutCnt[role] && (pX == 150))// 154
			{
				state = COMEOUT;
				pDir = UP; // 步伐该为1
			}
			break;
		case COMEOUT:
			// Sprite出屋之后，需要调整位置，先调整Y方向后调整X方向。
			pY += speeds[speedCnt % speedStep] * dy[pDir];
			if (pY == 121)// Y坐标到位后，调整X坐标。
			{
				state = GO;
				pDir = LEFT + (eater.pDir & 0x1);
				// 调控速度
				if (pDir == LEFT) {
					speedCnt = 1;
				} else if (pDir == RIGHT) {
					pX += 4;
					speedCnt = 0;
				}
				break;
			}
			// 死了之后，重获新生
			if (pY == 154) {
				stateChangeTo(READY);
				if (role == 0) {
					logicCnt = 0;
				} else {
					logicCnt = comeOutCnt[role - 1];
				}
			}
			break;
		case WEAK:
			// 从WEAK状态转换为GO状态。
			if (logicCnt < GameView.POWERTIMECNT * 2 / 3) {
				spriteBmpStep = 2;
			} else if (logicCnt < GameView.POWERTIMECNT) {
				spriteBmpStep = 4;
			} else {// 10秒后，在特定的坐标位置改变状态。
				if ((pX % 11) == 0 && (pY % 11) == 0) {
					stateChangeTo(GO);
				}
			}
		case DEAD:
		case GO://				
		// if(stateChangeToDead){
		// Log.v("stateChangeToDEAD" + role, pX + "*" + pY + "---" + state);
		// }
			if ((pX % 11) == 0 && (pY % 11) == 0) {
				mapX = pX / 11 + 1;
				mapY = pY / 11;
				if (state == GO) {
					targetX = eater.pX;
					targetY = eater.pY;
				} else if (state == WEAK) {
					targetX = 2 * pX - eater.pX;// 改变运动目标
					targetY = 2 * pY - eater.pY;
				}
				else if (state == DEAD) {
					targetX = 154;
					targetY = 121;
				}
				bestVal = 0;
				bestDir = 0;
				if (pDir != DOWN
						&& GameView.map[mapY + dy[UP]][mapX + dx[UP]] < 8) // 可以向上且没有正在向下运动的情况GameView.map[mapY][mapX]
				{
					bestVal = 50;
					bestDir = UP;
					if (targetY < pY) {
						bestVal += rand.nextInt(randomness) + 1;
					}
				}
				if (pDir != UP
						&& GameView.map[mapY + dy[DOWN]][mapX + dx[DOWN]] < 8) // 可以向下且没有正在向上运动的情况
				{
					downValue = 50;
					if (targetY > pY)
						downValue += rand.nextInt(randomness) + 1;
					if (downValue > bestVal) {
						bestDir = DOWN;
						bestVal = downValue;
					}
				}
				if (pDir != RIGHT
						&& GameView.map[mapY + dy[LEFT]][mapX + dx[LEFT]] < 8) // 可以向左运动且没有正在向右运动的情况
				{
					leftValue = 50;
					if (targetX < pX)
						leftValue += rand.nextInt(randomness) + 1;
					if (leftValue > bestVal) {
						bestDir = LEFT;
						bestVal = leftValue;
					}
				}
				if (pDir != LEFT
						&& GameView.map[mapY + dy[RIGHT]][mapX + dx[RIGHT]] < 8) // 可以向左运动且没有正在向右运动的情况gDir[j1]
				{
					rightValue = 50;
					if (targetX > pX)
						rightValue += rand.nextInt(randomness) + 1;
					if (rightValue > bestVal)
						bestDir = RIGHT;
				}
				pDir = bestDir;
				// 死亡后，到达小屋门口。
				if ((state == DEAD) && (pY == 121) && (pX == 154 || pX == 143)) {
					pX = 150;
					pDir = DOWN;
					state = COMEOUT;
				}
				// 必须在固定位置才可以改变状态为WEAK
//				if (stateChangeToWeak && (state == GO)) {
//					logicCnt = 0;
//					randomness = 1;
//					spriteBmp = ghostBmps;
//					spriteBmpStep = 2;
//					speeds = speeds_slow;
//					speedStep = speeds_slow.length;
//					scoreCnt = 0;
//					state = WEAK;
//					stateChangeToWeak = false;
//				}
				// if(stateChangeToDead){
				// stateChangeToDead = false;
				// }
			}
			// if(stateChangeToDead){
			// Log.v("stateChangeToDEAD" + role, pX + "*" + pY + "---" + state);
			// stateChangeToDead = false;
			// }
			pX += speeds[speedCnt % speedStep] * dx[pDir]; // Develop Men改变位置
			pY += speeds[speedCnt % speedStep] * dy[pDir]; // 注意！！！
			if (pX <= 0) // 从一端出去后从另一端返回
				pX += 308;
			else if (pX >= 308)
				pX -= 308;
//			 if((state == DEAD)){
//			 Log.v("Sprite:" + role, "pX:" + pX + "-" + "pY:" + pY + "-" +
//			 "targetX:" + targetX + "-" + "targetY:" + targetY);
//			 }
			// Log.v("Sprite:" + role, "state" + state);
			break;
		}
		logicCnt++;
		speedCnt++;
	}

	public boolean isCollisionWith(Eater eater) {
		// READY = 1, COMEOUT = 2, GO = 3, WEAK = 4, DEAD = 5;
		if ((state == DEAD) || (state == COMEOUT) || (state == READY)) {
			return false;
		}//spriteSizeeater.eaterSize
		if ((pX + 11) < eater.pX || pX > (eater.pX + 11)
				|| (pY + 11) < eater.pY
				|| pY > (eater.pY + 11)) {
			return false;
		}
		return true;
	}

	private int randomness;
//	private boolean stateChangeToWeak = false;
	// private boolean stateChangeToDead = false;
	private int speedStep, scorePos;
	// 修改状态是被调用
	public void stateChangeTo(int s) {
		switch (s) {
		case READY:
			pX = 126 + 12 * role;// rand.nextInt(45);// 121 126~171
			pY = 154;
			logicCnt = 0;
    		if(EATDOTActivity.isSoundOn &&  (EATDOTActivity.ghosteyesId != 0)){
    			EATDOTActivity.sp.stop(EATDOTActivity.ghosteyesId);
    			EATDOTActivity.ghosteyesId = 0;
    		}
		case GO:
			pDir = LEFT;
			randomness = 15;
			logicCnt = 0;
			spriteBmp = spriteBmps[role];
			speeds = speeds_mid;
			speedStep = speeds_mid.length;
			state = s;
			break;
		case WEAK:
//			stateChangeToWeak = true;
			logicCnt = 0;
			randomness = 1;
			spriteBmp = ghostBmps;
			spriteBmpStep = 2;
			speeds = speeds_slow;
			speedStep = speeds_slow.length;
			scoreCnt = 0;
			state = WEAK;			
			pX = (pX % 11 > 5) ? (pX / 11 + 1) * 11 : (pX / 11) * 11;
			pY = (pY % 11 > 5) ? (pY / 11 + 1) * 11 : (pY / 11) * 11;
//			stateChangeToWeak = false;
			break;
		case DEAD:
			drawCnt = 0;
			targetX = 154;
			targetY = 121;
			randomness = 1;
			spriteBmp = ghostEyesBmps;
			spriteBmpStep = ghostEyesBmps.length;
			speeds = speeds_fast;
			speedStep = speeds_fast.length;

			pX = (pX % 11 > 5) ? (pX / 11 + 1) * 11 : (pX / 11) * 11;
			pY = (pY % 11 > 5) ? (pY / 11 + 1) * 11 : (pY / 11) * 11;
			scoreX = pX;
			scoreY = pY;
			
			scorePos = scoreCnt;
			Sprite.scoreCnt++;
			speedCnt = 0;
			state = DEAD;
    		if(EATDOTActivity.isSoundOn &&  (EATDOTActivity.ghosteyesId == 0)){
    			EATDOTActivity.ghosteyesId = EATDOTActivity.sp.play(EATDOTActivity.pacghosteyes, 1, 1, 1, -1, 1);
    		}
			break;
		}
		if((s != DEAD) && EATDOTActivity.isSoundOn && (EATDOTActivity.ghosteyesId != 0)){
    		EATDOTActivity.sp.stop(EATDOTActivity.ghosteyesId);
    		EATDOTActivity.ghosteyesId = 0;
		}
	}

	private int speedCnt, drawCnt, spriteBmpStep, scoreX, scoreY;

	public void myDraw(Canvas canvas, Paint paint) {
		// Log.v("Sprite Draw", "role:" + role + "-" + pX + "*" + pY);
		switch (state) {
		case READY:
		case COMEOUT:
		case GO:
			if (pDir != NONE) {
				canvas
						.drawBitmap(spriteBmp[2 * pDir - drawCnt % 2 - 1],
								GameView.mazeX + pX - 4, GameView.mazeY + pY
										- 4, paint);
			}
			break;
		case WEAK:
			canvas.drawBitmap(spriteBmp[drawCnt % spriteBmpStep],
					GameView.mazeX + pX - 4, GameView.mazeY + pY - 4, paint);
			break;
		case DEAD:// drawCnt % spriteBmpStep
			//显示分数
			if((drawCnt < 2000 / GameView.MILLISPERSECOND) && (scoreCnt > 0)){
				canvas.drawBitmap(scoreBmps[scorePos], GameView.mazeX + scoreX - 4,
						GameView.mazeY + scoreY - 4, paint);
			}
			canvas.drawBitmap(spriteBmp[pDir - 1], GameView.mazeX + pX - 4,
					GameView.mazeY + pY - 4, paint);
			break;
		}
		drawCnt++;
	}

	public static int Blinky = 0, Clyde = 1, Inky = 2, Pinky = 3;
	public static Bitmap[][] spriteBmps;
	public static int spriteSize;
	public static Bitmap[] ghostBmps;
	public static int[] scoreValue = { 200, 400, 800, 1600 };
	public static int scoreCnt;
	public static Bitmap[] scoreBmps;
	public static Bitmap[] ghostEyesBmps;
	public static int[] comeOutCnt = { (int) (6000 / GameView.MILLISPERSECOND),
			(int) (12000 / GameView.MILLISPERSECOND),
			(int) (18000 / GameView.MILLISPERSECOND),
			(int) (24000 / GameView.MILLISPERSECOND) };

	public static void getBmps() {
		spriteBmps = new Bitmap[4][8];
		ghostBmps = new Bitmap[4];
		scoreBmps = new Bitmap[4];
		ghostEyesBmps = new Bitmap[4];

		Bitmap bmp;
		Resources res = EATDOTActivity.instance.getResources();

		bmp = BitmapFactory.decodeResource(res, R.drawable.blinkyup_anim);
		spriteSize = bmp.getHeight();
		spriteBmps[0][0] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[0][1] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.blinkydown_anim);
		spriteBmps[0][2] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[0][3] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.blinkyleft_anim);
		spriteBmps[0][4] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[0][5] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.blinkyright_anim);
		spriteBmps[0][6] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[0][7] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);

		bmp = BitmapFactory.decodeResource(res, R.drawable.clydeup_anim);
		spriteBmps[1][0] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[1][1] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.clydedown_anim);
		spriteBmps[1][2] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[1][3] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.clydeleft_anim);
		spriteBmps[1][4] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[1][5] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.clyderight_anim);
		spriteBmps[1][6] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[1][7] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);

		bmp = BitmapFactory.decodeResource(res, R.drawable.inkyup_anim);
		spriteBmps[2][0] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[2][1] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.inkydown_anim);
		spriteBmps[2][2] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[2][3] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.inkyleft_anim);
		spriteBmps[2][4] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[2][5] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.inkyright_anim);
		spriteBmps[2][6] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[2][7] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);

		bmp = BitmapFactory.decodeResource(res, R.drawable.pinkyup_anim);
		spriteBmps[3][0] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[3][1] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.pinkydown_anim);
		spriteBmps[3][2] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[3][3] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.pinkyleft_anim);
		spriteBmps[3][4] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[3][5] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.pinkyright_anim);
		spriteBmps[3][6] = Bitmap.createBitmap(bmp, 0, 0, spriteSize,
				spriteSize);
		spriteBmps[3][7] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);

		bmp = BitmapFactory.decodeResource(res, R.drawable.blueghost_anim);
		ghostBmps[0] = Bitmap.createBitmap(bmp, 0, 0, spriteSize, spriteSize);
		ghostBmps[1] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);
		bmp = BitmapFactory.decodeResource(res, R.drawable.whiteghost_anim);
		ghostBmps[2] = Bitmap.createBitmap(bmp, 0, 0, spriteSize, spriteSize);
		ghostBmps[3] = Bitmap.createBitmap(bmp, spriteSize, 0, spriteSize,
				spriteSize);

		scoreBmps[0] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_score_200);
		scoreBmps[1] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_score_400);
		scoreBmps[2] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_score_800);
		scoreBmps[3] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_score_1600);

		ghostEyesBmps[0] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_eyes_up);
		ghostEyesBmps[1] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_eyes_down);
		ghostEyesBmps[2] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_eyes_left);
		ghostEyesBmps[3] = BitmapFactory.decodeResource(res,
				R.drawable.ghost_eyes_right);
	}
}
