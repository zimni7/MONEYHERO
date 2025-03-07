package edu.sungshin.moneyhero

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import edu.sungshin.moneyhero.broadcast.AlarmReceiver
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private val POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE = 1

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private var selectedHour = 7 // 기본 시간 설정 (7시)
    private var selectedMinute = 0 // 기본 분 설정 (0분)

    // SharedPreferences에 저장할 키
    private val PREFS_NAME = "AlarmPrefs"
    private val KEY_HOUR = "hour"
    private val KEY_MINUTE = "minute"
    private val KEY_ALARM_ON = "alarm_on" // 알람 켜짐/꺼짐 상태 저장 키

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Android 13 이상에서 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE
                )
            }
        }

        // 알람 매니저 초기화
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // SharedPreferences에서 이전 설정된 시간 불러오기
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedHour = sharedPreferences.getInt(KEY_HOUR, 7) // 기본값 7시
        selectedMinute = sharedPreferences.getInt(KEY_MINUTE, 0) // 기본값 0분
        val alarmOn = sharedPreferences.getBoolean(KEY_ALARM_ON, false) // 알람 상태 불러오기


        // 스위치 연결
        val alarmSwitch = findViewById<Switch>(R.id.mySwitch)
        alarmSwitch.isChecked = alarmOn // 이전 알람 상태에 맞춰 스위치 설정

        // 홈 버튼
        val homeBtn = findViewById<Button>(R.id.homeBtn)
        homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 시간 선택 버튼 및 텍스트뷰
        val timePickerBtn = findViewById<Button>(R.id.timePickerBtn)
        val timeTextView = findViewById<TextView>(R.id.timeTextView)

        // 현재 선택된 시간을 표시 (기본 값)
        timeTextView.text = String.format("%02d:%02d", selectedHour, selectedMinute)

        // 시간 선택 버튼 클릭 시 TimePickerDialog 표시
        timePickerBtn.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute

                    // 선택된 시간 표시
                    timeTextView.text = String.format("%02d:%02d", selectedHour, selectedMinute)

                    // SharedPreferences에 저장
                    val editor = sharedPreferences.edit()
                    editor.putInt(KEY_HOUR, selectedHour)
                    editor.putInt(KEY_MINUTE, selectedMinute)
                    editor.apply()
                },
                selectedHour, selectedMinute, true
            )
            timePickerDialog.show()
        }

        // 스위치 상태 변화 처리
        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->

            // 알람 켜기/끄기 상태를 SharedPreferences에 저장
            val editor = sharedPreferences.edit()
            editor.putBoolean(KEY_ALARM_ON, isChecked)
            editor.apply()

            if (isChecked) {
                // 선택된 시간에 알람 설정
                setAlarm(selectedHour, selectedMinute)
            } else {
                // 알람 해제
                cancelAlarm()
                Toast.makeText(this, "알림이 꺼졌습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour) // 사용자가 설정한 시간
            set(Calendar.MINUTE, minute)     // 사용자가 설정한 분
            set(Calendar.SECOND, 0)

            // 현재 시간이 설정한 시간 이후라면 내일로 설정
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 정확한 알람 설정 (반복은 BroadcastReceiver에서 재설정으로 처리 가능)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        // 알림 채널 추가
        createNotificationChannel()

        Toast.makeText(this, "알람이 설정되었습니다: ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "알람이 취소되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "Alarm Notifications"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)  // 진동 패턴
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 알림을 보내는 메소드
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = Notification.Builder(context, "alarm_channel")
            .setContentTitle("알람")
            .setContentText("설정한 시간입니다!")
            .setSmallIcon(R.drawable.icon)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        notificationManager.notify(0, notification)
    }
}
