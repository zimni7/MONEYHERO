package edu.sungshin.moneyhero

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthdate: TextView
    private lateinit var logoutBtn: Button
    private lateinit var byeBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var inputButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // TextView 초기화
        tvUsername = findViewById(R.id.tvUsername)
        tvGender = findViewById(R.id.tvGender)
        tvBirthdate = findViewById(R.id.tvBirthdate)

        // Button 초기화
        logoutBtn = findViewById(R.id.logoutBtn)
        byeBtn = findViewById(R.id.byeBtn)

        // 홈 버튼
        val homeBtn = findViewById<Button>(R.id.homeBtn)
        homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 현재 로그인한 사용자 정보 가져오기
        val currentUser = auth.currentUser

        if (currentUser != null) {
            loadUserInfo(currentUser.uid)
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish() // 로그인 상태가 아니면 액티비티 종료
        }

        // 로그아웃 버튼 클릭 이벤트
        logoutBtn.setOnClickListener {
            auth.signOut() // Firebase 인증 로그아웃
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 회원탈퇴 버튼 클릭 이벤트
        byeBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val inputButton = findViewById<Button>(R.id.inputButton)

// SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

// 앱을 켤 때 저장된 이메일 가져오기
        val savedEmail = sharedPreferences.getString("email", "")
        emailEditText.setText(savedEmail)

// 이메일 등록 버튼 클릭 처리
        inputButton.setOnClickListener {
            val email = emailEditText.text.toString()

            if (isEmailValid(email)) {
                // 이메일을 SharedPreferences에 저장
                val editor = sharedPreferences.edit()
                editor.putString("email", email)
                editor.apply()  // 비동기적으로 저장

                Toast.makeText(this, "이메일이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "유효한 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // 이메일 형식 확인 함수
    private fun isEmailValid(email: String): Boolean {
        // 간단한 이메일 정규식
        val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return emailPattern.matches(email)
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("정말로 회원을 탈퇴하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("확인") { _, _ ->
                val currentUser = auth.currentUser
                currentUser?.let { user ->
                    // Firestore에서 사용자 데이터 삭제
                    db.collection("users").document(user.uid)
                        .delete()
                        .addOnSuccessListener {
                            // Firebase Authentication에서 계정 삭제
                            user.delete()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "회원 탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        Log.e("AccountActivity", "회원 탈퇴 실패: ${task.exception?.message}")
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "회원 정보를 삭제하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                            Log.e("AccountActivity", "Firestore Error: $e")
                        }
                }
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss() // 취소 시 대화상자 닫기
            }

        val alert = builder.create()
        alert.show()
    }


    private fun loadUserInfo(userId: String) {
        // Firestore에서 사용자 정보 가져오기
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    val gender = document.getString("gender")
                    val birthdate = document.getString("birthdate")

                    // TextView에 데이터 설정
                    tvUsername.text = "$username"
                    tvGender.text = "$gender"
                    tvBirthdate.text = "$birthdate"
                } else {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AccountActivity", "Error loading user info", e)
                Toast.makeText(this, "사용자 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

}
