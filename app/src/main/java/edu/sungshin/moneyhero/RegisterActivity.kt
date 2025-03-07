package edu.sungshin.moneyhero

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etID: EditText
    private lateinit var etPassword: EditText
    private lateinit var etBirthdate: EditText // 생년월일 입력 필드
    private lateinit var radioGroupGender: RadioGroup // 성별 라디오 그룹
    private lateinit var btnRegister: Button
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스 초기화

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        etUsername = findViewById(R.id.etUsername)
        etID = findViewById(R.id.etID)
        etPassword = findViewById(R.id.etPassword)
        etBirthdate = findViewById(R.id.etBirthdate) // 생년월일 필드
        radioGroupGender = findViewById(R.id.radioGroupGender) // 성별 라디오 그룹 초기화
        btnRegister = findViewById(R.id.btnRegister)

        // 엔터와 스페이스바 입력 방지 필터 추가
        val blockEnterSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains("\n") || source.contains(" ")) {
                "" // 제거
            } else {
                source
            }
        }

        etUsername.filters = arrayOf(blockEnterSpaceFilter)
        etID.filters = arrayOf(blockEnterSpaceFilter)
        etPassword.filters = arrayOf(blockEnterSpaceFilter)

        // 비밀번호 입력을 초기화 상태에서 활성화
        etPassword.isEnabled = false
        btnRegister.isEnabled = false

        btnRegister.setOnClickListener {
            val etID = etID.text.toString()
            val etUsername = etUsername.text.toString()
            val etPassword = etPassword.text.toString()
            val etBirthdate = etBirthdate.text.toString()

            val selectedGenderId = radioGroupGender.checkedRadioButtonId   // 선택된 성별 가져오기

            val gender = if (selectedGenderId != -1) {
                findViewById<RadioButton>(selectedGenderId).text.toString()
            } else {
                null
            }

            if (validateInputs(etID, etPassword, gender, etBirthdate)) {
                registerUser(etID, etPassword, etUsername, gender!!, etBirthdate)
            } else {
                Toast.makeText(this, "모든 필드를 올바르게 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCheck = findViewById<Button>(R.id.btnCheck) // btnCheck 버튼 가져오기
        btnCheck.setOnClickListener {
            val etID = etID.text.toString()

            if (etID.isNotEmpty()) {
                // Firestore에서 해당 ID가 이미 존재하는지 확인
                checkIdExistsInAuth(etID)
            } else {
                Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIdExistsInAuth(etID: String) {
        // Firebase Auth에서 해당 ID가 이미 존재하는지 확인
        auth.fetchSignInMethodsForEmail("$etID@yourapp.com")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        // Firebase Auth에서 해당 ID가 없을 경우
                        Toast.makeText(this, "사용 가능한 ID입니다.", Toast.LENGTH_SHORT).show()
                        enablePasswordInput()
                    } else {
                        // Firebase Auth에서 해당 ID가 이미 존재할 경우
                        Toast.makeText(this, "이미 존재하는 사용자 ID입니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Firebase Auth에서 ID 중복 확인 실패 시
                    Toast.makeText(this, "ID 중복 확인 실패", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Error checking ID: ${task.exception?.message}")
                }
            }
    }

    private fun enablePasswordInput() {
        etPassword.isEnabled = true
        btnRegister.isEnabled = true
    }

    private fun registerUser(etID: String, etPassword: String, etUsername: String, gender: String, etBirthdate: String) {
        val email = "$etID@yourapp.com" // 아이디 + 도메인 형식

        // FirebaseAuth에 etID와 etPassword만 저장
        auth.createUserWithEmailAndPassword(email, etPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // FirebaseAuth에 회원가입 후 Firestore에 사용자 정보 저장
                    saveUserInfoToFirestore(user, etUsername, gender, etBirthdate)

                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "이미 존재하는 사용자 ID입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterActivity", "Error: ${task.exception?.message}")
                    }
                }
            }
    }

    private fun saveUserInfoToFirestore(user: FirebaseUser?, etUsername: String, gender: String, etBirthdate: String) {
        val userInfo = hashMapOf(
            "username" to etUsername,
            "gender" to gender,
            "birthdate" to etBirthdate // 생년월일 추가
        )

        user?.let {
            db.collection("users").document(user.uid) // Firestore에 사용자의 UID를 문서 ID로 사용
                .set(userInfo)
                .addOnSuccessListener {
                    Log.d("Firestore", "User info saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error adding document", e)
                }
        }
    }

    private fun validateInputs(id: String, etPassword: String, gender: String?, etBirthdate: String): Boolean {
        // YYYY.MM.DD 형식의 정규식
        val birthdatePattern = Regex("^\\d{4}\\.\\d{2}\\.\\d{2}$")

        // 입력값이 비어있지 않고, 생년월일 형식이 정확한지 확인
        return id.isNotEmpty() && etPassword.isNotEmpty() && !gender.isNullOrEmpty() &&
                etBirthdate.isNotEmpty() && birthdatePattern.matches(etBirthdate)
    }
}
