package com.example.dictionary.tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.WordAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class AllWordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth

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
        return inflater.inflate(R.layout.fragment_all_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                database.updateWordCheckedStatus(word.id, userId, true)
            }
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)
    }

    private fun deleteSelectedWords() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        selectedWords.forEach { word ->
            database.deleteWord(word.id, userId)
        }

        adapter.clearSelection()
        toggleEditButtonsVisibility(false)
        refreshData()
    }

    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return
        database.getWords(userId, requireContext()) { wordList ->
            adapter = WordAdapter(wordList) { hasSelectedWords ->
                toggleEditButtonsVisibility(hasSelectedWords)
            }
            recyclerView.adapter = adapter
        }
    }
}

