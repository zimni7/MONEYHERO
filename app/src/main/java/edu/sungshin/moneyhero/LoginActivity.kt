package edu.sungshin.moneyhero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // FirebaseAuth 초기화
        auth = FirebaseAuth.getInstance()

        // 자동 로그인 확인
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 이전에 로그인한 사용자가 있으면 메인 화면으로 이동
            navigateToMainActivity()
        }

        // 뷰 초기화
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        // 로그인 버튼 클릭 리스너
        btnLogin.setOnClickListener {
            var username = etUsername.text.toString()
            // 아이디 뒤에 @yourapp.com 추가
            if (!username.contains("@yourapp.com")) {
                username = "$username@yourapp.com"
            }
            val password = etPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Firebase 인증을 이용해 로그인 시도
                auth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 로그인 성공
                            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                            // 로그인 성공 후 메인 화면으로 이동
                            navigateToMainActivity()
                        } else {
                            // 로그인 실패
                            Toast.makeText(this, "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 버튼 클릭 리스너
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 메인 화면으로 이동하는 함수
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // LoginActivity를 종료하여 뒤로 가기 버튼으로 돌아올 수 없게 함
    }
}
