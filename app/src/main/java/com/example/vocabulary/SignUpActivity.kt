package hoya.studio.vocabulary

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: Database

    private lateinit var emailEditText: TextInputEditText
    private lateinit var pwdEditText: TextInputEditText
    private lateinit var confPwdEditText: TextInputEditText
    private lateinit var nameEditText: TextInputEditText
    private lateinit var signUpButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = Database()

        setupViews()

        signUpButton.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("email", emailEditText.text.toString())
            setResult(Activity.RESULT_OK, resultIntent)

            signUp()
        }
    }

    private fun setupViews() {
        emailEditText = findViewById(R.id.email)
        pwdEditText = findViewById(R.id.pwd)
        confPwdEditText = findViewById(R.id.pwd_confirm)
        nameEditText = findViewById(R.id.name)
        signUpButton = findViewById(R.id.sign_up_btn)
    }

    private fun signUp() {
        val email = emailEditText.text.toString().trim()
        val pwd = pwdEditText.text.toString().trim()
        val cPwd = confPwdEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()

        if(email.isNotEmpty() && pwd.isNotEmpty() && cPwd.isNotEmpty() && name.isNotEmpty()) {
            if(pwd == cPwd) {
                auth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(this) { task ->
                    if(task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if(uid != null) {
                            val user = User(uid, name)
                            database.saveUser(user)
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "회원가입 실패, 다시 시도하세요", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "모든 정보를 입력하세요", Toast.LENGTH_SHORT).show()
        }
    }
}

