package com.example.ruanyulin.asr_tts;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
//import com.example.administrator.baiduvoicetest.R;
import com.example.ruanyulin.asr_tts.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView textResult;
    private EventManager eventManager;
    private String mSampleDirPath; //本地路径
    private String app;
    private SpeechSynthesizer speechSynthesizer;
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton fab_setting;

    //packageName
    private String weixinp = "com.tencent.mm";
    private String qqp = "com.tencent.mobileqq";
    private String zhihup = "com.zhihu.android";
    private String baidumapp = "com.baidu.BaiduMap";
    private String tiebap = "com.baidu.tieba";
    private String zhibo = "air.tv.douyu.android";
    private String train = "com.MobileTicket";
    private String transfor = "com.youdao.dict";
    private String taobao = "com.taobao.taobao";
    private String weibo = "com.sina.weibo";
    private String jingdong = "com.jingdong.app.mall";
    private String tianmao = "com.tmall.wireless";
    private String calendar = "com.android.calendar";
    private String clock = "com.android.alarmclock";
    private String calcu = "com.android.calculator2";


    private static final String TTS_TEXT_MODEL_FILE = "bd_etts_text.dat";
    private static final String TTS_SPEECH_MODEL_FILE = "bd_etts_speech_female.dat";
    private static final String TTS_SPEECH_MODEL1_FILE = "bd_etts_speech_male.dat";
    private static final String TTS_LICENSE_FILE = "temp_license";
    private static final String TTS_ENGLISH_SPEECH_MODEL1 = "bd_etts_speech_female_en.dat";
    private static final String TTS_ENGLISH_SPEECH_MODEL2 = "bd_etts_speech_male_en.dat";
    private static final String TTS_ENGLISH_TEXT_MODEL_FILE = "bd_etts_text_en.dat";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //textResult = (TextView) findViewById();
        textResult = (TextView) findViewById(R.id.texx);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        fab_setting = (FloatingActionButton) findViewById(R.id.fab_setting);
        textResult.setText("说出指令可进行相关操作 \n" +
                "如 “QQ” 或者 “打开QQ” 则会打开手机QQ\n" +
                "“安装 xx” 则会搜索相关应用\n" +
                "请点击按钮或说出唤醒词启动语音助手\n" +
                "唤醒词为 “合工你好”");

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("com.baidu.action.RECOGNIZE_SPEECH");
                //设置离线授权文件
                intent.putExtra("grammar","assets:///baidu_speech_grammar.bsg");
                startActivityForResult(intent,1);
            }
        });

        //setting
        fab_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        initEnv();
        initTTS();
        speak("欢迎使用语音助手,请点击按钮或说出唤醒词启动语音识别");

    }

    @Override
    protected void onStart() {
        super.onStart();

        //创建唤醒事件管理器
        eventManager = EventManagerFactory.create(MainActivity.this,"wp");
        //注册唤醒事件监听器
        eventManager.registerListener(new EventListener() {
            @Override
            public void onEvent(String name, String params, byte[] bytes, int i, int i1) {
                try{
                    JSONObject jsonObject = new JSONObject(params);
                    if ("wp.data".equals(name)){
                        Toast.makeText(MainActivity.this,"唤醒成功",Toast.LENGTH_SHORT).show();

                        speak("唤醒成功");
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                Intent intent = new Intent("com.baidu.action.RECOGNIZE_SPEECH");
                                //设置离线授权文件
                                intent.putExtra("grammar","asset:///baidu_speech_grammar.bsg");
                                startActivityForResult(intent,1);
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task,1500);


                    }else if ("wp.exit".equals(name)){
                        //Toast.makeText(MainActivity.this,"停止唤醒功能" ,Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        HashMap params = new HashMap();
        params.put("kws-file","assets:///WakeUp.bin");
        eventManager.send("wp.start",new JSONObject(params).toString(),null,0,0);
        //Toast.makeText(this,"启动唤醒",Toast.LENGTH_SHORT).show();

    }
    @Override protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //停止唤醒功能
        eventManager.send("wp.stop", null, null, 0, 0);
        //Toast.makeText(this,"唤醒关闭",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {

        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == RESULT_OK) {
            Bundle result = data.getExtras();
            ArrayList<String> result_recognition = result.getStringArrayList("results_recognition");

            String results = result_recognition + "";
            String res = results.substring(results.indexOf("[")+1,results.indexOf("]"));
            app = res;
        }
        analy();
    }

    //分析指令-启动app
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private void analy(){
        String apppackage = null;
        if (app.indexOf("搜索") != -1){
            try {
                String search = app.substring(2);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY,search);
                startActivity(intent);
                speak("正在搜索" + search);
            } catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("打电话") != -1){
            try {
                //String tel = app.substring(4);
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(app);
                matcher.find();
                Uri uri = Uri.parse("tel:" + matcher.group());
                Intent intent = new Intent(Intent.ACTION_DIAL,uri);
                startActivity(intent);
                speak("打电话给" + matcher.group());
            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("发短信") != -1){
            try {
                //String msg = app.substring(4);
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(app);
                matcher.find();
                Uri uri = Uri.parse("smsto:" + matcher.group());
                Intent intent = new Intent(Intent.ACTION_SENDTO,uri);
                intent.putExtra("sms_body","TheSMS text");
                startActivity(intent);
                speak("发短信给" + matcher.group());

            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("相机") != -1 || app.indexOf("照相") != -1){
            try {
                Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                speak("正在打开相机");
                startActivity(intent);
                speak("打开相机");
            }catch (Exception e){
                speak("系统出错");
            }finally {
                return;
            }

        } else if (app.indexOf("图片") != -1 || app.indexOf("照片") != -1 || app.indexOf("图库") != -1 ) {
            try {
                /*Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);*/
                Intent intent1 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(intent1);

            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("录音") != -1) {
            try {
                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                startActivity(intent);
                speak("正在录音");
            } catch (Exception e) {
                speak("系统出错");
            }

        } else if (app.indexOf("安装") != -1){
            try {
                String name = app.substring(2);
                Uri uri = Uri.parse("market://search?q=" + name);
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
                speak("正在安装" + name);
            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("联系人") != -1){
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);
                speak("正在打开联系人列表");
            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("音乐") != -1){
            try {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(Uri.EMPTY,"vnd.android.cursor.dir/playlist");
                intent.putExtra("withtabs",true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                Intent intent1 = Intent.createChooser(intent,"Choose an application to open with:");
                if (intent1 == intent){
                    startActivity(intent1);
                } else {
                    Intent intent2 = new Intent("android.intent.action.MUSIC_PLAYER");
                    startActivity(intent2);
                }
                speak("正在打开音乐播放器");

            }catch (Exception e){
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("视频") != -1) {
            try {
                Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,"1");
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                //intent.setClassName("com.cooliris.media","com.cooliris.media.MovieView");
                startActivity(intent);
                speak("打开视频播放器");
            }catch (Exception e) {
                speak("系统出错");
            } finally {
                return;
            }

        } else if (app.indexOf("微信") != -1){
            apppackage = weixinp;
            speak("正在打开微信");
            turnApp(apppackage);
        } else if (app.indexOf("QQ") != -1){
            apppackage = qqp;
            speak("正在打开QQ");
            turnApp(apppackage);
        } else if (app.indexOf("知乎") != -1){
            apppackage = zhihup;
            speak("正在打开知乎");
            turnApp(apppackage);
        } else if (app.indexOf("贴吧") != -1) {
            apppackage = tiebap;
            speak("正在打开百度贴吧");
            turnApp(apppackage);
        } else if (app.indexOf("地图") != -1) {
            apppackage = baidumapp;
            speak("正在打开地图");
            turnApp(apppackage);
        } else if (app.indexOf("购物") != -1 ||app.indexOf("买东西") != -1 ) {
            if (isAppInstalled(taobao)){
                apppackage = taobao;
                speak("正在打开淘宝");
            } else if (isAppInstalled(tianmao)){
                apppackage = tianmao;
                speak("正在打开天猫");
            } else if (isAppInstalled(jingdong)){
                apppackage = jingdong;
                speak("正在打开京东");
            }

            turnApp(apppackage);
        } else if (app.indexOf("微博") != -1) {
            apppackage = weibo;
            turnApp(apppackage);
        } else if (app.indexOf("词典") != -1 || app.indexOf("翻译") != -1) {
            apppackage = transfor;
            speak("正在打开词典");
            turnApp(apppackage);
        } else if (app.indexOf("火车") != -1 || app.indexOf("车票") != -1) {
            apppackage = train;
            speak("正在打开12306");
            turnApp(apppackage);
        } else if (app.indexOf("直播") != -1) {
            apppackage = zhibo;
            speak("正在打开斗鱼");
            turnApp(apppackage);
        } else if (app.indexOf("京东") != -1 ) {
            apppackage = jingdong;
            speak("正在打开京东");
            turnApp(apppackage);
        } else if (app.indexOf("天猫") != -1) {
            apppackage = tianmao;
            speak("正在打开天猫");
            turnApp(apppackage);
        } else if (app.indexOf("淘宝") != -1) {
            apppackage = taobao;
            speak("正在打开淘宝");
            turnApp(apppackage);
        } else if (app.indexOf("日历") != -1) {
            apppackage = calendar;
            turnApp(apppackage);
        } else if (app.indexOf("闹钟") != -1) {
            apppackage = clock;
            turnApp(apppackage);
        } else if (app.indexOf("计算器") != -1) {
            apppackage = calcu;
            turnApp(apppackage);
        }
        else {
            if (app.indexOf("打开") != -1) {
                try {
                    String name = app.substring(2);
                    Uri uri = Uri.parse("market://search?q=" + name);
                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                    startActivity(intent);
                    speak("请先安装应用" + name);
                }catch (Exception e){
                    speak("系统出错");
                } finally {
                    return;
                }
            } else {
                try {
                    //String search = app.substring(2);

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY,app);
                    startActivity(intent);
                    speak("正在搜索" + app);
                } catch (Exception e){
                    speak("系统出错");
                } finally {
                    return;
                }
            }

        }


    }

    //执行指令-打开应用
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    protected void turnApp(String packagename) {
        try {
            //方法1：通过包名获取类名
            /*
            PackageInfo packageInfo = getPackageManager().getPackageInfo(apppackage,0);
            Intent intentWeixin = new Intent(Intent.ACTION_MAIN,null);
            intentWeixin.setPackage(packageInfo.packageName);
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> apps = packageManager.queryIntentActivities(intentWeixin,0);
            ResolveInfo resolveInfo = apps.iterator().next();
            if (resolveInfo != null) {
                apppackage = resolveInfo.activityInfo.packageName;
                String className = resolveInfo.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName componentName = new ComponentName(apppackage,className);
                textResult.append(apppackage + "\n" + className);
                intent.setComponent(componentName);
                startActivity(intent);
            }*/

            //方法2：通过包名直接启动应用
            Intent intent;
            PackageManager packageManager=getPackageManager();
            intent = packageManager.getLaunchIntentForPackage(packagename);
            startActivity(intent);
        } catch (Exception e){
            speak( "应用未安装，请先安装应用");
            install(packagename);
        }
    }

    //判断app是否安装
    protected boolean isAppInstalled(String packagename) {
        try {
            this.getPackageManager().getPackageInfo(packagename,0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    //自动下载
    protected void install(String packagename) {
        Uri uri = Uri.parse("market://details?id=" + packagename);
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        try {
            this.startActivity(intent);
        } catch (ActivityNotFoundException e){
            speak("没有找到该应用");
        }
    }
    //初始化TTS
    private void initTTS(){
        this.speechSynthesizer = SpeechSynthesizer.getInstance();
        this.speechSynthesizer.setContext(this);
        this.speechSynthesizer.setSpeechSynthesizerListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizeStart(String s) {

            }

            @Override
            public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {

            }

            @Override
            public void onSynthesizeFinish(String s) {

            }

            @Override
            public void onSpeechStart(String s) {

            }

            @Override
            public void onSpeechProgressChanged(String s, int i) {

            }

            @Override
            public void onSpeechFinish(String s) {

            }

            @Override
            public void onError(String s, SpeechError speechError) {

            }
        });
        //文本模型路径（离线）
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE,mSampleDirPath + "/" + TTS_TEXT_MODEL_FILE);
        //设置声学模型（男声、女生）
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE,mSampleDirPath + "/" + TTS_SPEECH_MODEL_FILE);
        //发声人
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER,"0");
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE,SpeechSynthesizer.MIX_MODE_HIGH_SPEED_SYNTHESIZE);
        //请填写你申请到的appid、apikey
        this.speechSynthesizer.setAppId("9981566");
        this.speechSynthesizer.setApiKey("gT0SvLuw0qYVt6MIHdYEwSov","0cea5edd47a78f511f9ab95afff88480");
        //授权检测接口
        AuthInfo authInfo = this.speechSynthesizer.auth(TtsMode.MIX);

        if (authInfo.isSuccess()){
            Toast.makeText(MainActivity.this,"connected successed",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this,"connected failed",Toast.LENGTH_SHORT).show();
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            Toast.makeText(this,errorMsg,Toast.LENGTH_SHORT).show();
        }

        //初始化tts
        speechSynthesizer.initTts(TtsMode.MIX);
    }

    protected void speak(String text) {
        int re = this.speechSynthesizer.speak(text);
        if (re < 0) {
            Toast.makeText(MainActivity.this,"error to speak",Toast.LENGTH_SHORT).show();
        }
    }

    private void initEnv(){
        if (mSampleDirPath == null) {
            String path = Environment.getExternalStorageDirectory().toString();
            mSampleDirPath = path + "/" + "ASR_TTS";
            File file = new File(mSampleDirPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        }

    }

}
