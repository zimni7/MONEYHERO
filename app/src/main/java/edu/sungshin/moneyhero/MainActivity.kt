package edu.sungshin.moneyhero

import android.app.TabActivity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.common.reflect.TypeToken
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import edu.sungshin.moneyhero.data.Quote
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random


@Suppress("deprecation")
class MainActivity : TabActivity() {

    private lateinit var quoteTextView: TextView
    private lateinit var authorTextView: TextView

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var diaryContent: EditText
    private lateinit var saveBtn: Button
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var textViewDate: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var finishBtn: Button

    private lateinit var timerTextView: TextView
    private lateinit var targetTimeTextView: TextView
    private lateinit var userTimeTextView: TextView

    private lateinit var checkTimeButton: Button
    private lateinit var startGameBtn: Button
    private lateinit var stopGameBtn: Button

    private val tasks = ArrayList<String>()
    private var task = mutableListOf<String>() // 할 일 목록
    private var isTaskCompleted = false
    private var savedDate: String = ""

    private lateinit var tvChildName: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var tabHost = this.tabHost

        var tabSpecHome = tabHost.newTabSpec("HOME")
            .setIndicator(null, getResources().getDrawable(R.drawable.home))
        tabSpecHome.setContent(R.id.tabHome)
        tabHost.addTab(tabSpecHome)
        initHomeTab()

        var tabSpecCalendar = tabHost.newTabSpec("CALENDAR")
            .setIndicator(null, getResources().getDrawable(R.drawable.calendar))
        tabSpecCalendar.setContent(R.id.tabCalendar)
        tabHost.addTab(tabSpecCalendar)
        initCalendarTab()


        var tabSpecStudy = tabHost.newTabSpec("STUDY")
            .setIndicator(null, getResources().getDrawable(R.drawable.study))
        tabSpecStudy.setContent(R.id.tabStudy)
        tabHost.addTab(tabSpecStudy)
        initStudyTab()


        var tabSpecGame = tabHost.newTabSpec("GAME")
            .setIndicator(null, getResources().getDrawable(R.drawable.game))
        tabSpecGame.setContent(R.id.tabGame)
        tabHost.addTab(tabSpecGame)
        initGameTab()

        tabHost.currentTab = 0

    }

    private fun initHomeTab() {

        // SharedPreferences에서 GameTab의 level 데이터 불러오기
        val gameData = getSharedPreferences("GameData", MODE_PRIVATE)
        val gameLevel = gameData.getInt("level", 1)

        // TextView에 이름과 레벨 표시
        val levelText: TextView = findViewById(R.id.level)
        levelText.text = "Game Level: $gameLevel"

        // ProgressBar 즉시 갱신
        updateProgressBar()  // 별도 메서드 호출로 ProgressBar 갱신

        // tabHome에 포함된 뷰의 기능을 초기화하는 코드 작성
        val menuBtn = findViewById<Button>(R.id.menuBtn) // 예시 버튼
        menuBtn.setOnClickListener {
            // 팝업 메뉴 생성
            val popupMenu = PopupMenu(this, menuBtn)

            // 메뉴 아이템 추가
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu, popupMenu.menu) // 메뉴 리소스를 추가

            // 팝업 메뉴 아이템 클릭 리스너
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_account -> {
                        // 내 계정 정보 클릭 시 처리
                        val intent = Intent(this, AccountActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.menu_alarm -> {
                        val intent = Intent(this, AlarmActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            // 팝업 메뉴 보이기
            popupMenu.show()
        }

        quoteTextView = findViewById(R.id.quoteTextView)
        authorTextView = findViewById(R.id.authorTextView)

        val quotes = getQuotes()
        showRandomQuote(quotes)

        tvChildName = findViewById(R.id.childname)

        // 현재 로그인한 사용자의 UID를 가져옴
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            // Firestore에서 해당 UID의 사용자 정보 가져오기
            fetchUsernameFromFirestore(userId)
        } else {
            // 사용자가 로그인되어 있지 않은 경우
            tvChildName.text = "로그인 필요"
        }
    }

    private fun fetchUsernameFromFirestore(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    if (!username.isNullOrEmpty()) {
                        // TextView에 username 설정
                        tvChildName.text = "$username 학생"
                    } else {
                        tvChildName.text = "이름 없음"
                    }
                } else {
                    Log.d("MainActivity", "No such document")
                    tvChildName.text = "데이터 없음"
                }
            }
            .addOnFailureListener { e ->
                Log.w("MainActivity", "Error fetching document", e)
                tvChildName.text = "오류 발생"
            }
    }

    // ProgressBar 업데이트 메서드
    private fun updateProgressBar() {
        val sharedPreferences = getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE)
        val progress = sharedPreferences.getInt("progress", 0)
        val progressBar: ProgressBar = findViewById(R.id.progressbar)
        progressBar.progress = progress

        // 버튼 상태 업데이트
        updateStartTimerButtonState(progress)
    }

    // 타이머 버튼 상태 업데이트 메서드
    private fun updateStartTimerButtonState(progress: Int) {
        val startTimerButton: Button = findViewById(R.id.startTimerButton)

        // ProgressBar의 값에 따라 버튼 활성화/비활성화 설정
        if (progress > 0) {
            startTimerButton.isEnabled = true
        } else {
            startTimerButton.isEnabled = false
        }
    }


    // 명언 목록을 반환하는 함수
    private fun getQuotes(): List<Quote> {
        return listOf(
            Quote("알버트 아인슈타인", "삶은 자전거를 타는 것과 같다. 균형을 잡으려면 계속 움직여야 한다."),
            Quote("마르쿠스 아우렐리우스", "네가 할 수 있는 일을 하라."),
            Quote("공자", "지혜는 나누어야 한다."),
            Quote("헬렌 켈러", "세상에서 가장 아름다운 것은 볼 수 없고 만질 수도 없다. 그것은 바로 마음이다."),
            Quote("스티브 잡스", "여러분의 일은 여러분의 삶의 대부분을 차지할 것이다. 그러므로 일을 사랑하라."),
            Quote("알프레드 몽디", "성공은 오늘의 노력의 합이다."),
            Quote("월트 디즈니", "꿈을 이루기 위해서는 먼저 꿈을 꾸어야 한다."),
            Quote("나폴레옹 힐", "어려움은 성장을 위한 기회이다."),
            Quote("에이브러햄 링컨", "도전은 성취의 첫 번째 단계이다."),
            Quote("토마스 에디슨", "노력과 열정은 재능보다 중요하다."),
            Quote("마이클 조던", "성공은 실패에서 몇 걸음 떨어져 있다."),
            Quote("알베르트 아인슈타인", "삶은 진정한 이해와 인내를 통해 가치 있게 된다."),
            Quote("윈스턴 처칠", "어려움은 성장의 기회이다."),
            Quote("헬렌 켈러", "희망은 심장 깊은 곳에서 비춰진다."),
            Quote("톨스토이", "오늘의 내가 어제의 나보다 나은 사람이라면, 그게 바로 성공이다."),
            Quote("오스카 와일드", "불가능이란 단지 시간이 걸리는 일이다."),
            Quote("헨리 포드", "누구도 실패 없이 성공할 수 없다."),
            Quote("마하트마 간디", "성공을 위해서 가장 중요한 것은 포기하지 않는 것이다."),
            Quote("버지니아 울프", "배움의 길은 끝이 없다."),
            Quote("프랭클린 D. 루스벨트", "행복은 자신이 선택하는 것이다."),
            Quote("오프라 윈프리", "나 자신을 믿고 걸어가라."),
            Quote("라오쯔", "모든 것은 나의 마음에서 시작된다."),
            Quote("마크 트웨인", "나아가는 것만이 실패를 막을 수 있다."),
            Quote("소크라테스", "자신을 알면 나아갈 길이 보인다."),
            Quote("에픽테토스", "가장 중요한 것은 무엇을 하느냐보다 어떻게 하느냐이다."),
            Quote("헬렌 켈러", "희망을 잃지 않는 사람만이 길을 찾는다."),
            Quote("스티브 잡스", "열정은 성공의 열쇠이다."),
            Quote("버락 오바마", "이제는 멈추지 마라, 계속 나아가라."),
            Quote("에디슨", "실패는 그저 성공을 위한 준비일 뿐이다."),
            Quote("스티븐 호킹", "나의 미래는 내가 만드는 것이다."),
            Quote("찰스 다윈", "사람은 언제든지 변화할 수 있다."),
            Quote("프리드리히 니체", "현재의 내가 과거의 나를 이길 수 있다면 그것이 발전이다."),
            Quote("토머스 에디슨", "목표를 향해 달려라, 결국에는 성공이 온다."),
            Quote("존 F. 케네디", "꿈은 현실이 된다."),
            Quote("오스카 와일드", "가장 중요한 것은 지금 시작하는 것이다."),
            Quote("윌리엄 셰익스피어", "성공의 열쇠는 끈기이다."),
            Quote("레오나르도 다 빈치", "오늘의 내가 내일의 나를 만든다."),
            Quote("제프 베조스", "지금 시작하지 않으면 언제나 후회하게 된다."),
            Quote("빈센트 반 고흐", "불가능은 나의 사전에는 없다."),
            Quote("알베르트 아인슈타인", "자신의 길을 가는 것이 진정한 자유이다."),
            Quote("마크 트웨인", "오늘이 마지막 날처럼 최선을 다하라."),
            Quote("칼릴 지브란", "행복은 다른 사람에게서가 아니라, 내 마음 속에 있다."),
            Quote("마이클 조던", "열정이 없다면 인생은 의미 없다."),
            Quote("스티브 잡스", "꿈을 꾸는 자만이 그것을 이룰 수 있다."),
            Quote("윈스턴 처칠", "최선을 다해도 때로는 실패가 있다. 하지만 그 실패가 나를 더욱 강하게 만든다."),
            Quote("브루스 리", "시작이 반이다. 그래서 시작이 중요하다."),
            Quote("로버트 콜리어", "매일 성장하는 나 자신을 자랑스럽게 여겨라."),
            Quote("헬렌 켈러", "이 순간을 즐겨라, 그것이 삶이다."),
            Quote("피터 드러커", "어떤 일이든 시작하는 것이 중요하다."),
            Quote("잭 마", "성공한 사람들은 항상 한 가지를 갖고 있다: 끈기."),
            Quote("랄프 왈도 에머슨", "하루하루가 소중하다."),
            Quote("마하트마 간디", "꿈을 이루려면 우선 꿈을 꾸어야 한다."),
            Quote("헨리 포드", "당신이 할 수 있다고 믿으면, 절반은 성공한 것이다."),
            Quote("존 우든", "모든 일이 잘 될 것이다, 믿고 나아가라."),
        )
    }

    // 랜덤 명언을 화면에 표시하는 함수
    private fun showRandomQuote(quotes: List<Quote>) {
        val randomIndex = Random.nextInt(quotes.size)
        val randomQuote = quotes[randomIndex]

        quoteTextView.text = "\"${randomQuote.quote}\""
        authorTextView.text = "- ${randomQuote.author}"
    }

    private fun initCalendarTab() {
        val menuBtn2 = findViewById<Button>(R.id.menuBtn2)
        menuBtn2.setOnClickListener {
            val popupMenu = PopupMenu(this, menuBtn2)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_account -> {
                        startActivity(Intent(this, AccountActivity::class.java))
                        true
                    }
                    R.id.menu_alarm -> {
                        val intent = Intent(this, AlarmActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // Initialize views
        calendarView = findViewById(R.id.calenderview)
        selectedDateText = findViewById(R.id.selectedDateText)
        diaryContent = findViewById(R.id.diaryContent)
        saveBtn = findViewById(R.id.saveBtn)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("DiaryPrefs", MODE_PRIVATE)

        // Set initial date and load diary content
        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        selectedDateText.text = currentDate
        loadDiaryContent(currentDate)

        // Set listener for calendar view to handle date selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedDateText.text = selectedDate
            loadDiaryContent(selectedDate)
        }
        // Set listener for save button to save the diary content
        saveBtn.setOnClickListener {
            val selectedDate = selectedDateText.text.toString()
            val content = diaryContent.text.toString()
            saveDiaryContent(selectedDate, content)
            Toast.makeText(this, "내용이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Load diary content from SharedPreferences
    private fun loadDiaryContent(date: String) {
        val savedContent = sharedPreferences.getString(date, "")
        diaryContent.setText(savedContent ?: "")
    }

    // Save diary content to SharedPreferences
    private fun saveDiaryContent(date: String, content: String) {
        val editor = sharedPreferences.edit()
        editor.putString(date, content)
        editor.apply()
    }

    // Save current diary content when the activity is paused
    override fun onPause() {
        super.onPause()
        val selectedDate = selectedDateText.text.toString()
        val content = diaryContent.text.toString()
        saveDiaryContent(selectedDate, content)
        saveCheckedStates()
    }

    private fun initStudyTab() {
        // 팝업 메뉴 초기화
        val menuBtn3 = findViewById<Button>(R.id.menuBtn3)
        menuBtn3.setOnClickListener {
            val popupMenu = PopupMenu(this, menuBtn3)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_account -> {
                        startActivity(Intent(this, AccountActivity::class.java))
                        true
                    }
                    R.id.menu_alarm -> {
                        val intent = Intent(this, AlarmActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val plusButton: Button = findViewById(R.id.plus)
        finishBtn = findViewById(R.id.finishBtn)
        listView = findViewById(R.id.listView)
        val progressBar: ProgressBar = findViewById(R.id.progressbar)

        // Initialize the adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, tasks)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        loadCheckedStates()
        val isFinishButtonDisabled = sharedPreferences.getBoolean("isFinishButtonDisabled", false)

        // Show the dialog when the plus button is clicked
        plusButton.setOnClickListener {
            showAddTaskDialog()
        }

        textViewDate = findViewById(R.id.textViewDate)

        // 오늘의 날짜 가져오기
        val calendar = Calendar.getInstance()
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // TextView에 오늘 날짜 설정
        textViewDate.text = currentDate

        // 날짜가 바뀌었는지 확인하고 리스트 초기화
        checkDateAndResetTasks(currentDate)

        // Finish 버튼 이벤트
        finishBtn.setOnClickListener {
            if (!isTaskCompleted) {
                showConfirmationDialog()
            }
        }

        // 체크박스 상태 변경 리스너 설정
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = tasks[position] // 선택된 항목
            val isChecked = listView.isItemChecked(position) // 체크 상태

            // SharedPreferences에 프로그래스 바 진행 상태 저장
            val sharedPreferences = getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // 체크 시 프로그래스 증가, 체크 해제 시 프로그래스 감소
            val currentProgress = sharedPreferences.getInt("progress", 0)
            val newProgress = if (isChecked) {
                currentProgress + 1
            } else {
                currentProgress - 1
            }

            // 프로그래스를 0~15 사이로 제한
            val finalProgress = newProgress.coerceIn(0, 15)

            // 프로그래스 값 저장
            editor.putInt("progress", finalProgress)
            editor.apply()

            // 프로그래스 바 업데이트
            updateProgressBar()

            // 체크 상태 저장 (SharedPreferences나 다른 방법으로)
            saveCheckedStates()
            // 체크 상태 변경 후 ListView 갱신
            listView.setItemChecked(position, isChecked) // 선택된 항목 체크 상태 설정
            adapter.notifyDataSetChanged() // 어댑터 갱신
        }

    }

    private fun checkDateAndResetTasks(currentDate: String) {
        // SharedPreferences에서 저장된 날짜 확인
        val savedDate = sharedPreferences.getString("lastDate", "") ?: ""

        if (savedDate != currentDate) {
            // 1. 날짜 변경 시 SharedPreferences 초기화
            val editor = sharedPreferences.edit()
            editor.putString("lastDate", currentDate) // 새로운 날짜 저장
            editor.putString("checkedStates", "[]") // 체크 상태 초기화
            editor.putInt("progress", 0) // 프로그래스 값 초기화
            editor.apply()

            // 2. ListView 데이터 초기화
            tasks.clear()
            adapter.notifyDataSetChanged()

            // 3. 완료 상태 초기화
            isTaskCompleted = false

            // 4. Firestore 데이터 초기화 (날짜가 바뀔 때만)
            clearTasksFromFirestore()

            // 5. 프로그래스 바 초기화
            updateProgressBar()
        } else {
            // Adapter 초기화 후 체크 상태 복원
            listView.adapter = adapter
            listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            // 날짜가 같으면 기존 데이터 유지
            loadTasksFromFirestore()
        }
    }

    private fun clearTasksFromFirestore() {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val tasksCollection = db.collection("users").document(userId ?: "default").collection("tasks")

        // Firestore의 모든 작업 삭제
        tasksCollection.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot) {
                document.reference.delete()
            }
        }
    }

    private fun loadTasksFromFirestore() {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val tasksCollection =
            db.collection("users").document(userId ?: "default").collection("tasks")

        tasksCollection.get().addOnSuccessListener { querySnapshot ->
            tasks.clear()
            for (document in querySnapshot) {
                val task = document.getString("task")
                if (task != null) {
                    tasks.add(task)
                }
            }
            adapter.notifyDataSetChanged()

        }
    }

    private fun saveTasksToFirestore() {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val tasksCollection =
            db.collection("users").document(userId ?: "default").collection("tasks")

        // Clear the existing tasks
        tasksCollection.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot) {
                document.reference.delete()
            }

            // Add the new tasks to Firestore
            for (task in tasks) {
                tasksCollection.add(hashMapOf("task" to task))
            }
        }
    }

    private fun saveCheckedStates() {
        val sharedPreferences = getSharedPreferences("CheckedStates", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // 모든 체크 상태를 저장할 리스트
        val checkedStates = ArrayList<Boolean>()

        // 리스트뷰의 체크 상태를 저장
        for (i in 0 until listView.count) {
            checkedStates.add(listView.isItemChecked(i))
        }

        // JSON 형식으로 저장
        val json = Gson().toJson(checkedStates)
        Log.d("CheckedStates", "Saving checked states: $json")  // 저장된 값 확인
        editor.putString("checkedStates", json)
        editor.apply()
    }


    private fun loadCheckedStates() {
        val sharedPreferences = getSharedPreferences("CheckedStates", Context.MODE_PRIVATE)
        val checkedStatesJson = sharedPreferences.getString("checkedStates", "[]") ?: "[]"

        // JSON 형식으로 저장된 체크 상태 리스트 복원
        val checkedStates = Gson().fromJson<List<Boolean>>(checkedStatesJson, object : TypeToken<List<Boolean>>() {}.type)

        // 리스트뷰의 체크 상태를 설정
        for (i in checkedStates.indices) {
            if (i < listView.count) {
                listView.setItemChecked(i, checkedStates[i])
            }
        }
    }


    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("오늘의 할 일")
            .setMessage("오늘 할 일을 모두 완료하셨습니까?\n더 이상 수정할 수 없습니다.\n부모님에게 문자가 전송됩니다.")
            .setPositiveButton("확인") { _, _ ->
                // 모든 아이템의 체크 상태를 유지
                isTaskCompleted = true

                for (i in 0 until listView.count) {
                    listView.setItemChecked(i, listView.isItemChecked(i))
                }

                // ListView 비활성화
                listView.isEnabled = false
                finishBtn.isEnabled = false

                // Save tasks and checkbox states to Firestore and SharedPreferences
                saveTasksToFirestore()
                saveCheckedStates()

                Toast.makeText(this, "오늘의 할 일이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                finishBtn.isEnabled = false
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("오늘 할 일")

        // Inflate the custom layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        builder.setView(dialogView)

        val taskInput: EditText = dialogView.findViewById(R.id.task_input)

        builder.setPositiveButton("확인") { _, _ ->
            val task = taskInput.text.toString().trim()
            if (task.isNotEmpty()) {
                tasks.add(task)
                adapter.notifyDataSetChanged()

                // Firestore에 할 일 목록 저장
                saveTasksToFirestore()
                saveCheckedStates() // Save checkbox states after adding a task
            }
        }

        builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }


    override fun onResume() {
        super.onResume()
        loadCheckedStates() // 화면 복원 시 체크 상태 유지
    }

    private fun initGameTab() {
        // tabHome에 포함된 뷰의 기능을 초기화하는 코드 작성
        val menuBtn4 = findViewById<Button>(R.id.menuBtn4) // 예시 버튼
        menuBtn4.setOnClickListener {
            // 팝업 메뉴 생성
            val popupMenu = PopupMenu(this, menuBtn4)

            // 메뉴 아이템 추가
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu, popupMenu.menu) // 메뉴 리소스를 추가

            // 팝업 메뉴 아이템 클릭 리스너
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_account -> {
                        // 내 계정 정보 클릭 시 처리
                        val intent = Intent(this, AccountActivity::class.java)
                        startActivity(intent)
                        true
                    }

                    R.id.menu_alarm -> {
                        val intent = Intent(this, AlarmActivity::class.java)
                        startActivity(intent)
                        true
                    }

                    else -> false
                }
            }

            // 팝업 메뉴 보이기
            popupMenu.show()
        }

        // SharedPreferences 객체 생성
        val sharedPreferences = getSharedPreferences("GameData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

// 앱 시작 시 저장된 데이터 불러오기
        var level = sharedPreferences.getInt("level", 1) // 저장된 레벨, 기본값 1
        var exp = sharedPreferences.getInt("exp", 0)     // 저장된 경험치, 기본값 0

// UI 컴포넌트 가져오기
        val characterImage: ImageView = findViewById(R.id.characterImage)
        val levelText: TextView = findViewById(R.id.levelText)
        val feedButton: Button = findViewById(R.id.feedButton)
        val expText: TextView = findViewById(R.id.expText) // 경험치 표시용 TextView 추가
        val timerText: TextView = findViewById(R.id.timerText) // 타이머 표시용 TextView 추가
        val resultText: TextView = findViewById(R.id.resultText) // 결과 표시용 TextView 추가
        val gameCount: TextView = findViewById(R.id.gameCount)

        // 리스트뷰 및 어댑터 설정
        val listView: ListView = findViewById(R.id.listView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, tasks)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // 저장된 체크 상태 복원
        val checkedStatesJson = sharedPreferences.getString("checkedStates", "[]")
        val checkedStates = Gson().fromJson<List<Boolean>>(checkedStatesJson, object : TypeToken<List<Boolean>>() {}.type)
        for (i in checkedStates.indices) {
            listView.setItemChecked(i, checkedStates[i])
        }

// 초기 UI 설정 (불러온 데이터로 업데이트)
        levelText.text = "Level: $level"
        expText.text = "EXP: $exp / 100"

// 타이머 변수
        var timeLeft = 10 // 제한 시간 10초
        var clickCount = 0 // 버튼 클릭 횟수
        var timerRunning = false

        // 체크 상태 기반 게임 횟수 계산 함수
        fun updateProgress() {
            val checkedCount = (0 until listView.count).count { listView.isItemChecked(it) }
            gameCount.text = "Games Left: $checkedCount"
        }

        // 타이머 시작 함수
        fun startTimer() {
            val checkedCount = (0 until listView.count).count { listView.isItemChecked(it) }

            if (checkedCount <= 0) {
                Toast.makeText(this, "오늘의 할 일을 저장해주세요! \n게임을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val sharedPreferences = getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE)
            val currentProgress = sharedPreferences.getInt("progress", 0)

            // ProgressBar 값 확인
            if (currentProgress <= 0) {
                Toast.makeText(this, "Progress 부족! 타이머를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }


            timerRunning = true
            timeLeft = 10
            clickCount = 0
            resultText.text = "" // 결과 초기화
            feedButton.isEnabled = true // 버튼 활성화

            // 타이머 실행
            val timer = object : CountDownTimer((timeLeft * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeft--
                    timerText.text = "Time Left: $timeLeft s"
                }

                override fun onFinish() {
                    timerRunning = false
                    feedButton.isEnabled = false // 버튼 비활성화
                    resultText.text = "You clicked $clickCount times!"
                }
            }
            timer.start()
        }

        // 먹이 주기 버튼 클릭 이벤트
        feedButton.setOnClickListener {
            if (timerRunning) {
                clickCount++ // 클릭 횟수 증가
                exp += 1 // 먹이 1번당 경험치 1 증가
                if (exp >= 100) {
                    level++ // 레벨 증가
                    exp -= 100 // 남은 경험치 계산
                    levelText.text = "Level: $level"
                }

                // 경험치 UI 업데이트
                expText.text = "EXP: $exp / 100"

                // 데이터 저장
                editor.putInt("level", level)
                editor.putInt("exp", exp)
                editor.apply() // 데이터를 비동기로 저장

                // 캐릭터 애니메이션 효과 (간단한 크기 조정)
                characterImage.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction {
                        characterImage.animate().scaleX(1f).scaleY(1f).duration = 100
                    }
            }
        }

// 타이머 시작 버튼 이벤트 (타이머를 새로 시작)
        findViewById<Button>(R.id.startTimerButton).setOnClickListener {
            startTimer()
        }
    }
}