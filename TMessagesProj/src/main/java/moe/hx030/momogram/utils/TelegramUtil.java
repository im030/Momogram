package moe.hx030.momogram.utils;


import android.content.Intent;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.proxy.ProxySettings;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.LaunchActivity;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.NekoXConfig;

public class TelegramUtil {

    public static String getFileNameWithoutEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    // 消息是否为文件
    public static boolean messageObjectIsFile(int type, MessageObject messageObject) {
        boolean cansave = (type == 4 || type == 5 || type == 6 || type == 10);
        boolean downloading = messageObject.loadedFileSize > 0;

        //图片的问题
        if (type == 4 && messageObject.getDocument() == null) {
            return false;
        }
        return cansave || downloading;
    }

    // 当文件有过加载过程，loadedFileSize > 0 ，所以不能用loadedFileSize判断是否正在下载
    public static boolean messageObjectIsDownloading(int type) {
        boolean cansave = (type == 4 || type == 5 || type == 6 || type == 10);
        return !cansave;
    }

    public static boolean isConnecting() {
        int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        return state == ConnectionsManager.ConnectionStateConnecting;
    }

    private static int proxyToggleCount = 0;
    private static Thread toggleProxyOnOffThread = null;
    private static final Runnable toggleProxyOnOffRunnable = new Runnable() {
        @Override
        public void run() {
            // too many tries, assume bad network condition, stop trying for better chance of connecting
            if (proxyToggleCount > 10) {
                proxyToggleCount = 0;
                if (SharedConfig.getProxyEnable())
                    SharedConfig.setProxyEnable(false);

                return;
            }
            boolean suc = false;
            while (!suc) {
                try {
                    Thread.sleep(1000);
                    suc = true;
                } catch (InterruptedException e) {
                    FileLog.w("sleep in proxy toggle hack was interrupted");
                    if (!isConnecting())
                        return;
                }
            }
            SharedConfig.setProxyEnable(true);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
            SharedConfig.setProxyEnable(false);
        }
    };

    public static void toggleProxyOnOff(boolean cancel) {
        toggleProxyOnOff(cancel, false);
    }

    public static void toggleProxyOnOff(boolean cancel, boolean force) {
        if (toggleProxyOnOffThread != null) {
            if (cancel) {
                toggleProxyOnOffThread.interrupt();
                proxyToggleCount = 0;
            } else {
                try {
                    toggleProxyOnOffThread.join(350);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            toggleProxyOnOffThread = null;
            return;
        } else if (cancel || (!force && !MomoConfig.fasterReconnectHack.Bool()) || !isConnecting()) {
            proxyToggleCount = 0;
            return;
        }
        ++proxyToggleCount;
        if (SharedConfig.proxyList.isEmpty()) {
            SharedConfig.addProxy(new SharedConfig.ProxyInfo(ProxySettings.builder()
                    .setAddress("dummyProxy")
                    .setPort(1080)
                    .build()));
        }
        toggleProxyOnOffThread = new Thread(toggleProxyOnOffRunnable);
        toggleProxyOnOffThread.start();
    }

    public static String getStackTraceAsString(StackTraceElement[] stackTrace) {
        StackTraceElement[] st = (stackTrace == null) ?
                Arrays.stream(Thread.currentThread().getStackTrace()).skip(3).toArray(StackTraceElement[]::new) : stackTrace;
        return Arrays.toString(st);
    }

    public static void restartApp(boolean crash) {
        if (!crash) NekoXConfig.saveMusicPlaybackState(null);
        ProcessPhoenix.triggerRebirth(ApplicationLoader.applicationContext,
                new Intent(ApplicationLoader.applicationContext, LaunchActivity.class));
    }
}
