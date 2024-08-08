package com.example.dictionary

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var signInButton: MaterialButton
    private lateinit var signUpButton: MaterialButton
    private lateinit var emailEditText: TextInputEditText
    private lateinit var pwdEditText: TextInputEditText
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var backPressedTime: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()

        setupViews()

        signInButton.setOnClickListener {
            signIn()
        }
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    private fun setupViews() {
        signInButton = findViewById(R.id.sign_in_btn)
        signUpButton = findViewById(R.id.sign_up_btn)
        emailEditText = findViewById(R.id.email)
        pwdEditText = findViewById(R.id.pwd)
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringExtra("email")
                emailEditText.setText(data)
            }
        }
    }

    private fun signIn() {
        val email = emailEditText.text.toString().trim()
        val pwd = pwdEditText.text.toString().trim()

        if(email.isNotEmpty() && pwd.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    Toast.makeText(this, "잘못된 로그인 정보입니다", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "이메일 혹은 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finishAffinity()
            return
        } else {
            Toast.makeText(this, "단어장을 닫으려면 뒤로 가기 버튼을 한 번 더 누르세요", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}