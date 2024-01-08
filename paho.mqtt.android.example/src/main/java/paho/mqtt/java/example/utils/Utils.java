package paho.mqtt.java.example.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/**
 * @date: 2024-01-05 16:03
 * @author: mayz
 * @version: 1.0
 */
public class Utils {
     private static volatile Utils instance;
         private Utils() {}
         public static Utils getInstance() {
             if (instance == null) {
                 synchronized (Utils.class) {
                     if (instance == null) {
                         instance = new Utils();
                     }
                 }
             }
             return instance;
         }

          @RequiresApi(api = Build.VERSION_CODES.O)
         private String createNotificationChannel(Context mContext,String channelName){
             String channelId = mContext.getPackageName();
             NotificationChannel chan = new NotificationChannel(
                     channelId,
                     channelName, NotificationManager.IMPORTANCE_NONE
             );
             chan.setLightColor(Color.BLUE);
             chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
              NotificationManager service = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
             service.createNotificationChannel(chan);
             return channelId;
         }

         public Notification getNotification(Context mContext , String channelName, int icon){
             String mChannel = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 mChannel=createNotificationChannel(mContext,channelName);
             } else {
                mChannel="";
             }
             NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, mChannel);
             Notification notification = builder.setOngoing(true)
                     .setContentText(channelName)
                     .setSmallIcon(icon)
                     .setPriority(NotificationCompat.PRIORITY_MIN)
                     .setCategory(Notification.CATEGORY_SERVICE)
                     .build();
             notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
             return notification;
         }
}
