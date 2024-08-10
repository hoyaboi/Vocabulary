package com.example.dictionary.tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.tab.adapter.WordAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class WordsFragment : Fragment() {

    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth
    private lateinit var vocabId: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var editBtnContainer: LinearLayout
    private lateinit var checkBtn: MaterialButton
    private lateinit var deleteBtn: MaterialButton
    private lateinit var hideEngButton: MaterialButton
    private lateinit var hideKorButton: MaterialButton
    private lateinit var resetButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vocabId = arguments?.getString("vocabId") ?: throw IllegalStateException("vocabId가 전달되지 않았습니다.")

        setupViews(view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        database = Database()

        refreshData()

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

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.words_list)
        editBtnContainer = view.findViewById(R.id.edit_btn_container)
        checkBtn = view.findViewById(R.id.check_btn)
        deleteBtn = view.findViewById(R.id.delete_btn)
        hideEngButton = view.findViewById(R.id.hide_eng_btn)
        hideKorButton = view.findViewById(R.id.hide_kor_btn)
        resetButton = view.findViewById(R.id.reset_btn)
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
                database.updateWordCheckedStatus(userId, vocabId, word.id, true)
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
                    Toast.makeText(requireContext(), "단어를 삭제할 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)
        refreshData()
    }

    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return

        database.getWordsFromVocab(userId, vocabId, requireContext()) { wordList ->
            adapter = WordAdapter(wordList) { hasSelectedWords ->
                toggleEditButtonsVisibility(hasSelectedWords)
            }
            recyclerView.adapter = adapter
        }
    }

    companion object {
        fun newInstance(vocabId: String): WordsFragment {
            val fragment = WordsFragment()
            val args = Bundle()
            args.putString("vocabId", vocabId)
            fragment.arguments = args
            return fragment
        }
    }
}



