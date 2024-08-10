package com.example.dictionary

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.tab.adapter.WordAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class WordsActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth
    private lateinit var vocabId: String
    private lateinit var vocabName: String

    private lateinit var toolbar: MaterialToolbar
    private lateinit var addWordButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var editBtnContainer: LinearLayout
    private lateinit var checkBtn: MaterialButton
    private lateinit var deleteBtn: MaterialButton
    private lateinit var hideEngButton: MaterialButton
    private lateinit var hideKorButton: MaterialButton
    private lateinit var resetButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_words)
        // 상태표시줄 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.edge)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        vocabId = intent.getStringExtra("vocabId") ?: throw IllegalStateException("vocabId가 전달되지 않았습니다.")
        vocabName = intent.getStringExtra("vocabName") ?: throw IllegalStateException("vocabName이 전달되지 않았습니다.")

        setupViews()

        recyclerView.layoutManager = LinearLayoutManager(this)

        auth = FirebaseAuth.getInstance()
        database = Database()

        refreshData()

        addWordButton.setOnClickListener {
            showSaveWordDialog()
        }

        checkBtn.setOnClickListener {
            updateCheckedStatus()
        }

        deleteBtn.setOnClickListener {
            deleteSelectedWords()
        }

        hideEngButton.setOnClickListener {
            adapter.hideEnglishText()
        }

        hideKorButton.setOnClickListener {
            adapter.hideKoreanText()
        }

        resetButton.setOnClickListener {
            adapter.resetWords()
        }
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = vocabName
        addWordButton = findViewById(R.id.add_word_btn)
        recyclerView = findViewById(R.id.words_list)
        editBtnContainer = findViewById(R.id.edit_btn_container)
        checkBtn = findViewById(R.id.check_btn)
        deleteBtn = findViewById(R.id.delete_btn)
        hideEngButton = findViewById(R.id.hide_eng_btn)
        hideKorButton = findViewById(R.id.hide_kor_btn)
        resetButton = findViewById(R.id.reset_btn)
    }

    private fun toggleEditButtonsVisibility(hasSelectedWords: Boolean) {
        editBtnContainer.visibility = if (hasSelectedWords) View.VISIBLE else View.GONE
    }

    private fun updateCheckedStatus() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        selectedWords.forEach { word ->
            if (!word.checked) {
                word.checked = true
                database.updateWordCheckedStatus(word.id, vocabId, userId, true)
            }
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)
        refreshData()
    }

    private fun deleteSelectedWords() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        selectedWords.forEach { word ->
            database.deleteWordFromVocab(userId, vocabId, word.id) { success ->
                if (!success) {
                    Toast.makeText(this, "단어장 생성자만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)
        refreshData()
    }

    private fun showSaveWordDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_save_word, null)

        builder.setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val engInput = dialogView.findViewById<TextInputEditText>(R.id.word_eng_input)
                val korInput = dialogView.findViewById<TextInputEditText>(R.id.word_kor_input)

                val engWord = engInput.text.toString().trim()
                val korWord = korInput.text.toString().trim()

                if (engWord.isNotEmpty() && korWord.isNotEmpty()) {
                    val word = Word(english = engWord, korean = korWord, checked = false)
                    saveWord(word)
                } else {
                    Toast.makeText(this, "영어 단어와 한글 뜻을 입력하세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }

    private fun saveWord(word: Word) {
        // 단어 추가 로직
        val userId = auth.currentUser?.uid ?: return
        database.saveWordToVocab(word, vocabId, userId) { success ->
            if (!success) {
                Toast.makeText(this, "단어장 생성자만 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "단어가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                refreshData()
            }
        }
    }

    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return

        database.getWordsFromVocab(userId, vocabId, this) { wordList ->
            adapter = WordAdapter(wordList) { hasSelectedWords ->
                toggleEditButtonsVisibility(hasSelectedWords)
            }
            recyclerView.adapter = adapter
        }
    }
}

