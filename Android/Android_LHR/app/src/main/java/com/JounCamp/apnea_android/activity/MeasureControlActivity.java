package com.JounCamp.apnea_android.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.JounCamp.apnea_android.R;
import com.JounCamp.apnea_android.info.JoinInfo;
import com.JounCamp.apnea_android.info.MeasureRequestInfo;
import com.JounCamp.apnea_android.RetrofitClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeasureControlActivity extends AppCompatActivity implements View.OnClickListener {

    Button start_button;
    Button stop_button;
    Button history_button;
    Button logout_button;

    String wifi;

    TextView status_text;

    private RetrofitClient retrofitClient;
    private com.JounCamp.apnea_android.initMyApi initMyApi;
    static MeasureRequestInfo measureRequestInfo;
    static JoinInfo joinInfo;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measure_start_stop);

        status_text = (TextView) findViewById(R.id.status_textview);

        start_button = (Button) findViewById(R.id.start_button);
        stop_button = (Button) findViewById(R.id.stop_button);
        history_button = (Button) findViewById(R.id.history_button);
        logout_button = (Button) findViewById(R.id.logout_button);

        start_button.setOnClickListener(this);
        stop_button.setOnClickListener(this);
        history_button.setOnClickListener(this);
        logout_button.setOnClickListener(this);

        SharedPreferences sp = getSharedPreferences("shared", MODE_PRIVATE);
        Gson gson = new GsonBuilder().create();
        String jsonJoinInfo = sp.getString("jsonJoinInfo", "");
        String jsonMeasureRequestInfo = sp.getString("jsonMeasureRequestInfo", "");

        if (!jsonJoinInfo.equals("") && !jsonMeasureRequestInfo.equals("")) {
            joinInfo = gson.fromJson(jsonJoinInfo, JoinInfo.class);
            Log.d("kim", "username = " + joinInfo.getUsername());

            measureRequestInfo = gson.fromJson(jsonMeasureRequestInfo, MeasureRequestInfo.class);
            Log.d("kim", "user status = " + measureRequestInfo.getStatus());

        }

        if(measureRequestInfo.getStatus() == 2) {
            Toast.makeText(MeasureControlActivity.this, "연결되었습니다.", Toast.LENGTH_SHORT).show();

        }

        Intent statusIntent = getIntent();

        if(!TextUtils.isEmpty(statusIntent.getStringExtra("statusTitle"))){
            if(statusIntent.getStringExtra("statusTitle").equals("정상 호흡")) {
                status_text.setText("현재 상태 : 정상 호흡");
                Log.d("kim", "statusIntent - 정상 호흡");
            }
        }
        if(!TextUtils.isEmpty(statusIntent.getStringExtra("statusTitle"))){
            if(statusIntent.getStringExtra("statusTitle").equals("초기 측정 시작")) {
                status_text.setText("현재 상태 : 호흡 패턴 분석중 ..");
                Log.d("kim", "statusIntent - 초기측정 완료");
            }
        }

        if(!TextUtils.isEmpty(statusIntent.getStringExtra("statusTitle"))){
            if(statusIntent.getStringExtra("statusTitle").equals("초기측정 완료")) {
                status_text.setText("현재 상태 : 무호흡 감지 시작");
                Log.d("kim", "statusIntent - 초기측정 완료");
            }
        }

        if(!TextUtils.isEmpty(statusIntent.getStringExtra("statusTitle"))){
            if(statusIntent.getStringExtra("statusTitle").equals("진동")) {
                status_text.setText("현재 상태 : 무호흡 발생 (5~7초)");
                Log.d("kim", "statusIntent - 진동 울림");
            }
        }

        if(!TextUtils.isEmpty(statusIntent.getStringExtra("statusTitle"))){
            if(statusIntent.getStringExtra("statusTitle").equals("소리")) {
                status_text.setText("현재 상태 : 무호흡 발생 (7~10초)");
                Log.d("kim", "statusIntent - 소리 울림");
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:     // 측정 시작 버튼
                System.out.println("측정 시작 클릭");

                if(measureRequestInfo.getStatus() == 0) {
                    statusResponse(measureRequestInfo);
                    status_text.setText("현재 상태 : 측정 중");

                    //FCM Test
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    if(!task.isSuccessful()) {
                                        Log.d("kim", "Fatching FCM registration token failed", task.getException());
                                    }

                                    String token = task.getResult();

                                    String msg = "test" + token;
                                    Log.d("kim", msg);
//                                    Toast.makeText(login.this, msg, Toast.LENGTH_LONG).show();
                                }
                            });
                    //FCM Test
                } else if(measureRequestInfo.getStatus() == 1) {
                    Toast.makeText(MeasureControlActivity.this, "이미 측정중 입니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.stop_button:    // 측정 종료 버튼
                System.out.println("측정 종료 클릭");


                if(measureRequestInfo.getStatus() == 1) {
                    statusResponse(measureRequestInfo);
                    status_text.setText("현재 상태 : 측정 종료");
                } else if(measureRequestInfo.getStatus() == 0) {
                    Toast.makeText(MeasureControlActivity.this, "이미 측정 종료 상태 입니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.history_button:
                if(measureRequestInfo.getStatus() == 1){
                    Toast.makeText(MeasureControlActivity.this, "측정 중입니다.\n다른 페이지로 가시려면 측정 종료를 눌러주세요.", Toast.LENGTH_SHORT).show();
                }
                else if(measureRequestInfo.getStatus() == 0) {
                    System.out.println("메인페이지 클릭");
                    Toast.makeText(MeasureControlActivity.this, "최근 일주일 간 측정 현황입니다.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MeasureControlActivity.this, ChartActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.logout_button:

                SharedPreferences sp = getSharedPreferences("shared", MODE_PRIVATE);

                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.commit();

                String jsonJoinInfo = sp.getString("jsonJoinInfo", "");
                String jsonMeasureRequestInfo = sp.getString("jsonMeasureRequestInfo", "");

                if (jsonJoinInfo.equals("") && jsonMeasureRequestInfo.equals("")) {
                    Log.d("kim", "jsonJoinInfo, jsonMeasureRequestInfo 프리퍼런스 정보 삭제 완료");
                }

                Toast.makeText(MeasureControlActivity.this, joinInfo.getUsername()+"님 로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MeasureControlActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
        }
    }

    public void statusResponse(MeasureRequestInfo statusUser) {
        //retrofit 생성
        retrofitClient = RetrofitClient.getInstance();
        initMyApi = RetrofitClient.getRetrofitInterface();


        //User에 저장된 데이터와 함께 init에서 정의한 getJoinResponse 함수를 실행한 후 응답을 받음
        initMyApi.getStatusResponse(statusUser).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                Log.d("kim", "retrofit Data Fetch success");
                Log.d("kim", "joinResponse: " + statusUser.toString());
                //통신 성공
                if (response.isSuccessful()) {
                    Log.d("kim", response.body().toString());
                    if (response.body() == 1) {
                        Toast.makeText(MeasureControlActivity.this, "측정 시작", Toast.LENGTH_SHORT).show();
                    } else if (response.body() == 0) {
                        Toast.makeText(MeasureControlActivity.this, "측정 종료", Toast.LENGTH_SHORT).show();
                    } else if (response.body() == -1) {
                        Toast.makeText(MeasureControlActivity.this, "? 오류 발생 ?", Toast.LENGTH_SHORT).show();
                    }
                    statusUser.setStatus(response.body());
                    //preference status 값 최신화
                    saveJoinInfo(statusUser);
                } else {
                    Toast.makeText(MeasureControlActivity.this, "0.통신 오류로 인한 측정 시작 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Toast.makeText(MeasureControlActivity.this, "1.통신 오류로 인한 측정 시작 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    //sharedPreference에 MeasureRequestInfo 저장
    public void saveJoinInfo(MeasureRequestInfo measureRequestInfo) {
        Gson gson = new GsonBuilder().create();
        SharedPreferences sp;

        sp = getSharedPreferences("shared", MODE_PRIVATE);

        String jsonMeasureRequestInfo = gson.toJson(measureRequestInfo, MeasureRequestInfo.class);

        SharedPreferences.Editor editor = sp.edit();
        editor.putString("jsonMeasureRequestInfo", jsonMeasureRequestInfo);
        editor.commit();
    }
}