package hoya.studio.vocabulary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import hoya.studio.vocabulary.tab.adapter.WordAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class WordsActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth
    private lateinit var vocabId: String
    private lateinit var vocabName: String

    private lateinit var adView: AdView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var addWordButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var editBtnContainer: LinearLayout
    private lateinit var checkBtn: MaterialButton
    private lateinit var deleteBtn: MaterialButton
    private lateinit var hideEngButton: MaterialButton
    private lateinit var hideKorButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var noWordText: TextView
    private lateinit var checkAllButton: FrameLayout
    private lateinit var checkAllButtonImageView: ImageView
    private lateinit var loadingContainer: LinearLayout

    private var isAllChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_words)
        // 상태표시줄 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        vocabId = intent.getStringExtra("vocabId") ?: throw IllegalStateException("vocabId가 전달되지 않았습니다.")
        vocabName = intent.getStringExtra("vocabName") ?: throw IllegalStateException("vocabName이 전달되지 않았습니다.")

        setupViews()
        checkAndRemoveAds()

        // ad 로드
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

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

        checkAllButton.setOnClickListener {
            if (::adapter.isInitialized && adapter.itemCount > 0) {
                toggleSelectAllItems()
            }
        }
    }

    private fun setupViews() {
        adView = findViewById(R.id.ad_view)
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
        noWordText = findViewById(R.id.no_word_text)
        checkAllButton = findViewById(R.id.check_all_btn)
        checkAllButtonImageView = findViewById(R.id.check_all_btn_image)
        loadingContainer = findViewById(R.id.loading_container)
    }

    private fun toggleEditButtonsVisibility(hasSelectedWords: Boolean) {
        if (hasSelectedWords) {
            if (editBtnContainer.visibility == View.GONE) {
                editBtnContainer.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(100)
                        .setListener(null)
                }
            }
        } else {
            if (editBtnContainer.visibility == View.VISIBLE) {
                editBtnContainer.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            editBtnContainer.visibility = View.GONE
                        }
                    })
            }
        }
    }

    private fun updateCheckedStatus() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        selectedWords.forEach { word ->
            database.updateWordToChecked(word.id, vocabId, userId)
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)

        Toast.makeText(this, "선택된 단어가 체크되었습니다", Toast.LENGTH_SHORT).show()
        refreshData()
    }

    private fun deleteSelectedWords() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("단어 삭제")
            .setMessage("선택한 단어를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                selectedWords.forEach { word ->
                    database.deleteWordFromVocab(userId, vocabId, word.id) { success ->
                        if (!success) {
                            Toast.makeText(this, "단어장 생성자만 삭제할 수 있습니다", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "선택된 단어가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                adapter.clearSelection()
                toggleEditButtonsVisibility(false)
                refreshData()
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }

    private fun showSaveWordDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_save_word, null)

        builder.setView(dialogView).setPositiveButton("추가") { _, _ ->
            val engInput = dialogView.findViewById<TextInputEditText>(R.id.word_eng_input)
            val korInput = dialogView.findViewById<TextInputEditText>(R.id.word_kor_input)

            val engWord = engInput.text.toString().trim()
            val korWord = korInput.text.toString().trim()

            if (engWord.isNotEmpty() && korWord.isNotEmpty()) {
                val word = Word(english = engWord, korean = korWord)
                saveWord(word)
            } else {
                Toast.makeText(this, "영어 단어와 한글 뜻을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
            .setTitle("단어 추가")
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

        showLoading(true)

        database.getWordsFromVocab(userId, vocabId, this) { wordList ->
            showLoading(false)

            if (wordList.isEmpty()) {
                noWordText.visibility = View.VISIBLE
                clearAdapter()
            } else {
                noWordText.visibility = View.GONE
                adapter = WordAdapter(
                    wordList,
                    onCheckChanged = { hasSelectedWords ->
                        toggleEditButtonsVisibility(hasSelectedWords)
                    },
                    onAllItemsChecked = { allItemsChecked ->
                        isAllChecked = allItemsChecked
                        updateCheckAllButtonState()
                    },
                    isCheckBoxEnabled = true
                )
                recyclerView.adapter = adapter
                enableCheckAllButton()
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

    private fun clearAdapter() {
        adapter = WordAdapter(
            emptyList(),  // 빈 목록을 가진 어댑터로 초기화
            onCheckChanged = { toggleEditButtonsVisibility(false) },
            onAllItemsChecked = { isAllChecked = false },
            isCheckBoxEnabled = false
        )
        recyclerView.adapter = adapter
        disableCheckAllButton()
    }

    private fun disableCheckAllButton() {
        checkAllButton.isEnabled = false
        checkAllButtonImageView.setImageResource(R.drawable.checkbox_unchecked)
    }

    private fun enableCheckAllButton() {
        checkAllButton.isEnabled = true
    }

    private fun toggleSelectAllItems() {
        isAllChecked = !isAllChecked
        adapter.selectAllItems(isAllChecked)
        updateCheckAllButtonState()
        toggleEditButtonsVisibility(isAllChecked)
    }

    private fun updateCheckAllButtonState() {
        val allCheckedImage = if (isAllChecked) {
            R.drawable.checkbox_checked
        } else {
            R.drawable.checkbox_unchecked
        }
        checkAllButtonImageView.setImageResource(allCheckedImage)
    }

    private fun checkAndRemoveAds() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val paymentRef = Firebase.database.reference.child("users").child(currentUserId).child("payment")
        val rewardRef = Firebase.database.reference.child("users").child(currentUserId).child("reward").child("time")

        paymentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sku = snapshot.child("sku").getValue(String::class.java)
                val purchaseTime = snapshot.child("purchaseTime").getValue(Long::class.java) ?: 0L

                when (sku) {
                    "remove_ad_subs" -> removeAds()
                    "remove_ad_annual" -> {
                        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
                        if (System.currentTimeMillis() - purchaseTime < oneYearInMillis) {
                            removeAds()
                        } else {
                            // 만료된 연간권 정보 삭제
                            paymentRef.removeValue()
                            showAds()
                        }
                    }
                    else -> {
                        rewardRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val rewardTime = snapshot.getValue(Long::class.java) ?: 0L
//                                val oneDayInMillis = 24 * 60 * 60 * 1000
                                val oneDayInMillis = 20 * 1000

                                if (System.currentTimeMillis() - rewardTime < oneDayInMillis) {
                                    removeAds()
                                } else {
                                    showAds()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                showAds()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showAds()
            }
        })
    }

    private fun removeAds() {
        adView.visibility = View.GONE
    }

    private fun showAds() {
        adView.visibility = View.VISIBLE
    }
}
