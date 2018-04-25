package com.anrongtec.ocr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anrongtec.ocr.utils.NavigationBarHeightUtils;
import com.anrongtec.ocr.utils.StreamEmpowerFileUtils;
import com.anrongtec.ocr.utils.UserIdUtils;
import com.anrongtec.ocr.view.PLViewfinderView;
import com.etop.plate.PlateAPI;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * @author huiliu
 */
public class CarActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String PATH = Environment.getExternalStorageDirectory() + "/alpha/Plate/";
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private RelativeLayout mainRl;
    private SurfaceHolder surfaceHolder;
    private PlateAPI plApi = null;
    private Bitmap bitmap;
    private int preWidth = 0;
    private int preHeight = 0;
    private int screenWidth;
    private int screenHeight;
    private Vibrator mVibrator;
    private byte[] tackData;
    private PLViewfinderView myView;
    private long recogTime;
    private boolean isFatty = false;
    private boolean bInitKernal = false;
    private AlertDialog alertDialog = null;
    private int[] m_ROI = {0, 0, 0, 0};
    private boolean isROI = false;
    private ImageButton ibnBack;
    private ImageButton ibnFlash;
    private boolean baddView = false;
    private SeekBar mSeekBar;
    private TextView mTvRoll;
    private float oldDist = 1f;

    private Camera.Parameters params;

    //相机放大最大值
    private int maxZoom;
    //根据相机最大值不同,获取每次加减数值
    private int countZoom;
    //屏幕宽
    private int width;
    //屏幕高
    private int height;
    //首次移动手指距离
    private int firstMove;
    //非首次移动手指距离
    private int everyMove;
    //滑动间隔时间
    private final static float TIME = 100;
    //上次滑动时间
    private float oldTime;
    //是否是每次的第一次滑动
    private boolean isFirstMove = false;
    //改变seekBarUI
    private final static int CHANGE_SEEKBAR = 1;
    //当前进度条
    private int nowProgress;
    //通过手指缩放改变seekbar位置
    private Handler seekBarHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CHANGE_SEEKBAR:
                    mSeekBar.setProgress((Integer) msg.obj);
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 屏幕常亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main_etop);
        findView();
        /******************* 更变内容 *******************/
        //获取当前屏幕分辨率
        getWindowDpi();
        /******************* 更变内容 *******************/
    }

    /******************* 更变内容 *******************/
    /**
     * 获取当前屏幕分辨率,并设置移动速度
     */
    private void getWindowDpi() {
        DisplayMetrics metrics = new DisplayMetrics();
        /**
         * getRealMetrics - 屏幕的原始尺寸，即包含状态栏。
         * version >= 4.2.2
         */
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        //根据分辨率不同,改变手指移动距离(灵敏度)
        if(width == 1080 && height == 1920){
            //通用分辨率1
            // TODO: 2017/12/1 如果速度不合适，请调整首次缩放手指移动距离和连续缩放手指移动距离
            firstMove = 150;
            everyMove = 70;
        }else if(width == 720 && height == 1280){
            //通用分辨率2
            // TODO: 2017/12/1 如果速度不合适，请调整首次缩放手指移动距离和连续缩放手指移动距离
            firstMove = 70;
            everyMove = 40;
        }else{
            //其他分辨率
            // TODO: 2017/12/1 可增加其他分辨率机型
            firstMove = 70;
            everyMove = 40;
        }
    }
    /******************* 更变内容 *******************/

    private void findView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.etop_sv_main);
        mainRl = (RelativeLayout) findViewById(R.id.etop_rl_main);
        ibnBack = (ImageButton) findViewById(R.id.etop_ibn_back);
        ibnFlash = (ImageButton) findViewById(R.id.etop_ibn_flash);
        mSeekBar = (SeekBar) findViewById(R.id.etop_seekbar);
        mTvRoll = (TextView) findViewById(R.id.etop_tv_roll);


        try {
            StreamEmpowerFileUtils.copyDataBase(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //获取设置的配置信息
        Configuration cf = this.getResources().getConfiguration();
        int noriention = cf.orientation;

        if (noriention == Configuration.ORIENTATION_PORTRAIT) {
            initOCRKernal();//初始化识别核心
        }
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        // 屏幕宽度（像素）
        screenWidth = metric.widthPixels;
        // 屏幕高度（像素）
        screenHeight = metric.heightPixels;
        if (screenWidth * 3 == screenHeight * 4) {
            isFatty = true;
        }


        surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(CarActivity.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (alertDialog == null) {
            alertDialog = new Builder(this).create();
        }

        File file = new File(PATH);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        mOnClick();
    }

    private void mOnClick() {
        ibnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ibnFlash.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    String mess = "当前设备不支持闪光灯";
                    Toast.makeText(getApplicationContext(), mess, Toast.LENGTH_SHORT).show();
                } else {
                    if (mCamera != null) {
                        params = mCamera.getParameters();
                        String flashMode = params.getFlashMode();
                        if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            params.setExposureCompensation(0);
                        } else {
                            // 闪光灯常亮
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            params.setExposureCompensation(-1);
                        }
                        try {
                            mCamera.setParameters(params);
                        } catch (Exception e) {
                            String mess = "当前设备不支持闪光灯";
                            Toast.makeText(getApplicationContext(), mess, Toast.LENGTH_SHORT).show();
                        }
                        mCamera.startPreview();
                    }
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initOCRKernal();//如果初始化核心失败，重新初始化
        if (alertDialog == null) {
            alertDialog = new Builder(this).create();
        }
        if (mCamera == null) {
            try {
                /******************* 更变内容 *******************/
                mCamera = Camera.open();
                //获取maxZoom和countZoom数值
                getMaxZoom();
                mSeekBar.setOnSeekBarChangeListener(onZoomChangeListener);
                /******************* 更变内容 *******************/
            } catch (Exception e) {
                e.printStackTrace();
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
                String mess = getResources().getString(R.string.toast_camera);
                Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        initCamera(holder);
    }

    /******************* 更变内容 *******************/
    /**
     * 获取maxZoom和countZoom数值并调整相机初始缩放大小
     */
    private void getMaxZoom() {
        params = mCamera.getParameters();
        if (params.isZoomSupported()) {
            maxZoom = params.getMaxZoom();
            countZoom = Math.round(maxZoom / 10);
            mSeekBar.setMax(maxZoom);
            nowProgress = (int) Math.round(maxZoom * 0.2);
            mSeekBar.setProgress(nowProgress);
            mCamera.setParameters(params);
            mTvRoll.setText((nowProgress) + ".0x");
        }
        //设置进入时相机初始倍数，max的20%
        params.setZoom(nowProgress);
        mCamera.cancelAutoFocus();
        mCamera.setParameters(params);
    }
    /******************* 更变内容 *******************/

    @Override
    public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCamera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        synchronized (camera) {
                            new Thread() {
                                @Override
                                public void run() {
                                    initCamera(holder);
                                    super.run();
                                }
                            }.start();
                        }
                         mCamera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        releaseCamera();//相机资源释放
        //卸载识别核心
        if (plApi != null) {
            plApi.ETUnInitPlateKernal();
            plApi = null;
        }
    }


    /********************初始化识别核心**********************/
    private void initOCRKernal() {
        if (plApi == null) {
            plApi = new PlateAPI();
            String cacheDir = (this.getExternalCacheDir()).getPath();
            String userIdPath = cacheDir + "/" + UserIdUtils.UserID + ".lic";
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            int nRet = plApi.ETInitPlateKernal("", userIdPath, UserIdUtils.UserID, 0x06, 0x02, telephonyManager, this);
            if (nRet != 0) {
                Toast.makeText(getApplicationContext(), "激活失败", Toast.LENGTH_SHORT).show();
                System.out.print("nRet=" + nRet);
                bInitKernal = false;
            } else {
                System.out.print("nRet=" + nRet);
                bInitKernal = true;
            }
        }
    }

    @TargetApi(14)
    private void initCamera(SurfaceHolder holder) {
        params = mCamera.getParameters();
        List<Size> list = params.getSupportedPreviewSizes();
        Size size;
        int length = list.size();
        Size tmpsize = list.get(0);
        int navigationBarHeight = NavigationBarHeightUtils.getNavigationBarHeight(this);
        tmpsize = getOptimalPreviewSize(list, screenHeight + navigationBarHeight, screenWidth);

        int previewWidth = list.get(0).width;
        int previewheight = list.get(0).height;
        previewWidth = tmpsize.width;
        previewheight = tmpsize.height;
        int second_previewWidth = 0;
        int second_previewheight = 0;

        if (length == 1) {
            preWidth = previewWidth;
            preHeight = previewheight;
        } else {
            second_previewWidth = previewWidth;
            second_previewheight = previewheight;
            for (int i = 0; i < length; i++) {
                size = list.get(i);
                if (size.height > 700) {
                    if (size.width * previewheight == size.height * previewWidth && size.height < second_previewheight) {
                        second_previewWidth = size.width;
                        second_previewheight = size.height;
                    }
                }
            }
            preWidth = second_previewWidth;
            preHeight = second_previewheight;
        }
        if (!isROI) {
            int t;
            int b;
            int l;
            int r;
            l = screenHeight / 5;
            r = screenHeight * 3 / 5;
            t = 4;
            b = screenWidth - 4;
            double proportion = (double) screenHeight / (double) preWidth;
            l = (int) (l / proportion);
            t = 0;
            r = (int) (r / proportion);
            b = preHeight;
            int borders[] = {l, t, r, b};
            m_ROI[0] = l;
            m_ROI[1] = t;
            m_ROI[2] = r;
            m_ROI[3] = b;
            plApi.ETSetPlateROI(borders, preWidth, preHeight);
            isROI = true;
        }
        if (!baddView) {
            if (isFatty) {
                myView = new PLViewfinderView(this, screenWidth, screenHeight, isFatty);
            } else {
                myView = new PLViewfinderView(this, screenWidth, screenHeight);
            }
            mainRl.addView(myView);
            baddView = true;
        }

        params.setPreviewSize(preWidth, preHeight);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        mCamera.setPreviewCallback(this);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        if ("500CT".equals(Build.MODEL)) {
            mCamera.setDisplayOrientation(270);
        }
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    private SeekBar.OnSeekBarChangeListener onZoomChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            /******************* 更变内容 *******************/
            params = mCamera.getParameters();
            params.setZoom(progress);
            if (params.getMaxZoom() >= progress) {
                mSeekBar.setProgress(progress);
            } else {
                Toast.makeText(getApplicationContext(), "已达到最高倍数" + params.getMaxZoom(), Toast.LENGTH_SHORT).show();
            }
            mTvRoll.setText((progress) + ".0x");
            Log.d("progress", String.valueOf(progress));
            mCamera.cancelAutoFocus();
            mCamera.setParameters(params);
            /******************* 更变内容 *******************/
        }

    };

    public String pictureName() {
        String str = "";
        Time t = new Time();
        t.setToNow(); // 取得系统时间。
        int year = t.year;
        int month = t.month + 1;
        int date = t.monthDay;
        int hour = t.hour; // 0-23
        int minute = t.minute;
        int second = t.second;
        if (month < 10) {
            str = String.valueOf(year) + "0" + String.valueOf(month);
        } else {
            str = String.valueOf(year) + String.valueOf(month);
        }
        if (date < 10) {
            str = str + "0" + String.valueOf(date + "_");
        } else {
            str = str + String.valueOf(date + "_");
        }
        if (hour < 10) {
            str = str + "0" + String.valueOf(hour);
        } else {
            str = str + String.valueOf(hour);
        }
        if (minute < 10) {
            str = str + "0" + String.valueOf(minute);
        } else {
            str = str + String.valueOf(minute);
        }
        if (second < 10) {
            str = str + "0" + String.valueOf(second);
        } else {
            str = str + String.valueOf(second);
        }
        return str;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        tackData = data;
        params = camera.getParameters();
        int buffl = 256;
        char recogval[] = new char[buffl];
        Long timeStart = System.currentTimeMillis();
        if (!alertDialog.isShowing()) {
            int pLineWarp[] = new int[800 * 45];
            int nv21Width = params.getPreviewSize().width;
            int nv21Height = params.getPreviewSize().height;
            int r = plApi.RecognizePlateNV21(data, 1, nv21Width, nv21Height, recogval, buffl, pLineWarp);
            Long timeEnd = System.currentTimeMillis();
            if (r == 0) {
                // 震动
                recogTime = (timeEnd - timeStart);
                String plateNo = plApi.GetRecogResult(0);
                String plateColor = plApi.GetRecogResult(1);
                mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                mVibrator.vibrate(50);
                // 删除正常识别保存图片功能
                int[] datas = convertYUV420_NV21toARGB8888(tackData, params.getPreviewSize().width,
                        params.getPreviewSize().height);

                Intent intent = new Intent();
                if (!TextUtils.isEmpty(plateNo)) {
                    intent.putExtra("carResult", plateNo);
                } else {
                    intent.putExtra("carResult", "");
                }
                setResult(RESULT_OK, intent);
                finish();

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inInputShareable = true;
                opts.inPurgeable = true;
                bitmap = Bitmap.createBitmap(datas, params.getPreviewSize().width,
                        params.getPreviewSize().height, Bitmap.Config.RGB_565);
                Bitmap tmpbitmap = Bitmap.createBitmap(bitmap, m_ROI[0], m_ROI[1], m_ROI[2] - m_ROI[0], m_ROI[3] - m_ROI[1]);
                System.out.println("m_ROI:" + m_ROI[0] + " " + m_ROI[1] + " " + m_ROI[2] + " " + m_ROI[3]);
                String strFilePath = PATH + "Plate_" + pictureName() + ".jpg";
                plApi.SavePlateImg(strFilePath, 0);
                String strFileRROIPath = PATH + "Plate_ROI_" + pictureName() + ".jpg";
                plApi.SavePlateImg(strFileRROIPath, 1);
                //通知相册更新图片
                CarActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(strFilePath))));
                CarActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(strFileRROIPath))));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alertDialog != null) {
            alertDialog.cancel();
            alertDialog.dismiss();
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        releaseCamera();//释放相机资源

        /************释放识别核心************/
        if (plApi != null) {
            plApi.ETUnInitPlateKernal();
            plApi = null;
        }
    }

    /************相机用完资源释放*************/
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public int[] convertYUV420_NV21toARGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i along Y and the final pixels
        // k along pixels U and V
        for (int i = 0, k = 0; i < size; i += 2, k += 2) {
            y1 = data[i] & 0xff;
            y2 = data[i + 1] & 0xff;
            y3 = data[width + i] & 0xff;
            y4 = data[width + i + 1] & 0xff;

            u = data[offset + k] & 0xff;
            v = data[offset + k + 1] & 0xff;
            u = u - 128;
            v = v - 128;

            pixels[i] = convertYUVtoARGB(y1, u, v);
            pixels[i + 1] = convertYUVtoARGB(y2, u, v);
            pixels[width + i] = convertYUVtoARGB(y3, u, v);
            pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);

            if (i != 0 && (i + 2) % width == 0) {
                i += width;
            }
        }

        return pixels;
    }

    private int convertYUVtoARGB(int y, int u, int v) {
        int r, g, b;

        r = y + (int) 1.402f * u;
        g = y - (int) (0.344f * v + 0.714f * u);
        b = y + (int) 1.772f * v;
        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        int nminwh = (w > h ? h : w);
        int nthresh = nminwh >= 700 ? 700 : nminwh;
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (size.height < nthresh) {
                continue;
            }
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (size.height < nthresh) {
                    continue;
                }
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                } else if (Math.abs(size.height - targetHeight) == minDiff) {
                    optimalSize = size;
                }
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                } else if (Math.abs(size.height - targetHeight) == minDiff) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }

    /**
     * 处理放大缩小
     *
     * @param isZoomIn
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        params = camera.getParameters();
        if (params.isZoomSupported()) {
            /******************* 更变内容 *******************/
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom = zoom + countZoom > maxZoom ? maxZoom : zoom + countZoom;
                seekBarHandler.obtainMessage(CHANGE_SEEKBAR, zoom).sendToTarget();
            } else if (!isZoomIn && zoom > 0) {
                zoom = zoom - countZoom < 0  ? 0 : zoom - countZoom;
                seekBarHandler.obtainMessage(CHANGE_SEEKBAR, zoom).sendToTarget();
            }else if ((isZoomIn && zoom == maxZoom) || (!isZoomIn && zoom == 0)){

            }
            /******************* 更变内容 *******************/
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = getFingerSpacing(event);
                oldTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = getFingerSpacing(event);
                /******************* 更变内容 *******************/
                //当前滑动时间
                float newTime = System.currentTimeMillis();
                //如果滑动间隔大于默认值，则认定不是连续滑动
                if(newTime - oldTime > TIME){
                    isFirstMove = false;
                }
                //如果是间隔后第一次滑动,并且首次手指移动的距离满足像素要求,则处理
                if (isFirstMove && Math.abs(newDist - oldDist) > firstMove){
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    oldTime = newTime;
                    return true;
                }
                //如果是连续滑动中,并且手指移动距离满足像素要求,则处理
                if (newTime - oldTime < TIME && Math.abs(newDist - oldDist) > everyMove){
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    oldTime = newTime;
                    isFirstMove = true;
                }
                /******************* 更变内容 *******************/
                break;
            default:
                break;
        }
        return true;
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = 0;
        float y = 0;
        try {
            x = event.getX(0) - event.getX(1);
            y = event.getY(0) - event.getY(1);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return (float) Math.sqrt(x * x + y * y);
    }
}


