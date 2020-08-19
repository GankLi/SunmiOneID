package com.gank.quotademo;

import android.os.StatFs;
import android.system.StructStatVfs;

import java.lang.reflect.Field;

public class FileUtils {

    /**
     * 监测分区
     */
    private static final String DATA_PARTITION = "/data";

    /**
     * 执行stat -f /data命令
     *
     * @return 当前节点数占用情况
     */
    public static StatFs calculateBlock() {
        StatFs statFs = new StatFs(DATA_PARTITION);
        return statFs;
    }

    /**
     * 执行stat -f /data命令
     *
     * @return 当前节点数占用情况
     */
    public static StructStatVfs calculateInode() {
        StatFs statFs = new StatFs(DATA_PARTITION);
        try {
            Field field = statFs.getClass().getDeclaredField("mStat");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            Object object = field.get(statFs);
            if (object instanceof StructStatVfs) {
                return (StructStatVfs) object;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return null;
    }

    public static int getFSLevel(){
        StructStatVfs statVfs =  calculateInode();
        if(statVfs != null){
            return (int) ((statVfs.f_files - statVfs.f_ffree) * 100 / statVfs.f_files);
        }
        return -1;
    }

}
