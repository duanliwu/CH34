package com.example.ch34demo.adapter;

import static android.content.ContentValues.TAG;

import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ch34demo.LogUtil;
import com.example.ch34demo.R;
import com.example.ch34demo.WCHApplication;
import com.example.ch34demo.entity.ModemEntity;
import com.example.ch34demo.entity.ModemErrorEntity;
import com.example.ch34demo.entity.SerialBaudBean;
import com.example.ch34demo.entity.SerialEntity;
import com.example.ch34demo.ui.CustomTextView;
import com.example.ch34demo.ui.SerialConfigDialog;
import com.example.ch34demo.utils.FormatUtil;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.chipImpl.SerialErrorType;

public class SerialListAdapter extends RecyclerView.Adapter<SerialListAdapter.MyViewHolder> {

    private FragmentActivity activity;
    private ArrayList<SerialEntity> serialEntities;
    private HashMap<Integer, Integer> writeCountMap;

    private String ATStr;
    private Timer mTimer;
    private TimerTask mTimerTask;

    private  MyViewHolder MyViewHolder;
    private SerialEntity serialEntities2;
    private  int timer = 1000;
    private  String SendMsgStr;


    private Handler handler=new Handler(Looper.getMainLooper());

    public SerialListAdapter(@NonNull FragmentActivity activity, @NonNull ArrayList<SerialEntity> serialEntities) {
        this.activity=activity;
        this.serialEntities = serialEntities;
        writeCountMap=new HashMap<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(activity).inflate(R.layout.serial_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        serialEntities2 = serialEntities.get(position);

        SerialEntity serialEntity = serialEntities.get(position);
        writeCountMap.put(serialEntity.getSerialNumber(),0);
        holder.tvDescription.setText(String.format(Locale.getDefault(),"串口%d",serialEntity.getSerialNumber()));
        //设置串口
        holder.cbDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDTR(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), isChecked);
            }
        });
        holder.cbRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setRTS(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),isChecked);

            }
        });
        holder.cbBREAK.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setBreak(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),isChecked);
            }
        });

        holder.setSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SerialConfigDialog dialog=SerialConfigDialog.newInstance(null);
                dialog.setCancelable(false);
                dialog.show(activity.getSupportFragmentManager(),SerialConfigDialog.class.getName());
                dialog.setListener(new SerialConfigDialog.onClickListener() {
                    @Override
                    public void onSetBaud(SerialBaudBean data) {

                        if(setSerialParameter(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),data )){
                            holder.serialInfo.setText(data.toString());
                            showToast("设置成功");
                        }else {
                            showToast("设置失败");
                        }
                    }
                });
            }
        });
        //发送
        holder.write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                     String s =  holder.writeBuffer.getText().toString();
//                if (ATStr != null){
//                    StringBuilder hex = new StringBuilder();
//                    for (char ch : s.toCharArray()) {
//                        hex.append(Integer.toHexString((int) ch));
//                    }
//                    s = ATStr + hex.toString()+"0d0a";
//                    showToast("数据"+s);
//
//                }
//
//                if(TextUtils.isEmpty(s)){
//                    showToast("发送内容为空");
//                    return;
//                }
//                byte[] bytes = null;
//                if(holder.scWrite.isChecked()){
//                    if(!s.matches("([0-9|a-f|A-F]{2})*")){
//                        showToast("发送内容不符合HEX规范");
//                        return;
//                    }
//                    bytes= FormatUtil.hexStringToBytes(s);
//                }else {
//                    bytes = s.getBytes(StandardCharsets.UTF_8);
//                }
//                int ret = writeData(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), bytes, bytes.length);
//                if(ret>0){
//                    //更新发送计数
//                    int writeCount = getWriteCount(serialEntity.getSerialNumber());
//                    writeCount+=ret;
//                    setWriteCount(serialEntity.getSerialNumber(),writeCount);
//                    holder.writeCount.setText(String.format(Locale.getDefault(),"发送计数：%d字节",writeCount));
//                    //showToast("发送成功");
//                }else {
//                    showToast("发送失败");
//                }



                if (mTimer == null){
                    MyViewHolder = holder;
                    startTimer();
                }
            }
        });
        holder.clearWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                setWriteCount(serialEntity.getSerialNumber(),0);
                holder.writeBuffer.setText("");
                holder.writeCount.setText(String.format(Locale.getDefault(),"发送计数：%d字节",getWriteCount(serialEntity.getSerialNumber())));
            }
        });

        holder.queryError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int i1 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.OVERRUN);
                    int i2 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.PARITY);
                    int i3 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.FRAME);
                    showToast(String.format(Locale.getDefault(),"overrun error:%d parity error:%d frame error:%d ",i1,i2,i3));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });




        holder.At.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41540d0a");
            }
        });

        holder.AT_RST.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41542b5253540d0a");
            }
        });

        holder.AT_FREQ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41542b465245513d3438363830303030302c3438363830303030302c3438363830303030300d0a");

            }
        });
        holder.AT_TXP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41542b5458503d31350d0a");
            }
        });

        holder.AT_RATE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogPlus dialogPlus = DialogPlus.newDialog(v.getContext())
                        .setContentHolder(new ViewHolder(R.layout.dialog_rate_layout))
                        .setOnItemClickListener(new OnItemClickListener() {
                            @Override
                            public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                                Button button = view.findViewById(R.id.btn_7);
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                    }
                                });
                            }
                        })
                        .create();
                Button button2 = (Button) dialogPlus.findViewById(R.id.btn_2);
                Button button3 = (Button) dialogPlus.findViewById(R.id.btn_3);
                Button button4 = (Button) dialogPlus.findViewById(R.id.btn_4);
                Button button5 = (Button) dialogPlus.findViewById(R.id.btn_5);
                Button button6 = (Button) dialogPlus.findViewById(R.id.btn_6);
                Button button7 = (Button) dialogPlus.findViewById(R.id.btn_7);
                Button button8 = (Button) dialogPlus.findViewById(R.id.btn_8);
                Button button9 = (Button) dialogPlus.findViewById(R.id.btn_9);
                Button button10 = (Button) dialogPlus.findViewById(R.id.btn_10);
                Button button11 = (Button) dialogPlus.findViewById(R.id.btn_11);
                Button button12 = (Button) dialogPlus.findViewById(R.id.btn_12);
                Button button13 = (Button) dialogPlus.findViewById(R.id.btn_13);
                Button button14 = (Button) dialogPlus.findViewById(R.id.btn_14);
                Button button15 = (Button) dialogPlus.findViewById(R.id.btn_15);
                Button button16 = (Button) dialogPlus.findViewById(R.id.btn_16);
                Button button17 = (Button) dialogPlus.findViewById(R.id.btn_17);
                Button button18 = (Button) dialogPlus.findViewById(R.id.btn_18);


                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 4000;
                        holder.writeBuffer.setText("41542b524154453d320d0a");
                        dialogPlus.dismiss();
                    }
                });
                button3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 4000;
                        holder.writeBuffer.setText("41542b524154453d330d0a");
                        dialogPlus.dismiss();
                    }
                });
                button4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 4000;
                        holder.writeBuffer.setText("41542b524154453d340d0a");
                        dialogPlus.dismiss();
                    }
                });
                button5.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 4000;
                        holder.writeBuffer.setText("41542b524154453d350d0a");
                        dialogPlus.dismiss();
                    }
                });
                button6.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d360d0a");
                        dialogPlus.dismiss();
                    }
                });
                button7.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d370d0a");
                        dialogPlus.dismiss();
                    }
                });
                button8.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d380d0a");
                        dialogPlus.dismiss();
                    }
                });
                button9.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d390d0a");
                        dialogPlus.dismiss();
                    }
                });
                button10.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31300d0a");
                        dialogPlus.dismiss();
                    }
                });
                button11.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31310d0a");
                        dialogPlus.dismiss();
                    }
                });
                button12.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31320d0a");
                        dialogPlus.dismiss();
                    }
                });
                button13.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31330d0a");
                        dialogPlus.dismiss();
                    }
                });
                button14.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31340d0a");
                        dialogPlus.dismiss();
                    }
                });
                button15.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31350d0a");
                        dialogPlus.dismiss();
                    }
                });
                button16.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31360d0a");
                        dialogPlus.dismiss();
                    }
                });
                button17.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31370d0a");
                        dialogPlus.dismiss();
                    }
                });

                button18.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer = 1000;
                        holder.writeBuffer.setText("41542b524154453d31380d0a");
                        dialogPlus.dismiss();
                    }
                });

                dialogPlus.show();


            }
        });

        holder.AT_MAXBYTE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41542b4d4158425954453d320d0a");

            }
        });


        holder.AT_SENDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("");
                ATStr = "41542b53454e44423d";
            }
        });


        holder.AT_WORKMODE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
                holder.writeBuffer.setText("41542b574f524b4d4f44453d32310d0a");
            }
        });


    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()){
            onBindViewHolder(holder,position);
        }else {
            Object o = payloads.get(0);
            if(o instanceof ModemEntity){
                ModemEntity o1 = (ModemEntity) o;
                holder.cbDCD.setChecked(o1.DCD);
                holder.cbDSR.setChecked(o1.DSR);
                holder.cbCTS.setChecked(o1.CTS);
                holder.cbRING.setChecked(o1.RING);
            }else if(o instanceof ModemErrorEntity){
                ModemErrorEntity o2 = (ModemErrorEntity) o;
                ModemErrorEntity.ErrorType errorType = o2.errorType;
                if(errorType!=null){
                    switch (errorType){
                        case FRAME:
                            holder.cbFrame.setChecked(true);
                            break;
                        case OVERRUN:
                            holder.cbOverrun.setChecked(true);
                            break;
                        case PARITY:
                            holder.cbParity.setChecked(true);
                            break;
                    }
                }
            }
        }
    }

    public void updateModemStatus(ModemEntity modemEntity){
        int index=-1;
        for (int i = 0; i < serialEntities.size(); i++) {
            SerialEntity serialEntity = serialEntities.get(i);
            if(serialEntity.getSerialNumber()==modemEntity.serialNumber){
                index=i;
                break;
            }
        }
        if(index>=0){
            notifyItemChanged(index,modemEntity);
        }
    }

    public void updateModemErrorStatus(ModemErrorEntity errorEntity){
        int index=-1;
        for (int i = 0; i < serialEntities.size(); i++) {
            SerialEntity serialEntity = serialEntities.get(i);
            if(serialEntity.getSerialNumber()==errorEntity.serialNumber){
                index=i;
                break;
            }
        }
        if(index>=0){
            notifyItemChanged(index,errorEntity);
        }
    }

    @Override
    public int getItemCount() {
        return serialEntities==null? 0:serialEntities.size();
    }

    public SerialEntity get(int position){
        return serialEntities==null? null :serialEntities.get(position);
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tvDescription;
        TextView serialInfo;
        CustomTextView setSerial;

        AppCompatCheckBox cbDTR;
        AppCompatCheckBox cbRTS;
        AppCompatCheckBox cbBREAK;

        AppCompatCheckBox cbDCD;
        AppCompatCheckBox cbDSR;
        AppCompatCheckBox cbCTS;
        AppCompatCheckBox cbRING;

        AppCompatCheckBox cbOverrun;
        AppCompatCheckBox cbParity;
        AppCompatCheckBox cbFrame;

        CustomTextView queryError;

        CustomTextView write;
        CustomTextView clearWrite;
        SwitchCompat scWrite;
        TextView writeCount;


        EditText writeBuffer;


        CustomTextView clearRead,At,AT_RST,AT_WORKMODE,AT_FREQ,AT_RATE,AT_MAXBYTE,AT_SENDB,AT_TXP;




        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription=itemView.findViewById(R.id.tvSerialDescription);
            serialInfo=itemView.findViewById(R.id.tvSerialInfo);
            setSerial=itemView.findViewById(R.id.tvSerialConfig);

            cbDTR=itemView.findViewById(R.id.cbDTR);
            cbRTS=itemView.findViewById(R.id.cbRTS);
            cbBREAK=itemView.findViewById(R.id.cbBreak);

            cbDCD=itemView.findViewById(R.id.cbDCD);
            cbDSR=itemView.findViewById(R.id.cbDSR);
            cbCTS=itemView.findViewById(R.id.cbCTS);
            cbRING=itemView.findViewById(R.id.cbRing);

            cbOverrun=itemView.findViewById(R.id.cbOverrun);
            cbParity=itemView.findViewById(R.id.cbParity);
            cbFrame=itemView.findViewById(R.id.cbFrame);

            queryError=itemView.findViewById(R.id.queryErrorStatus);

            write=itemView.findViewById(R.id.tvWrite);
            writeBuffer=itemView.findViewById(R.id.send_data);
            writeCount=itemView.findViewById(R.id.tvWriteCount);

            clearWrite=itemView.findViewById(R.id.tvClearWrite);

            scWrite=itemView.findViewById(R.id.scWrite);

            At=itemView.findViewById(R.id.AT);
            AT_RST=itemView.findViewById(R.id.AT_RST);
            AT_WORKMODE = itemView.findViewById(R.id.AT_WORKMODE);
            AT_FREQ=itemView.findViewById(R.id.AT_FREQ);
            AT_RATE=itemView.findViewById(R.id.AT_RATE);
            AT_MAXBYTE=itemView.findViewById(R.id.AT_MAXBYTE);
            AT_SENDB=itemView.findViewById(R.id.AT_SENDB);
            AT_TXP=itemView.findViewById(R.id.AT_TXP);



        }
    }
    //设置串口参数
    boolean setSerialParameter(UsbDevice usbDevice, int serialNumber, SerialBaudBean baudBean){
        try {
            boolean b = WCHUARTManager.getInstance().setSerialParameter(usbDevice, serialNumber,
                    baudBean.getBaud(), baudBean.getData(), baudBean.getStop(), baudBean.getParity(),baudBean.isFlow());
            return b;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return false;
    }

    public void setDTR(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setDTR(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置DTR失败");
            }
            //showToast("设置DTR"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    public void setRTS(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setRTS(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置RTS失败");
            }
            //showToast("设置RTS"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    public void setBreak(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setBreak(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置Break失败");
            }
            //showToast("设置Break"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    //写数据
    int writeData(UsbDevice usbDevice,int serialNumber,@NonNull byte[] data,int length){
        try {
            int write = WCHUARTManager.getInstance().writeData(usbDevice, serialNumber, data, length,2000);
            return write;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());

        }
        return -2;
    }

    public int getWriteCount(int serialNumber){
        return writeCountMap.get(serialNumber);
    }

    public void setWriteCount(int serialNumber,int newValue){
        writeCountMap.put(serialNumber,newValue);
    }



    void showToast(String message){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WCHApplication.getContext(),message,Toast.LENGTH_SHORT).show();
                //ToastUtil.create(activity,message).show();
            }
        });

    }


    private void startTimer() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里编写定时任务的逻辑
                writeData();
            }
        };
        mTimer.schedule(mTimerTask, 0, 2500); // 每隔1秒执行一次任务
    }

    private void pauseTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }


    private void writeData() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = dateFormat.format(new Date());

        Log.e(TAG, "writeData: "+currentTime );



        String s = MyViewHolder.writeBuffer.getText().toString();

        boolean isNumeric = true;
        try {
            double num = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            isNumeric = false;
        }

        if (isNumeric) {
//            System.out.println("字符串 \"" + str + "\" 是纯数字。");
            int i = Integer.valueOf(s).intValue() + 1;
            s = String.valueOf(i);

            if (i >= 1000 && i <= 9999) {
//                System.out.println("这个数是一个4位数。");
            } else if (i >= 100 && i <= 999) {
//                System.out.println("这个数是一个3位数。");
                s = "0"+s;
            } else if (i >= 10 && i <= 99) {
//                System.out.println("这个数是一个2位数。");
            }else if (i < 10 && i > -1) {
                s = "0"+s;
            }else {
//                System.out.println("这个数不是2位数、3位数或4位数。");
            }

            SendMsgStr = s;
        } else {
//            System.out.println("字符串 \"" + str + "\" 不是纯数字。");
            SendMsgStr = "";
        }


        if (ATStr != null){
                    StringBuilder hex = new StringBuilder();
                    for (char ch : s.toCharArray()) {
                        hex.append(Integer.toHexString((int) ch));
                    }
                    s = ATStr + hex.toString()+"0d0a";
                }

                if(TextUtils.isEmpty(s)){
                    showToast("发送内容为空");
                    return;
                }
                byte[] bytes = null;
                if(MyViewHolder.scWrite.isChecked()){
                    if(!s.matches("([0-9|a-f|A-F]{2})*")){
                        showToast("发送内容不符合HEX规范");
                        return;
                    }
                    bytes= FormatUtil.hexStringToBytes(s);
                }else {
                    bytes = s.getBytes(StandardCharsets.UTF_8);
                }
                int ret = writeData(serialEntities2.getUsbDevice(), serialEntities2.getSerialNumber(), bytes, bytes.length);
                if(ret>0){
                    //更新发送计数
                    int writeCount = getWriteCount(serialEntities2.getSerialNumber());
                    writeCount+=ret;
                    setWriteCount(serialEntities2.getSerialNumber(),writeCount);
                    MyViewHolder.writeCount.setText(String.format(Locale.getDefault(),"发送计数：%d字节",writeCount));
                    //showToast("发送成功");

                    if(SendMsgStr.length()>1){
                        MyViewHolder.writeBuffer.setText(SendMsgStr);
                    }
                }else {
                    showToast("发送失败");
                }
    }
}