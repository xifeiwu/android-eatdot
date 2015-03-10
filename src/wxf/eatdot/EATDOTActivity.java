package wxf.eatdot;

import java.util.Calendar;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class EATDOTActivity extends Activity {
	public static int screenW, screenH;
	public static EATDOTActivity instance;
	
	public static final int WVGA = 1, HVGA = 2;
	public static int screenType;
	public static final int TOUCH = 1, SENSOR = 2;
	public static int controlType;
	public static boolean isSoundOn;
	
	private SharedPreferences sharedData;
	private Editor editor;
	
	public static SoundPool sp;
	public static int pacstartgame, paceatdot, pacblueghost, pacdie, paceatfruit, 
	paceatghost, pacghosteyes;
	public static int pacblueghostId = 0, paceatdotId = 0, ghosteyesId = 0;

	/**声音的播放
	 * 1.pacstartgame，游戏开始时在GameView的newGame方法中调用；
	 * 2.paceatdot，pac吃到豆子时，在Eater的logic方法中调用；无限循环
	 * 3.pacblueghost，在Eater吃到超级豆后，在Eater的stateChangeTo方法中调用；无限循环
	 * 4.pacdie，Eater死亡后，在Eater的stateChangeTo方法中调用；
	 * 5.paceatfruit，Eater吃掉fruit后，在Eater的logic方法中调用；
	 * 6.paceatghost，Eater吃掉ghost，在GameView的logic方法中调用；
	 * 7.pacghosteyes，ghost死亡回屋时，在Sprite的stateChangeTo方法中调用。无线循环
	 */
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;		
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
 		requestWindowFeature(Window.FEATURE_NO_TITLE);
 		
 		sp = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
 		pacstartgame = sp.load(this, R.raw.pacstartgame, 1);
 		paceatdot = sp.load(this, R.raw.pacdot_combined, 1);//pacdot_kapaceatdot
 		pacblueghost = sp.load(this, R.raw.pacblueghost, 1);
 		paceatfruit = sp.load(this, R.raw.paceatfruit, 1);
 		paceatghost = sp.load(this, R.raw.paceatghost, 1);
 		pacdie = sp.load(this, R.raw.pacdie, 1);
 		pacghosteyes = sp.load(this, R.raw.pacghosteyes, 1);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
 		getSharedData();
        showMenuView();//setContentView(R.layout.main);
    }

    // 界面宏
	public static final int MENUVIEW = 1, SETTINGVIEW = 3, SCOREVIEW = 4,
			ABOUTVIEW = 5, GAMEVIEW = 2, CLIPBMPVIEW = 6;
	public int currentView;
    public void showMenuView(){
    	currentView = MENUVIEW;
    	setContentView(new MenuView(this));
    }
    private GameView gameView = null;
	public void showGameView(){
    	Calendar calendar = Calendar.getInstance();
    	int year = calendar.get(Calendar.YEAR);
    	if(year > 2015){
    		return;
    	}
    	currentView = GAMEVIEW;
    	gameView = new GameView(this);
    	setContentView(gameView);		
	}
	public void showSetView(){
		currentView = SETTINGVIEW;
		setContentView(R.layout.setting);
		Button set_btn = (Button) findViewById(R.id.set_ok_btn);
		final RadioButton touch_rb = (RadioButton) findViewById(R.id.touch_rb);
		final RadioButton sensor_rb = (RadioButton) findViewById(R.id.sensor_rb);
		final RadioButton yes_rb = (RadioButton) findViewById(R.id.yes_rb);
		final RadioButton no_rb = (RadioButton) findViewById(R.id.no_rb);
		switch(controlType){
		case TOUCH:
			touch_rb.setChecked(true);
			break;
		case SENSOR:
			sensor_rb.setChecked(true);
			break;
		}
		if(isSoundOn){
			yes_rb.setChecked(true);
			no_rb.setChecked(false);
		}else{
			yes_rb.setChecked(false);
			no_rb.setChecked(true);
		}
		set_btn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (touch_rb.isChecked()) {
					controlType = TOUCH;
				} else if (sensor_rb.isChecked()) {
					controlType = SENSOR;
				}
				if (yes_rb.isChecked()) {
					isSoundOn = true;
				} else if (no_rb.isChecked()) {
					isSoundOn = false;
				}
				showMenuView();
			}
			
		});
	}
	public int numbersInScoreView;
	private  Vector<ScoreItem> scoreItems;
	private TextView tv;
	private ListView lv;
	public void showScoreView(){    	
		currentView = SCOREVIEW;       
		setContentView(R.layout.score);
		tv = (TextView) findViewById(R.id.tv_scoretitle);
		lv = (ListView) findViewById(R.id.list);
		lv.setCacheColorHint(0);
		if (scoreItems.size() == 0) {
			tv.setText("您还没有成功过哦");
		} else {
			tv.setText("成绩排行榜");
			lv.setAdapter(new ScoreAdapter(this, 0, scoreItems));
		}
	}

    private TextView about_content1, about_content2, about_content3;
	public void showAboutView(){
    	currentView = ABOUTVIEW;       
    	setContentView(R.layout.about);
    	String content1 = "    游戏目标：在不被敌人吃掉的情况下吃光所有豆子，即可进入下一关。";
    	String content2 = "    关于成绩：吃掉一个小豆获得10分，吃掉闪光豆后15秒内，可以反过来将敌人吃掉，" +
    			"吃掉第一个获得200分奖励，第二个获得400分、第三个800分，第四个1600分。" +
    			"每隔30秒，在游戏界面中会出现水果，吃掉后会获得高额分数奖励，成绩每满10000分增加一条生命" +
    			"。游戏会保存前十名最高成绩。";
    	String content3 = "    操作：可以通过手势识别或重力感应的方式来控制方向；在游戏界面，" +
    			"单击menu键可以选择暂停或重新开始游戏；游戏结束后单击屏幕返回主菜单。";
    	about_content1 = (TextView) findViewById(R.id.about_content1);
    	about_content2 = (TextView) findViewById(R.id.about_content2);
    	about_content3 = (TextView) findViewById(R.id.about_content3);
    	about_content1.setText(content1);
    	about_content2.setText(content2);
    	about_content3.setText(content3);  	
	}
	private String[] time_int = { "t1", "t2", "t3", "t4", "t5", "t6", "t7",
			"t8", "t9", "t10" };
	private String[] fn_str = { "fn1", "fn2", "fn3", "fn4", "fn5", "fn6",
			"fn7", "fn8", "fn9", "fn10" };
    private void getSharedData(){
		sharedData = this.getSharedPreferences("EATDOT",
				Context.MODE_PRIVATE);
		editor = sharedData.edit();
		controlType = TOUCH;
		isSoundOn = true;
		numbersInScoreView = 0;
		int tmp;
		tmp = sharedData.getInt("sound&control&size", -1);
//		if (tmp != -1) {
//			controlType = tmp & 0xf;
//			isSoundOn = ((tmp >> 4) & 0xf) > 0 ? true : false;
//		}
//		tmp = sharedData.getInt("vecsize", -1);
		if (tmp != -1) {
			numbersInScoreView = tmp & 0xf;
			controlType = (tmp >> 4) & 0xf;
			isSoundOn = (((tmp >> 8) & 0xf) == 0) ? false : true;
		}
		int i, timeused;
		String time;
		scoreItems = new Vector<ScoreItem>();
		for (i = 0; i < numbersInScoreView; i++) {
			// 将数据取出
			timeused = sharedData.getInt(time_int[i], 0);
			time = sharedData.getString(fn_str[i], "");
			// 如果数据没有错误，加入到vec中
			if ((time != "") && (timeused != 0)) {
				ScoreItem item = new ScoreItem(timeused, time);
				scoreItems.add(item);
			}
		}
		// 重设numbersInScoreView的大小
		numbersInScoreView = scoreItems.size();    	
    }
    /**保存成绩的两种情况：
	*游戏结束，在生命数为0，进入GAMEOVER或TOUCHTOMENUVIEW时；
	*游戏没有结束，按BACK键进入MENUVIEW时。
	*/
    public int setScore(int scores, String timestr) {
		ScoreItem item = new ScoreItem(scores, timestr);
		int i, pos = -1, tscore, rank;
		if (numbersInScoreView == 0) {// 如果没有存储数据，直接加入vec
			scoreItems.add(item);
			rank = 1;
		} else {// 已有历史数据
				// 将新的成绩加入到适当的位置
			for (i = 0; i < numbersInScoreView; i++) {
				tscore = scoreItems.get(i).scores;
				if (tscore < scores) {
					pos = i;
					break;
				}
			}
			if (pos == -1) {
				scoreItems.add(item);
				rank = numbersInScoreView + 1;
			} else {
				scoreItems.add(pos, item);
				rank = pos + 1;
			}
		}
		numbersInScoreView++;
		if (numbersInScoreView > 10) {
			numbersInScoreView = 10;
		}
		return rank;
	}
    
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		stopSoundPool();
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub		
		int tmp;
		// 最后还是要同步vec中的数目
		numbersInScoreView = scoreItems.size();
		if (numbersInScoreView > 10) {
			numbersInScoreView = 10;
		}
		tmp = ((isSoundOn? 1 : 0) << 8) | (controlType << 4) | numbersInScoreView;
		editor.putInt("sound&control&size", tmp);
		for (int i = 0; i < numbersInScoreView; i++) {
			editor.putInt(time_int[i], scoreItems.get(i).scores);
			editor.putString(fn_str[i], scoreItems.get(i).timeStr);
		}
		editor.commit();
		super.onDestroy();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(currentView != MENUVIEW){
//				if(currentView == GAMEVIEW){
//					stopSoundPool();
//				}
				showMenuView();
				return true;
			}
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (currentView == GAMEVIEW) {
				openOptionsMenu();
			}
			return true;
		} 
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "暂停");
		menu.add(0, 1, 0, "重新开始");
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch(item.getItemId()){
		case 0:
			gameView.stateChangedTo(GameView.PAUSE);
			stopSoundPool();
			Toast.makeText(this, "点击屏幕，继续游戏。", Toast.LENGTH_LONG).show();
			break;
		case 1:
			stopSoundPool();
			gameView.newGame();
			break;
		}
		return true;
	}
	public static void stopSoundPool(){
		if(isSoundOn){
			if(paceatdotId != 0){
				sp.stop(paceatdotId);
				paceatdotId = 0;
			}
			if(pacblueghostId != 0){
	    		sp.stop(pacblueghostId);
	    		pacblueghostId = 0;   
			}
			if(ghosteyesId != 0){
				sp.stop(ghosteyesId);
				ghosteyesId = 0;
			}	
		}
	}
}
class ScoreItem {
	public int scores;
	public String timeStr;
	public ScoreItem(int s, String str){
		scores = s;
		timeStr = str;
	}
}