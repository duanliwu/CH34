package com.example.ch34demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ch34demo.adapter.DeviceAdapter;
import com.example.ch34demo.entity.DeviceEntity;
import com.example.ch34demo.entity.ModemErrorEntity;
import com.example.ch34demo.entity.SerialEntity;
import com.example.ch34demo.ui.CustomTextView;
import com.example.ch34demo.ui.DeviceListDialog;
import com.example.ch34demo.ui.GPIODialog;
import com.example.ch34demo.utils.FormatUtil;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.callback.IDataCallback;
import cn.wch.uartlib.callback.IModemStatus;
import cn.wch.uartlib.callback.IUsbStateChange;
import cn.wch.uartlib.chipImpl.SerialErrorType;
import cn.wch.uartlib.chipImpl.type.ChipType2;
import cn.wch.uartlib.exception.ChipException;
import cn.wch.uartlib.exception.NoPermissionException;
import cn.wch.uartlib.exception.UartLibException;

public class MainActivity extends AppCompatActivity {
    RecyclerView deviceRecyclerVIew;
    DeviceAdapter deviceAdapter;
    private Context context;

    //接收区
    TextView readBuffer;
    CustomTextView clearRead;
    SwitchCompat scRead;
    TextView readCount;
    //保存各个串口的接收计数
    HashMap<String, Integer> readCountMap=new HashMap<>();
    //已打开的设备列表
    final Set<UsbDevice> devices= Collections.synchronizedSet(new HashSet<UsbDevice>());
    //读线程
    Thread readThread;
    boolean flag=false;

    //接收文件测试。文件默认保存在-->内部存储\Android\data\cn.wch.wchuartdemo\files\下
    private static boolean FILE_TEST=false;

    public static final File SNAPSHOT = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "/logText");
    private File mFile;


    //lyra
    private static final String TAG = "MainActivity";

//    static {
//        System.loadLibrary("lyra_android_example");
//    }



    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLE_RATE = 16000;
    private static final int LYRA_NUM_RANDOM_FEATURE_VECTORS = 10000;
    private static final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private boolean hasStartedDecode = false;
    private boolean isRecording = false;
    private String weightsDirectory;
    private AudioRecord record;
    private AudioTrack player;
    private short[] micData;
    private int micDataShortsWritten;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.context=this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(!UsbFeatureSupported()){
            showToast("系统不支持USB Host功能");
            System.exit(0);
            return;
        }

        CrashReport.initCrashReport(getApplicationContext(), "729c62d995", true);

        initUI();
//        readThread=new ReadThread();
//        readThread.start();


        //lyra
        // Populate the bits per second dropdown widget.
//        Spinner spinner = (Spinner) findViewById(R.id.bps_spinner);
//        Integer[] bpsArray = new Integer[]{3200, 6000, 9200};
//        ArrayAdapter<Integer> adapter =
//                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bpsArray);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//
//        // The weights are stored inside of the APK as assets for this demo, but
//        // the Lyra library requires them to live in files.
//        // This helper function copies the assets to files.
//        // It is not necessarily the case that you should have the weights as assets.
//        // For example, your application might download the weights from a server
//        // instead, in which case they would only exist as files.
//        weightsDirectory = getExternalFilesDir(null).getAbsolutePath();
//        copyWeightsAssetsToDirectory(weightsDirectory);
//
//        // This demo uses the microphone, which we need permission for.
//        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

    }


    private synchronized void recordAudioStream() {
        Log.i(TAG, "Starting recording from microphone.");

        // This example records and encodes in series, to minimize complexity.
        final int chunkSize = 1000;
        if (micData == null) {
            micData = new short[SAMPLE_RATE * 5 + chunkSize];
        }
        micDataShortsWritten = 0;
        while (isRecording) {
            // If we are not yet full, write the wav data;
            if (micDataShortsWritten <= micData.length - chunkSize) {
                int amountRead =
                        0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    amountRead = record.read(micData, micDataShortsWritten, chunkSize, AudioRecord.READ_NON_BLOCKING);
                }
                micDataShortsWritten += amountRead;
            }
        }

        // Recording has stopped.  Encoding/decoding will happen later.
        record.release();
        record = null;
        Log.i(
                TAG, "Finished recording from microphone.  Recorded " + micDataShortsWritten + " samples.");
    }

    private synchronized void encodeAndDecodeMicDataToSpeaker(int bitrate) {
        // There must be at least enough data recorded to output something useful.
        if (micDataShortsWritten == 0) {
            return;
        }
        // Whatever micData holds, encode and decode with Lyra.
        short[] decodedAudio = encodeAndDecodeSamples(micData, micDataShortsWritten, bitrate,
                weightsDirectory);

        if (decodedAudio == null) {
            Log.e(TAG, "Failed to encode and decode microphone data.");
            return;
        }

        // Create a new AudioTrack in static mode so we can write once and
        // replay it.
        AudioTrack player =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            player = new AudioTrack.Builder()
                    .setAudioAttributes(
                            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setAudioFormat(
                            new AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(SAMPLE_RATE)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                    .setBufferSizeInBytes(micData.length * 2)
                    .build();
        }

        // Skip the first quarter second because it contains transient noise.
        int shortsWritten =
                0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            shortsWritten = player.write(
                    decodedAudio,
                    0,
                    decodedAudio.length,
                    AudioTrack.WRITE_BLOCKING);
        }
        Log.i(
                TAG,
                "Wrote "
                        + shortsWritten
                        + " of total length "
                        + decodedAudio.length
                        + " samples to AudioTrack.");
        player.play();
    }

    private void stopRecording() {
        record.stop();
        isRecording = false;
        // Notify we stopped recording.
        Button button = (Button) findViewById(R.id.button_record);
        button.post(() -> button.setText(R.string.button_record));
        Button decodeButton = (Button) findViewById(R.id.button_decode);
        decodeButton.setEnabled(true);
    }

    /** Called when user taps the 'Encode/Decode To Speaker' button. */
    public void onDecodeButtonClicked(View view) {
        Log.i(TAG, "Starting decoding.");

        Button decodeButton = (Button) view;
        decodeButton.setEnabled(false);
        Button recordButton = (Button) findViewById(R.id.button_record);
        recordButton.setEnabled(false);

        Spinner bpsSpinner = (Spinner) findViewById(R.id.bps_spinner);
        int bps = Integer.parseInt(bpsSpinner.getSelectedItem().toString());
        MainActivity mainActivity = this;
        Thread thread =
                new Thread(
                        () -> {
                            encodeAndDecodeMicDataToSpeaker(bps);
                            mainActivity.runOnUiThread(
                                    () -> {
                                        decodeButton.setEnabled(true);
                                        recordButton.setEnabled(true);
                                    });
                        });
        thread.start();
    }

    /** Called when user taps the 'record microphone' button. */
    public void onMicButtonClicked(View view) {
        if (!isRecording) {
            isRecording = true;
            // Begin recording, and set the button to be a stop button.
            ((Button) view).setText(R.string.button_stop);
            Button decodeButton = (Button) findViewById(R.id.button_decode);
            decodeButton.setEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                record =
                        new AudioRecord.Builder()
                                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                                .setAudioFormat(
                                        new AudioFormat.Builder()
                                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                                .setSampleRate(SAMPLE_RATE)
                                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                                .build())
                                .setBufferSizeInBytes(1024 * 256)
                                .build();
            }
            record.startRecording();
            new Thread(this::recordAudioStream).start();
        } else {
            stopRecording();
        }
    }

    /** Called when user taps the benchmark button. */
    public void runBenchmark(View view) {
        if (!hasStartedDecode) {
            TextView tv = (TextView) findViewById(R.id.sample_text);
            Button button = (Button) view;
            button.setEnabled(false);
            tv.setText(R.string.benchmark_in_progress);
            hasStartedDecode = true;

            new Thread(
                    () -> {
                        Log.i(TAG, "Starting lyraBenchmark()");
                        // Example of a call to a C++ lyra method on a background
                        // thread.
                        lyraBenchmark(LYRA_NUM_RANDOM_FEATURE_VECTORS, weightsDirectory);
//                Log.i(TAG, "Finished lyraBenchmark()");
//                tv.post(() -> tv.setText(R.string.benchmark_finished));
//                button.post(() -> button.setEnabled(true));
//                hasStartedDecode = false;
                    })
                    .start();
        }
    }

    private void copyWeightsAssetsToDirectory(String targetDirectory) {
        try {
            AssetManager assetManager = getAssets();
            String[] files = {"lyra_config.binarypb", "lyragan.tflite",
                    "quantizer.tflite", "soundstream_encoder.tflite"};
            byte[] buffer = new byte[1024];
            int amountRead;
            for (String file : files) {

                InputStream inputStream = assetManager.open(file);
                File outputFile = new File(targetDirectory, file);

                OutputStream outputStream = new FileOutputStream(outputFile);
                Log.i(TAG, "copying asset to " + outputFile.getPath());

                while ((amountRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, amountRead);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying assets", e);
        }
    }

    /**
     * A method that is implemented by the 'lyra_android_example' C++ library, which is packaged with
     * this application.
     */
    public native String lyraBenchmark(int numCondVectors, String modelBasePath);

    public native short[] encodeAndDecodeSamples(
            short[] samples, int sampleLength, int bitrate, String modelBasePath);











    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maim,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId==R.id.enumDevice){
            enumDevice();
        }else if(itemId==R.id.configGPIO){
            openGPIODialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止读线程
        //stopReadThread();
        //停止文件测试
        if(FILE_TEST){
            cancelLinks();
        }
        //关闭所有连接设备
        closeAll();
        //释放资源
        WCHUARTManager.getInstance().close(this);
    }

    /**
     * 系统是否支持USB Host功能
     *
     * @return true:系统支持USB Host false:系统不支持USB Host
     */
    public boolean UsbFeatureSupported() {
        boolean bool = this.getPackageManager().hasSystemFeature(
                "android.hardware.usb.host");
        return bool;
    }

    void initUI(){
        deviceRecyclerVIew=findViewById(R.id.rvDevice);
        readBuffer=findViewById(R.id.tvReadData);
        clearRead=findViewById(R.id.tvClearRead);
        scRead=findViewById(R.id.scRead);
        readCount=findViewById(R.id.tvReadCount);
        //初始化recyclerview
        deviceRecyclerVIew.setNestedScrollingEnabled(false);
        deviceAdapter =new DeviceAdapter(this);


        deviceRecyclerVIew.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        deviceAdapter.setEmptyView(LayoutInflater.from(this).inflate(R.layout.empty_view,deviceRecyclerVIew,false));
        deviceRecyclerVIew.setAdapter(deviceAdapter);
        deviceAdapter.setActionListener(new DeviceAdapter.OnActionListener() {
            @Override
            public void onRemove(UsbDevice usbDevice) {
                removeReadDataDevice(usbDevice);
            }
        });

        readBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());
        clearRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearReadData();
            }
        });
        //监测USB插拔状态
        monitorUSBState();
        //动态申请权限
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},111);
        }

    }

    void openGPIODialog(){
        //simply,select first device
        UsbDevice device=null;
        Iterator<UsbDevice> iterator = devices.iterator();
        while (iterator.hasNext()){
            device = iterator.next();
        }
        if(device!=null){
            GPIODialog dialog=GPIODialog.newInstance(device);
            dialog.setCancelable(false);
            dialog.show(getSupportFragmentManager(),GPIODialog.class.getName());

        }

    }

    /**
     * 枚举当前所有符合要求的设备，显示设备列表
     */
    void enumDevice(){
        try {
            //枚举符合要求的设备
            ArrayList<UsbDevice> usbDeviceArrayList = WCHUARTManager.getInstance().enumDevice();
            if(usbDeviceArrayList.size()==0){
                showToast("no matched devices");
                return;
            }
            //显示设备列表dialog
            DeviceListDialog deviceListDialog=DeviceListDialog.newInstance(usbDeviceArrayList);
            deviceListDialog.setCancelable(false);
            deviceListDialog.show(getSupportFragmentManager(),DeviceListDialog.class.getName());
            deviceListDialog.setOnClickListener(new DeviceListDialog.OnClickListener() {
                @Override
                public void onClick(UsbDevice usbDevice) {
                    //选择了某一个设备打开
                    open(usbDevice);
                }
            });
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    /**
     * 从设备列表中打开某个设备
     *
     * @param usbDevice
     */
    void open(@NonNull UsbDevice usbDevice){
        openLog();

        if(WCHUARTManager.getInstance().isConnected(usbDevice)){
            showToast("当前设备已经打开");
            return;
        }
        try {
            boolean b = WCHUARTManager.getInstance().openDevice(usbDevice);
            if(b){
                //打开成功
                //更新显示的ui
                update(usbDevice);
                //初始化接收计数
                int serialCount = 0;
                try {
                    serialCount = WCHUARTManager.getInstance().getSerialCount(usbDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < serialCount; i++) {
                    readCountMap.put(FormatUtil.getSerialKey(usbDevice,i),0);
                }
                //将该设备添加至已打开设备列表,在读线程ReadThread中,将会读取该设备的每个串口数据
                addToReadDeviceSet(usbDevice);
                //用作文件对比测试,在打开每个设备时，对每个串口新建对应的保存数据的文件
                if(FILE_TEST){
                    for (int i = 0; i < serialCount; i++) {
                        linkSerialToFile(usbDevice,i);
                    }
                }
                registerModemStatusCallback(usbDevice);
                registerDataCallback(usbDevice);
            }else {
                showToast("打开失败");
            }
        } catch (ChipException e) {
            LogUtil.d(e.getMessage());
        } catch (NoPermissionException e) {
            //没有权限打开该设备
            //申请权限
            showToast("没有权限打开该设备");
            requestPermission(usbDevice);
        } catch (UartLibException e) {
            e.printStackTrace();
        }
    }

    /**
     * 申请读写权限
     * @param usbDevice
     */
    private void requestPermission(@NonNull UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().requestPermission(this,usbDevice);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    /**
     * 监测USB的状态
     */
    private void monitorUSBState(){
        WCHUARTManager.getInstance().setUsbStateListener(new IUsbStateChange() {
            @Override
            public void usbDeviceDetach(UsbDevice device) {
                //设备移除
                removeReadDataDevice(device);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //从界面上移除
                        if(deviceAdapter!=null){
                            deviceAdapter.removeDevice(device);
                        }
                    }
                });

            }

            @Override
            public void usbDeviceAttach(UsbDevice device) {
                //设备插入
            }

            @Override
            public void usbDevicePermission(UsbDevice device, boolean result) {
                //请求打开设备权限结果
            }
        });
    }

    /**
     * //recyclerView更新UI
     * @param usbDevice
     */
    void update(UsbDevice usbDevice){
        //根据vid/pid获取芯片类型
        ChipType2 chipType = null;
        try {
            chipType = WCHUARTManager.getInstance().getChipType(usbDevice);
            //获取芯片串口数目,为负则代表出错
            int serialCount = WCHUARTManager.getInstance().getSerialCount(usbDevice);
            //构建recyclerView所绑定的数据,添加设备
            ArrayList<SerialEntity> serialEntities=new ArrayList<>();
            for (int i = 0; i < serialCount; i++) {
                SerialEntity serialEntity=new SerialEntity(usbDevice,i);
                serialEntities.add(serialEntity);
            }
            DeviceEntity deviceEntity=new DeviceEntity(usbDevice,chipType.getDescription(),serialEntities);
            if(deviceAdapter.hasExist(deviceEntity)){
                //已经显示
                showToast("该设备已经存在");
            }else {
                deviceAdapter.addDevice(deviceEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 关闭所有设备
     */
    void closeAll(){
        ArrayList<UsbDevice> usbDeviceArrayList = null;
        try {
            usbDeviceArrayList = WCHUARTManager.getInstance().enumDevice();
            for (UsbDevice usbDevice : usbDeviceArrayList) {
                if(WCHUARTManager.getInstance().isConnected(usbDevice)){
                    WCHUARTManager.getInstance().disconnect(usbDevice);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addToReadDeviceSet(@NonNull UsbDevice usbDevice){
        synchronized (devices){
            devices.add(usbDevice);
        }

    }

    private void removeReadDataDevice(@NonNull UsbDevice usbDevice){
        synchronized (devices){
            devices.remove(usbDevice);
        }
    }

    private void registerModemStatusCallback(UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().registerModemStatusCallback(usbDevice, new IModemStatus() {
                @Override
                public void onStatusChanged(int serialNumber, boolean isDCDRaised, boolean isDSRRaised, boolean isCTSRaised, boolean isRINGRaised) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.updateDeviceModemStatus(usbDevice,serialNumber,isDCDRaised,isDSRRaised,isCTSRaised,isRINGRaised);
                        }
                    });
                }

                @Override
                public void onOverrunError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.OVERRUN);
                        LogUtil.d("overrun error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.OVERRUN));

                        }
                    });

                }

                @Override
                public void onParityError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.PARITY);
                        LogUtil.d("parity error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("parity error!");
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.PARITY));

                        }
                    });
                }

                @Override
                public void onFrameError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.FRAME);
                        LogUtil.d("frame error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("frame error!");
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.FRAME));

                        }
                    });
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerDataCallback(UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().registerDataCallback(usbDevice, new IDataCallback() {
                @Override
                public void onData(int serialNumber, byte[] buffer, int length) {
                    //LogUtil.d(String.format(Locale.getDefault(),"serial %d receive data %d:%s", serialNumber,length, FormatUtil.bytesToHexString(buffer, length)));
                    //1.注意回调的执行线程与调用回调方法的线程属于同一线程
                    //2.此处所在的线程将是线程池中多个端点的读取线程，可打印线程id查看
                    //3.buffer是底层数组，如果此处将其传给其他线程使用，例如通过runOnUiThread显示数据在界面上,
                    //涉及到线程切换需要一定时间，buffer可能被读到的新数据覆盖，可以新建一个临时数组保存数据

                    byte[] data=new byte[length];
                    System.arraycopy(buffer,0,data,0,data.length);
                    if(FILE_TEST){
                        updateReadDataToFile(usbDevice,serialNumber,data,length);
                    }else {
                        updateReadData(usbDevice,serialNumber,data,length);
                    }


                }
            });
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    @Deprecated
    public class ReadThread extends Thread{

        public ReadThread() {
            flag=true;
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            LogUtil.d("---------------开始读取数据");
            while (flag){
                if(devices.isEmpty()){
                    continue;
                }
                //遍历已打开的设备列表中的设备
                synchronized (devices){
                    Iterator<UsbDevice> iterator = devices.iterator();
                    while (iterator.hasNext()){
                        UsbDevice device = iterator.next();
                        try {
                            int serialCount = WCHUARTManager.getInstance().getSerialCount(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        //读取该设备每个串口的数据
//                        for (int i = 0; i < serialCount; i++) {
//                            try {
//                                byte[] bytes = WCHUARTManager.getInstance().readData(device, i);
//                                if(bytes!=null){
//                                    //使用获取到的数据
//                                    updateReadData(device,i,bytes,bytes.length);
//
//                                    //updateReadDataToFile(device,i,bytes,bytes.length);
//                                }
//                            } catch (ChipException e) {
//                                //LogUtil.d(e.getMessage());
//                                break;
//                            }
//                        }
                    }
                }
            }
            LogUtil.d("读取数据线程结束");
        }
    }

    public void stopReadThread(){
        if(readThread!=null && readThread.isAlive()){
            flag=false;
        }
    }

    private void updateReadData(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        if(buffer==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Integer integer = readCountMap.get(FormatUtil.getSerialKey(usbDevice, serialNumber));
                if(integer==null){
                    //不包含此key
                    return;
                }
                //更新计数
                integer+=length;
                readCountMap.put(FormatUtil.getSerialKey(usbDevice, serialNumber),integer);
                //

                String result="";
                if (readBuffer.getText().toString().length() >= 1500) {
                    readBuffer.setText("");
                    readBuffer.scrollTo(0, 0);
                }

                if(scRead.isChecked()){
                    result= FormatUtil.bytesToHexString(buffer, length);

                }else {
                    result=new String(buffer,0,length);
                }
                String readBufferLogPrefix = FormatUtil.getReadBufferLogPrefix(usbDevice, serialNumber,integer);

                Date currentDate = new Date(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String  time = dateFormat.format(currentDate);


                if(result.length()>1){
                    InputFile(time + readBufferLogPrefix +result);
                }


                //LogUtil.d(readBufferLogPrefix);
                readBuffer.append( time + readBufferLogPrefix+ result+"\r\n");

                int offset = readBuffer.getLineCount() * readBuffer.getLineHeight();
                //int maxHeight = usbReadValue.getMaxHeight();
                int height = readBuffer.getHeight();
                //USBLog.d("offset: "+offset+"  maxHeight: "+maxHeight+" height: "+height);
                if (offset > height) {
                    //USBLog.d("scroll: "+(offset - usbReadValue.getHeight() + usbReadValue.getLineHeight()));
                    readBuffer.scrollTo(0, offset - readBuffer.getHeight() + readBuffer.getLineHeight());
                }
            }
        });
    }


    private void clearReadData(){
        readBuffer.scrollTo(0,0);
        readBuffer.setText("");
        for (String s : readCountMap.keySet()) {
            readCountMap.put(s,0);
        }
    }

    private void showToast(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //ToastUtil.create(context,message).show();
                Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            }
        });
    }
    ///////////////////////////////////////将数据保存至文件,与发送文件对比测试////////////////////////////////////////////////////

    //该Map的key是每个设备的串口，value是其对应的保存数据的文件的fileStream
    private HashMap<String, FileOutputStream> fileOutputStreamMap=new HashMap<>();

    //用作文件对比测试,在打开每个设备时，每个串口都新建对应的保存数据的文件，其映射关系保存到fileOutputStreamMap中
    private void linkSerialToFile(UsbDevice usbDevice,int serialNumber){
        LogUtil.d("linkSerialToFile:");
        File testFile = getExternalFilesDir("TestFile");
        File file=new File(testFile,WCHUARTManager.getInstance().getChipType(usbDevice).toString()+"_"+serialNumber+".txt");
        if(file.exists()){
            file.delete();
        }
        try {
            boolean ret = file.createNewFile();
            LogUtil.d("新建文件:"+ret);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            if(!fileOutputStreamMap.containsKey(FormatUtil.getSerialKey(usbDevice,serialNumber))){
                fileOutputStreamMap.put(FormatUtil.getSerialKey(usbDevice,serialNumber),fileOutputStream);
            }
        } catch (IOException e) {
            LogUtil.d(e.getMessage());
        }

    }

    //将接收到的数据保存至文件，用作对比
    private void updateReadDataToFile(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        updateToFile(usbDevice, serialNumber, buffer, length);
    }

    private void updateToFile(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        if(fileOutputStreamMap.containsKey(FormatUtil.getSerialKey(usbDevice,serialNumber))){
            FileOutputStream fileOutputStream = fileOutputStreamMap.get(FormatUtil.getSerialKey(usbDevice, serialNumber));
            try {
                fileOutputStream.write(buffer,0,length);
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //结束保存至文件的功能,关闭Stream
    private void cancelLinks(){
        for (String s : fileOutputStreamMap.keySet()) {
            FileOutputStream fileOutputStream = fileOutputStreamMap.get(s);
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //hex转字符串
    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int count = 0; count < hex.length() - 1; count += 2) {
            String output = hex.substring(count, (count + 2));    //grab the hex in pairs
            int decimal = Integer.parseInt(output, 16);    //convert hex to decimal
            sb.append((char) decimal);    //convert the decimal to character
        }
        return sb.toString();
    }

    //日志

    public void openLog(){

        //创建文件夹
        if (!SNAPSHOT.exists()) {
            SNAPSHOT.mkdirs();
        }

        mFile = new File(getExternalFilesDir(null), "LogText.txt");
        //判断文件是否存在，存在就删除
//        if (mFile.exists()) {
//            mFile.delete();
//        }
        try {
            //创建文件
            mFile.createNewFile();
            Log.i("文件创建", "文件创建成功");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void InputFile(String  msgStr) {

        // 获取应用的私有目录
        File privateFolder = getExternalFilesDir(null);
        // 创建文件对象
        File mFile = new File(privateFolder, "logsText.txt");

        try {
            // 创建 FileWriter 对象，将第二个参数设为 true 表示追加写入
            FileWriter writer = new FileWriter(mFile, true);

//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String currentTime = dateFormat.format(new Date());

            // 将文本内容写入文件
            writer.write( msgStr+ "\n\n");
            writer.write("\n"); // 可选：每次写入后换行

            // 关闭 FileWriter
            writer.close();

            Log.i("写入文件", "文本内容已成功写入到文件中");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}