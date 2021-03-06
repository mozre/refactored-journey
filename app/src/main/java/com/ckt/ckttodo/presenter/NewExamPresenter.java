package com.ckt.ckttodo.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ckt.ckttodo.Base.BasePresenter;
import com.ckt.ckttodo.database.Exam;
import com.ckt.ckttodo.database.PostTaskData;
import com.ckt.ckttodo.database.ServerHost;
import com.ckt.ckttodo.database.User;
import com.ckt.ckttodo.ui.MainActivity;
import com.ckt.ckttodo.ui.NewExamActivity;
import com.ckt.ckttodo.util.HttpUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by MOZRE on 2016/6/27.
 */
public class NewExamPresenter extends BasePresenter {

    private static final String TAG = "NewMessagePresenter";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private static final String PATH_EXAM = "/exam";
    private static final String RESULT_CODE = "resultCode";
    private Context mContext;
    private ServerHost serverHost;

    public NewExamPresenter(Context mContext) {
        this.mContext = mContext;
        serverHost = new ServerHost(mContext);
    }

    public void postNewArticleMessage(final Exam mData, final Handler handler) {
        final User user = new User(mContext);
        final PostTaskData postTaskData = new PostTaskData(mData);
        JSONObject object = new JSONObject();
        object.put("data", mData);
        Log.d(TAG, "postNewArticleMessage: " + object.toJSONString());
        Observable
                .create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> subscriber) {
                        JSONObject object = new JSONObject();
                        object.put("data", postTaskData);
                        OkHttpClient client = HttpUtils.getClient();
                        RequestBody requestBody = null;
                        requestBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("username", user.getUserName())
                                .addFormDataPart("token", user.getToken())
                                .addFormDataPart("data", object.toJSONString())
                                .build();
                        Log.d(TAG, "call: " + requestBody.toString());
                        final Request request = HttpUtils.getCommonBuilder(PATH_EXAM).post(requestBody).tag(NewExamPresenter.this).build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                subscriber.onNext(response.body().string());
                            }
                        });

                    }
                })
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String resultStr) {
                        JSONObject jsonObject = JSONObject.parseObject(resultStr);
                        int result = Integer.valueOf(jsonObject.getInteger(RESULT_CODE));
                        return result;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Message msg = new Message();
                        msg.what = MainActivity.PUSHLISH_NEW_EXAM_FAIL;
                        msg.obj = mData.getExam_id();
                        handler.handleMessage(msg);
                    }

                    @Override
                    public void onNext(Integer o) {
                        Message msg = new Message();
                        if (o == HttpUtils.SUCCESS_REPONSE_CODE) {

                            if (mData.getStatus() == Exam.STATUS_DATA_SAVE) {
                                msg.what = MainActivity.SAVE_NEW_EXAM_SUCCESS;
                            } else {
                                msg.what = MainActivity.PUSHLISH_NEW_EXAM_SUCCESS;
                            }

                        } else {
                            msg.what = o;
                        }
                        handler.handleMessage(msg);
                    }
                });
    }

    public void delExam(final String id, final NewExamActivity.DelSuccessful notify) {

        final User user = new User(mContext);

        Observable
                .create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        OkHttpClient client = HttpUtils.getClient();
                        StringBuilder sb = new StringBuilder("/exam/del?").append("username=").append(user.getUserName())
                                .append("&token=").append(user.getToken()).append("&id=").append(id);
                        Log.d(TAG, "call: sb = " + sb.toString());
                        try {
                            Request request = HttpUtils.getCommonBuilder(sb.toString()).get().tag(NewExamPresenter.this).build();


                            Log.d(TAG, "call: here " + request.toString());
                            Response response = client.newCall(request).execute();
                            Log.d(TAG, "call: end");
                            String str = response.body().string();
//                            Log.d(TAG, "call: str = " + str);
                            subscriber.onNext(str);
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        notify.networkErro();
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String rs) {
                        JSONObject object = JSON.parseObject(rs);
                        int result = object.getInteger(RESULT_CODE);
                        switch (result) {
                            case HttpUtils.SUCCESS_REPONSE_CODE:
                               notify.delSuccessful();
                                break;
                            case HttpUtils.FAIL_ILLEGAL_USER_RESPONSE_CODE:
                            case HttpUtils.FALL_ILLEGAL_PASSWORD_RESPONSE_CODE:
                                break;
                        }
                    }
                });


    }

    @Override
    public void onDestroy() {
        HttpUtils.cancel(this);
    }


}
