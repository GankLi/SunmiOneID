package com.gank.quotademo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.system.StructStatVfs;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView mTextView;
    Button mButton;
    RadioGroup mRadioGroup;

    volatile boolean mWorking;
    int mFileType; //1: little 2: big 0: none
    int mFilePath; //1: data 2: cache

    volatile long curNodeUsedSize = 0L;
    volatile long curBlockUsedSize = 0L;

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.textView);
        mButton = findViewById(R.id.button);
        mRadioGroup = findViewById(R.id.file_type);

        mWorking = false;
        mFileType = 0;

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.big:
                        mFileType = 2;
                        break;
                    case R.id.none:
                        mFileType = 0;
                        break;
                    case R.id.little:
                        mFileType = 1;
                        break;
                }

            }
        });

        ((RadioGroup)findViewById(R.id.file_path)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.data:
                        mFilePath = 1;
                        break;
                    case R.id.cache:
                        mFilePath = 2;
                        break;
                }

            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWorking){
                    mWorking = false;
                } else {
                    mWorking = true;

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            int i = 0;
                            while (true){
                                if(!mWorking){
                                    updateInodeInfo();
                                    break;
                                }
                                Random random = new Random();
                                File path = (mFilePath == 1 ? getFilesDir() : getCacheDir());
                                File file = new File(path, "" + random.nextLong() + ".txt");
                                if(file.exists()){
                                    continue;
                                }
                                try {
                                    file.createNewFile();
                                    if(mFileType > 0){
                                        FileWriter fileWriter = new FileWriter(file);
                                        if (mFileType == 1){
                                            fileWriter.append("abcd");
                                        } else {
                                            curNodeUsedSize = 0L;
                                            curBlockUsedSize = 0L;

                                            char[] buffer = new char[1024];
                                            for (int j = 0; j < 1024 * 1024; j++){
                                                fileWriter.write(buffer);
                                                if(i % 1024 * 5 == 0) {
                                                    updateInodeInfo();
                                                }
                                            }
                                        }
                                        fileWriter.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.v("Gank", "", e);
                                    mWorking = false;
                                    updateErrorInfo(e.getMessage());
                                    break;
                                }
                                if( i% 9999 == 0){
                                    updateInodeInfo();
                                }
                            }
                        }
                    });
                }
            }
        });
        updateInodeInfo();
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            boolean showToast = msg.arg1 == 1;
            String txt = (String) msg.obj;
            mTextView.setText(txt);

            if(mWorking){
                mButton.setText("Stop");
            }else{
                mButton.setText("Start");
            }

            if (showToast){
                Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_LONG).show();
            }
            return true;
        }
    });

    private void updateInodeInfo(){
        boolean needUpdate = false;
        StringBuffer sb = new StringBuffer();
        StatFs statFs = FileUtils.calculateBlock();
        long blockUsed = (statFs.getBlockCountLong() - statFs.getAvailableBlocksLong()) * 100 / statFs.getBlockCountLong();
        if(blockUsed != curBlockUsedSize) {
            needUpdate = true;
            curBlockUsedSize = blockUsed;
            sb.append("Data - Total Block = ").append(statFs.getBlockCountLong()).append(" Available Block = ").append(statFs.getAvailableBlocksLong())
                    .append(" Used = ").append(curBlockUsedSize).append("%\n");
        }

        StructStatVfs statVfs = FileUtils.calculateInode();
        long nodeUsed = (statVfs.f_files - statVfs.f_ffree) * 100 / statVfs.f_files;
        if(nodeUsed != curNodeUsedSize) {
            needUpdate = true;
            curNodeUsedSize = nodeUsed;
            sb.append("Data - Total Inode = ").append(statVfs.f_files).append(" Available Inode = ").append(statVfs.f_ffree)
                    .append(" Used = ").append(curNodeUsedSize).append("%");
        }

        if(needUpdate) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 0;
            message.obj = sb.toString();
            mHandler.sendMessageDelayed(message, 500);
        }
    }

    private void updateErrorInfo(final String msg) {
        Message message = mHandler.obtainMessage();
        message.arg1 =  1 ;
        message.obj = msg;
        mHandler.sendMessageDelayed(message, 500);
    }
}