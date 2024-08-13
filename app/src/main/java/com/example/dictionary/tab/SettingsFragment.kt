package com.example.dictionary.tab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.SignInActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: Database

    private lateinit var nameText: TextView
    private lateinit var wordCountText: TextView
    private lateinit var checkedCountText: TextView
    private lateinit var personalSettingText: TextView
    private lateinit var removeAddText: TextView
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

        personalSettingText.setOnClickListener {
            moveToPersonalSetting()
        }

        removeAddText.setOnClickListener {
            removeAdd()
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
        personalSettingText = view.findViewById(R.id.personal_setting_text)
        removeAddText = view.findViewById(R.id.remove_add_text)
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

    private fun moveToPersonalSetting() {

    }

    private fun removeAdd() {

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