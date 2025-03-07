package edu.sungshin.moneyhero.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import edu.sungshin.moneyhero.R

class AlarmReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // 알림을 보내는 함수 호출
        sendNotification(context)
    }

    annotation class RequiresApi(val value: Int)

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 ID
        val channelId = "alarm_channel"

        // 알림 생성
        val notification = Notification.Builder(context, channelId)
            .setContentTitle("알람")
            .setContentText("설정한 시간입니다!")
            .setSmallIcon(R.drawable.icon)  // 알림 아이콘
            .setAutoCancel(true)  // 클릭 시 알림 닫힘
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))  // 기본 알림 소리
            .setVibrate(longArrayOf(0, 1000, 500, 1000))  // 진동 패턴
            .build()

        // 알림을 notificationManager에 표시
        notificationManager.notify(0, notification)
    }
}
