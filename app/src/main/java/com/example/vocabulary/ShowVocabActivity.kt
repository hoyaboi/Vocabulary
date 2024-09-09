package hoya.studio.vocabulary

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hoya.studio.vocabulary.tab.adapter.WordAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ShowVocabActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth
    private lateinit var vocabId: String
    private lateinit var vocabName: String

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: MaterialButton
    private lateinit var noWordText: TextView
    private lateinit var loadingContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_vocab)
        // 상태표시줄 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        vocabId = intent.getStringExtra("vocabId") ?: throw IllegalStateException("vocabId가 전달되지 않았습니다.")
        vocabName = intent.getStringExtra("vocabName") ?: throw IllegalStateException("vocabName이 전달되지 않았습니다.")

        auth = FirebaseAuth.getInstance()
        database = Database()

        setupViews()

        loadWords()

        addButton.setOnClickListener {
            addVocabToUser()
        }

    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = vocabName
        addButton = findViewById(R.id.add_btn)
        recyclerView = findViewById(R.id.words_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        noWordText = findViewById(R.id.no_word_text)
        loadingContainer = findViewById(R.id.loading_container)
    }

    private fun loadWords() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)
        database.getWordsFromVocab(userId, vocabId, this) { wordList ->
            showLoading(false)
            if (wordList.isEmpty()) {
                noWordText.visibility = View.VISIBLE
                recyclerView.adapter = null
            } else {
                noWordText.visibility = View.GONE
                adapter = WordAdapter(wordList, {}, {}, {}, isCheckBoxEnabled = false)
                recyclerView.adapter = adapter
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            noWordText.visibility = View.GONE
        } else {
            loadingContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun addVocabToUser() {
        val userId = auth.currentUser?.uid ?: return

        database.isVocabAlreadyAdded(vocabId, userId) { isAlreadyAdded ->
            if (isAlreadyAdded) {
                Toast.makeText(this, "이미 추가된 단어장입니다", Toast.LENGTH_SHORT).show()
            } else {
                database.addVocabToUser(vocabId, userId) { success ->
                    if (success) {
                        Toast.makeText(this, "내 단어장에 추가되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "단어장 추가에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }
        }
    }
}