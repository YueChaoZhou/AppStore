package com.bb.googleplaybb.ui.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bb.googleplaybb.R;
import com.bb.googleplaybb.domain.User;
import com.bb.googleplaybb.mvp.IUserView;
import com.bb.googleplaybb.mvp.UserPresenter;
import com.bb.googleplaybb.net.NetHelper;
import com.bb.googleplaybb.utils.FileUtils;
import com.bb.googleplaybb.utils.LoginUtils2;
import com.bb.googleplaybb.utils.SharePreferenceUtils;
import com.bb.googleplaybb.utils.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Boby on 2019/5/26.
 */

public class LoginActivity2 extends BaseActivity implements View.OnClickListener, IUserView {

    private TextView mTvPwd, mTvId, mTvUserName, mTvSwitch;
    private EditText mEtUserName, mEtPwd, mEtId;
    private ImageView mIvIcon, mIdWarming, mPwdWarming, mNameWarming;

    private float mYDistance;
    private View mLayoutRegister;
    private Button mMBtnConfirm;
    private View mTvIcon;
//    private View mLayoutPb;

    private PopupWindow mPopupWindow;
    private UserPresenter mPresenter;
    private int REQUEST_CODE_PICK_PHOTO = 0;
    private String photoPath;
    private int mOriginalColor;
    private int mColorAccent;
    private View mProgressing;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private TextView mTvPopup;


    public static void startForResult(Activity context, int requestCode) {
        Intent intent = new Intent(context, LoginActivity2.class);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);
        mPresenter = new UserPresenter(this);

        initView();
        initData();
        initEvent();
    }

    private void initData() {
        mOriginalColor = mTvId.getCurrentTextColor();
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        mColorAccent = typedValue.data;
    }

    private void initView() {
        mTvUserName = findViewById(R.id.tv_user_name);
        mTvPwd = findViewById(R.id.tv_pwd);
        mTvId = findViewById(R.id.tv_id);
        mEtUserName = findViewById(R.id.et_user_name);
        mEtPwd = findViewById(R.id.et_pwd);
        mEtId = findViewById(R.id.et_id);
        mTvSwitch = findViewById(R.id.tv_switch);
        mLayoutRegister = findViewById(R.id.layout_user_name);
        mIvIcon = findViewById(R.id.iv_icon);
        mIdWarming = findViewById(R.id.iv_id_warming);
        mPwdWarming = findViewById(R.id.iv_pwd_warming);
        mNameWarming = findViewById(R.id.iv_name_warming);
        mMBtnConfirm = findViewById(R.id.btn_confirm);
        mTvIcon = findViewById(R.id.tv_icon);
//        mLayoutPb = findViewById(R.id.layout_pb);
    }

    private void initEvent() {
        mEtUserName.addTextChangedListener(new Watcher(mNameWarming));
        mEtId.addTextChangedListener(new Watcher(mIdWarming));
        mEtPwd.addTextChangedListener(new Watcher(mPwdWarming));

        setFocusListener();

        //切换注册
        mTvSwitch.setOnClickListener(this);
        //登录按键
        mMBtnConfirm.setOnClickListener(this);
//        mLayoutPb.setOnClickListener(this);

    }

    private void showProgress() {
        if (mProgressing == null) {
            mProgressing = LayoutInflater.from(this).inflate(R.layout.view_progressing, null);
        }
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getApplicationContext().getSystemService(
                    Context.WINDOW_SERVICE);
        }
        if (mWindowLayoutParams == null) {

            mWindowLayoutParams = createWindowLayoutParams(mTvUserName.getWindowToken());
        }

//        getDialogPermission();
        mWindowManager.addView(mProgressing, mWindowLayoutParams);
    }

    private void removeProgress() {
        if (mProgressing == null) {
            return;
        }
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getApplicationContext().getSystemService(
                    Context.WINDOW_SERVICE);
        }
        mWindowManager.removeView(mProgressing);
    }

    /**
     * create dark layout
     *
     * @param token
     * @return
     */
    private WindowManager.LayoutParams createWindowLayoutParams(IBinder token) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.gravity = Gravity.START | Gravity.TOP;
        p.width = WindowManager.LayoutParams.MATCH_PARENT;
        p.height = WindowManager.LayoutParams.MATCH_PARENT;
        p.format = PixelFormat.TRANSLUCENT;
        //不用申请权限
        p.type = WindowManager.LayoutParams.LAST_SUB_WINDOW;
//        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;//需要权限
        p.token = token;
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
        return p;
    }

    //获取显示在其他应用之上权限
    private void getDialogPermission() {
        //Android6.0 + need require permission
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
                return;
            }
        }
    }

    private void login() {
        showProgress();
        mPresenter.login(new UserPresenter.OnLoginResult() {
            @Override
            public void onResult(User result) {
                removeProgress();
                if (result == null) {

                } else if (result.getResultCode() == User.RESULT_FINDED_USER) {
                    SharePreferenceUtils.setUser(getId(), getPwd());
                    Intent intent = new Intent();
                    intent.putExtra("user", result);
                    setResult(MainActivity.RESULT_LOGIN, intent);
                    finish();
                } else if (result.getResultCode() == User.RESULT_ID_OR_PWD_WRONG) {
                    //id或密码错误提示
                    Toast.makeText(UIUtils.getContext(), "id密码不匹配", Toast.LENGTH_LONG).show();
                } else if (result.getResultCode() == User.RESULT_RESPONSE_FAILED) {
                    Toast.makeText(UIUtils.getContext(), "服务器连接失败", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void clearAndResetFocusListener() {
        mEtId.setOnFocusChangeListener(null);
        mEtPwd.setOnFocusChangeListener(null);
        mEtUserName.setOnFocusChangeListener(null);

        mEtId.clearFocus();
        mEtPwd.clearFocus();
        mEtUserName.clearFocus();

        setFocusListener();
    }

    private void setFocusListener() {
        mEtUserName.setOnFocusChangeListener(new FocusListener(mTvUserName));
        mEtPwd.setOnFocusChangeListener(new FocusListener(mTvPwd));
        mEtId.setOnFocusChangeListener(new FocusListener(mTvId));
    }

    private boolean mRegistering = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_icon:
                //选择头像
                Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
                intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intentToPickPic, REQUEST_CODE_PICK_PHOTO);
                break;
            case R.id.tv_switch:
                removePopupWindow();
                showRegisterLayout();
                resetAll();
                break;
            case R.id.btn_confirm:
                String id = getId();
                String pwd = getPwd();
                String userName = getUserName();
                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(pwd) && (!mRegistering || !TextUtils.isEmpty(userName))) {
                    if (mRegistering) {
                        //注册
                        Log.e("btn_confirm", "onClick: 注册");
                        if (TextUtils.isEmpty(getPhotoPath())) {
                            Toast.makeText(UIUtils.getContext(), "头像不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showProgress();
                        NetHelper.uploadImage(LoginUtils2.UPLOAD_URL, photoPath, new NetHelper.OnUploadResultCallback() {
                            @Override
                            public void onFailure(Call call) {
                                UIUtils.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        removeProgress();
                                        Toast.makeText(UIUtils.getContext(), "onFailure", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onResponse(Call call, Response response) {
                                if (response.isSuccessful()) {
                                    mPresenter.register(new UserPresenter.OnRegisterResult() {
                                        @Override
                                        public void onResult(boolean result) {
                                            removeProgress();
                                            if (result) {
                                                //登录
                                                Toast.makeText(UIUtils.getContext(), "注册成功", Toast.LENGTH_LONG).show();
                                                login();
                                            } else {
                                                Toast.makeText(UIUtils.getContext(), "注册失败", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        //登录
                        Log.e("btn_confirm", "onClick: 登录");
                        login();
                    }
                } else {
                    //弹窗
                    Toast.makeText(LoginActivity2.this, "帐号密码不能为空", Toast.LENGTH_SHORT).show();
                    showWarming();
                    showPopWindow();
                }
                break;
        }
    }

    private void showPopWindow() {
        boolean pwd = mEtPwd.isFocused();
        boolean id = mEtId.isFocused();
        boolean userName = mEtUserName.isFocused();
        View target = pwd ? mPwdWarming : (id ? mIdWarming : (userName ? mNameWarming : null));

        if (target != null) {
            if (target.getVisibility() != View.VISIBLE) {
                return;
            }
            if (mPopupWindow == null) {
                mPopupWindow = new PopupWindow(this);
                View view = View.inflate(this, R.layout.popup_window_layout, null);
                mTvPopup = view.findViewById(R.id.text);
                mPopupWindow.setBackgroundDrawable(null);
                mPopupWindow.setContentView(view);
            }

            if (mTvPopup != null) {
                String text = (pwd ? mTvPwd.getText() : (id ? mTvId.getText() : mTvUserName.getText())).toString() + "不能为空";
                mTvPopup.setText(text);
            }

            mPopupWindow.showAsDropDown(target);
        }
    }

    private void removePopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private void resetAll() {
        mEtUserName.setText("");
        mEtId.setText("");
        mEtPwd.setText("");

        clearAndResetFocusListener();

        mTvUserName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTvUserName.setTextColor(0xFFCCCCCC);
        mTvId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTvId.setTextColor(0xFFCCCCCC);
        mTvPwd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTvPwd.setTextColor(0xFFCCCCCC);

        //重新设置位置
        mTvId.setTranslationY(0);
        mTvPwd.setTranslationY(0);
        mTvUserName.setTranslationY(0);

        mNameWarming.setVisibility(View.GONE);
        mIdWarming.setVisibility(View.GONE);
        mPwdWarming.setVisibility(View.GONE);
    }

    private void showWarming() {
        mIdWarming.setVisibility(TextUtils.isEmpty(getId()) ? View.VISIBLE : View.GONE);
        mNameWarming.setVisibility(TextUtils.isEmpty(getUserName()) ? View.VISIBLE : View.GONE);
        mPwdWarming.setVisibility(TextUtils.isEmpty(getPwd()) ? View.VISIBLE : View.GONE);
    }

    private void showRegisterLayout() {
        if (!mRegistering) {
            //显示选择头像文字、用户名布局、修改按钮和切换文字
            mTvIcon.setVisibility(View.VISIBLE);
            mLayoutRegister.setVisibility(View.VISIBLE);
            mMBtnConfirm.setText("注册");
            mTvSwitch.setText("已有账户?去登录");
            photoPath = null;
            mIvIcon.setOnClickListener(this);
            Glide.with(this).load(R.drawable.select_picture).into(mIvIcon);
        } else {
            //隐藏
            mTvIcon.setVisibility(View.GONE);
            mLayoutRegister.setVisibility(View.GONE);
            mMBtnConfirm.setText("登录");
            mTvSwitch.setText("没有账户?去注册");
            mIvIcon.setOnClickListener(null);
            Glide.with(this).load(R.mipmap.ic_launcher_round).into(mIvIcon);
        }
        mRegistering = !mRegistering;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                Uri uri = data.getData();
                photoPath = FileUtils.getFilePathByUri(this, uri);

                if (!TextUtils.isEmpty(photoPath)) {
                    RequestOptions requestOptions1 = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).circleCrop();
                    //将照片显示在 ivImage上
                    Glide.with(this).load(photoPath).apply(requestOptions1).into(mIvIcon);
                }
                break;
        }
    }

    private void colorAnima(TextView textView, int startColor, int endColor) {
        ObjectAnimator animator = ObjectAnimator.ofObject(textView, "textColor", new TypeEvaluator() {
            @Override
            public Object evaluate(float fraction, Object startValue, Object endValue) {
                int startInt = (Integer) startValue;
                int startA = (startInt >> 24) & 0xff;
                int startR = (startInt >> 16) & 0xff;
                int startG = (startInt >> 8) & 0xff;
                int startB = startInt & 0xff;

                int endInt = (Integer) endValue;
                int endA = (endInt >> 24) & 0xff;
                int endR = (endInt >> 16) & 0xff;
                int endG = (endInt >> 8) & 0xff;
                int endB = endInt & 0xff;

                return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                        (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                        (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                        (int) ((startB + (int) (fraction * (endB - startB))));
            }
        }, startColor, endColor);
        animator.setDuration(100);
        animator.start();
//        return animator;
    }

    private float getYDistance() {
        if (mYDistance == 0) {
            int smallHeight = heigthInSmallSize();
            Log.e("zyc", "mTvId.getY(): " + mTvId.getY() + "---mEtId.getY()" + mEtId.getY() + "--smallHeight: " + smallHeight);
            mYDistance = mTvId.getY() - mEtId.getY() + smallHeight;
        }
        return mYDistance;
    }

    private int heigthInSmallSize() {
        TextView textView = new TextView(this);
        ViewGroup.LayoutParams lp = mTvUserName.getLayoutParams();
        textView.setLayoutParams(lp);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setText(mTvUserName.getText());

        textView.measure(0, 0);
        return textView.getMeasuredHeight();
    }

    private ObjectAnimator scale(final TextView view, float from, float to) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "textSize", from, to);
//        objectAnimator.setDuration(100);
//        objectAnimator.start();
        return objectAnimator;
//        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(from, to);
//        scaleAnimator.setDuration(100);
//        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float value = (float) animation.getAnimatedValue();
//                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
//            }
//        });
//        scaleAnimator.start();
    }

    private ObjectAnimator translateY(final View view, final float disY) {
        ObjectAnimator translationY;
        if (disY < 0) {
            translationY = ObjectAnimator.ofFloat(view, "translationY", 0, disY);
        } else {
            translationY = ObjectAnimator.ofFloat(view, "translationY", -disY, 0);
        }
//        translationY.setDuration(100);
//        translationY.start();
        return translationY;
    }

    @Override
    public String getId() {
        return mEtId.getText().toString();
    }

    @Override
    public String getPwd() {
        return mEtPwd.getText().toString();
    }

    @Override
    public String getUserName() {
        return mEtUserName.getText().toString();
    }

    @Override
    public String getPhotoPath() {
        return photoPath;
    }

    class Watcher implements TextWatcher {
        private View mView;

        public Watcher(View view) {
            mView = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0 && mView != null && mView.getVisibility() != View.GONE) {
                mView.setVisibility(View.GONE);
                removePopupWindow();
            }
        }
    }

    class FocusListener implements View.OnFocusChangeListener {
        private TextView mView;

        public FocusListener(TextView view) {
            mView = view;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            float yDistance = getYDistance();
            String content = ((EditText) v).getText().toString();
            if (hasFocus) {
                colorAnima(mView, mOriginalColor, mColorAccent);
                if (TextUtils.isEmpty(content)) {
                    ObjectAnimator translateY = translateY(mView, -yDistance);
                    ObjectAnimator scale = scale(mView, 18, 14);
                    AnimatorSet set = new AnimatorSet();
                    set.setDuration(100);
                    set.play(translateY).with(scale);
                    set.start();
                    showPopWindow();
                }
            } else {
                removePopupWindow();
                colorAnima(mView, mColorAccent, mOriginalColor);
                if (TextUtils.isEmpty(content)) {
                    ObjectAnimator translateY = translateY(mView, yDistance);
                    ObjectAnimator scale = scale(mView, 14, 18);
                    AnimatorSet set = new AnimatorSet();
                    set.setDuration(100);
                    set.play(translateY).with(scale);
                    set.start();
                }
            }
        }
    }
}
