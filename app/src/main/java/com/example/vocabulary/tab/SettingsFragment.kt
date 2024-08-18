package hoya.studio.vocabulary.tab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import hoya.studio.vocabulary.Database
import hoya.studio.vocabulary.R
import hoya.studio.vocabulary.SignInActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import hoya.studio.vocabulary.RemoveAd

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: Database

    private lateinit var nameText: TextView
    private lateinit var wordCountText: TextView
    private lateinit var checkedCountText: TextView
    private lateinit var removeAdText: TextView
    private lateinit var changeNameText: TextView
    private lateinit var changePwdText: TextView
    private lateinit var signOutText: TextView
    private lateinit var deleteAccountText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = Database()

        setupViews(view)

        setPersonalInfo()

        removeAdText.setOnClickListener {
            removeAdd()
        }

        changeNameText.setOnClickListener {
            changeName()
        }

        changePwdText.setOnClickListener {
            changePassword()
        }

        signOutText.setOnClickListener {
            signOut()
        }

        deleteAccountText.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun setupViews(view: View) {
        nameText = view.findViewById(R.id.name_text)
        wordCountText = view.findViewById(R.id.word_count_text)
        checkedCountText = view.findViewById(R.id.checked_word_count_text)
        changeNameText = view.findViewById(R.id.change_name_text)
        changePwdText = view.findViewById(R.id.change_pwd_text)
        removeAdText = view.findViewById(R.id.remove_ad_text)
        signOutText = view.findViewById(R.id.sign_out_text)
        deleteAccountText = view.findViewById(R.id.delete_account_text)
    }

    private fun setPersonalInfo() {
        val userId = auth.currentUser?.uid ?: return

        database.getUserName(userId) { name ->
            nameText.text = name
        }

        database.getVocabsForUser(userId, requireContext()) { vocabs ->
            var totalWordCount = 0
            var totalCheckedCount = 0

            vocabs.forEach { vocab ->
                totalWordCount += vocab.words.size

                database.getCheckedWordsInVocab(userId, vocab.id) { checkedWords ->
                    totalCheckedCount += checkedWords.size
                    checkedCountText.text = "$totalCheckedCount 개"
                }
            }
            wordCountText.text = "$totalWordCount 개"
        }
    }

    private fun removeAdd() {
        startActivity(Intent(requireContext(), RemoveAd::class.java))
    }

    private fun changeName() {
        val userId = auth.currentUser?.uid ?: return

        // 다이얼로그 생성
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_change_name, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.new_name_edit_text)

        // 현재 이름을 불러와 다이얼로그의 입력 필드에 설정
        database.getUserName(userId) { currentName ->
            nameInput.setText(currentName)
        }

        builder.setView(dialogView)
            .setTitle("이름 변경")
            .setPositiveButton("변경") { _, _ ->
                val newName = nameInput.text.toString().trim()

                if (newName.isNotEmpty()) {
                    database.changeUserName(userId, newName) { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "이름이 변경되었습니다", Toast.LENGTH_SHORT).show()
                            nameText.text = newName // UI 업데이트
                        } else {
                            Toast.makeText(requireContext(), "이름 변경에 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }

    private fun changePassword() {
        // 다이얼로그 생성
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_change_password, null)

        val originPwdInput = dialogView.findViewById<TextInputEditText>(R.id.origin_pwd_edit_text)
        val newPwdInput = dialogView.findViewById<TextInputEditText>(R.id.new_pwd_edit_text)
        val checkNewPwdInput = dialogView.findViewById<TextInputEditText>(R.id.check_new_pwd_edit_text)

        builder.setView(dialogView)
            .setTitle("비밀번호 변경")
            .setPositiveButton("변경") { _, _ ->
                val originPwd = originPwdInput.text.toString().trim()
                val newPwd = newPwdInput.text.toString().trim()
                val checkNewPwd = checkNewPwdInput.text.toString().trim()

                if (originPwd.isNotEmpty() && newPwd.isNotEmpty() && checkNewPwd.isNotEmpty()) {
                    if (newPwd == checkNewPwd) {
                        database.changeUserPassword(originPwd, newPwd) { success ->
                            if (success) {
                                Toast.makeText(requireContext(), "비밀번호가 변경되었습니다", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "비밀번호 변경에 실패했습니다. 현재 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "새 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }


    private fun signOut() {
        auth.signOut()

        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("탈퇴하기")
            .setMessage("탈퇴 후 계정 복구가 불가능합니다. 정말로 탈퇴하시겠습니까?")
            .setPositiveButton("탈퇴") { _, _, ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }

    private fun deleteAccount() {
        val userId = auth.currentUser?.uid ?: return

        auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                database.deleteUserData(userId) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), SignInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "계정 삭제에 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "계정 삭제에 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}