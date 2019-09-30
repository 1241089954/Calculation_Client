package com.zjicm.calculation;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import androidx.appcompat.app.AppCompatActivity;

public class CalculationActivity extends AppCompatActivity {
    private final static String HOST = "115.198.214.91";
    private final static int PORT = 12345;
    //定义相关变量,完成初始化
    private TextView mHistory;
    private EditText mMessage;
    private Socket mSocket = null;
    private PrintWriter mPrintWriter = null;
    private StringBuilder mStringBuilder = null;
    private BufferedReader mBufferedReader = null;
    private BufferedWriter mBufferedWriter = null;
    private long time = System.currentTimeMillis();
    private InetAddress address;

    private String mContent = "";
    private int mFlag = 1;

    //定义一个handler对象,用来刷新界面
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Log.d("message", (String) msg.obj);
                mStringBuilder.append((String) msg.obj);
                mHistory.setText(mStringBuilder.toString());
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculation);

        ((TextView) findViewById(R.id.ipaddress)).setText(IPGetUtil.getIPAddress(CalculationActivity.this));

        //当程序一开始运行的时候就实例化Socket对象,与服务端进行连接,获取输入输出流
        //因为4.0以后不能再主线程中进行网络操作,所以需要另外开辟一个线程
        new Thread() {
            public void run() {
                try {
                    mSocket = new Socket(HOST, PORT);

                    InputStreamReader reader = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);

                    mBufferedReader = new BufferedReader(reader);

                    OutputStreamWriter writer = new OutputStreamWriter(mSocket.getOutputStream());

                    mBufferedWriter = new BufferedWriter(writer);

                    try {
                        address = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }

                    mPrintWriter = new PrintWriter(mBufferedWriter, true);
                    Log.d("WYL",IPGetUtil.getIPAddress(CalculationActivity.this));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        mStringBuilder = new StringBuilder();
        mHistory = (TextView) findViewById(R.id.txtshow);
        mMessage = (EditText) findViewById(R.id.editsend);
        //为发送按钮设置点击事件
        findViewById(R.id.btnsend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String msg = mMessage.getText().toString();
                if (mSocket == null || msg.equals("")) {
                    return;
                }
                if (mSocket.isConnected()) {
                    if (!mSocket.isOutputShutdown()) {
                        new Thread() {
                            @Override
                            public void run() {
                                mContent = mFlag++ + " : 请求计算表达式：" + msg + "\n";
                                Message message = new Message();
                                message.what = 1;
                                message.obj = mContent;
                                handler.sendMessage(message);
                                Log.d("WYL", mContent);
                                //mMessage.setText("");
                                mPrintWriter.println(msg);
                            }
                        }.start();
                    }
                }
            }
        });

        new Thread() {
            public void run() {
                try {
                    while (true) {
                        Log.d("WYL", "aaaa");
                        if (mSocket == null || mBufferedReader == null)
                            continue;
                        Log.d("WYL", "mSocket:" + mSocket.isConnected());
                        if (mSocket.isConnected()) {
                            if (!mSocket.isInputShutdown()) {
                                if ((mContent = mBufferedReader.readLine()) != null) {
                                    mContent += "\n\n";
                                    Message message = new Message();
                                    message.what = 1;
                                    message.obj = mContent;
                                    handler.sendMessage(message);
                                    Log.d("WYL", mContent);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("WYL",e.toString());
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (mSocket.isConnected()) {
            if (!mSocket.isOutputShutdown()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mPrintWriter.println("bye");
                        mSocket = null;
                        mPrintWriter = null;
                        mStringBuilder = null;
                        mBufferedReader = null;
                        mBufferedWriter = null;
                    }
                }).start();
            }
        }
        super.onDestroy();
        Log.d("WYL", "onDestroy");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - time > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                time = System.currentTimeMillis();
            } else {
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
