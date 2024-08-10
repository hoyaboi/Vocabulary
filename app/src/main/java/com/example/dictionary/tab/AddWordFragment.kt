package com.example.dictionary.tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.Word
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class AddWordFragment : Fragment() {
    private lateinit var engEditText: TextInputEditText
    private lateinit var korEditText: TextInputEditText
    private lateinit var addButton: MaterialButton

    private lateinit var auth: FirebaseAuth
    private lateinit var database: Database

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_word, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = Database()

        setupViews(view)

        addButton.setOnClickListener {
//            val engWord = engEditText.text.toString().trim()
//            val korMean = korEditText.text.toString().trim()
//
//            if (engWord.isNotEmpty() && korMean.isNotEmpty()) {
//                val userId = auth.currentUser?.uid
//                if (userId != null) {
//                    val word = Word(english = engWord, korean = korMean, checked = false)
//                    database.saveWord(word, userId)
//
//                    Toast.makeText(requireContext(), "단어가 추가되었습니다", Toast.LENGTH_SHORT).show()
//                    engEditText.text?.clear()
//                    korEditText.text?.clear()
//                }
//            } else {
//                Toast.makeText(requireContext(), "영어 단어와 뜻을 입력하세요", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun setupViews(view: View) {
        engEditText = view.findViewById(R.id.eng_edit_text)
        korEditText = view.findViewById(R.id.kor_edit_text)
        addButton = view.findViewById(R.id.add_btn)
    }
}