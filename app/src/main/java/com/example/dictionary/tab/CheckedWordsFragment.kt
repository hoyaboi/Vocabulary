package com.example.dictionary.tab

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.tab.adapter.WordAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class CheckedWordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WordAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth

    private lateinit var uncheckButton: MaterialButton
    private lateinit var hideEngButton: MaterialButton
    private lateinit var hideKorButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var noCheckedWordText: TextView
    private lateinit var checkAllButton: ImageView

    private var isAllChecked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_checked_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        database = Database()

        refreshData()

        uncheckButton.setOnClickListener {
            updateCheckedStatus()
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

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.words_list)
        uncheckButton = view.findViewById(R.id.uncheck_btn)
        hideEngButton = view.findViewById(R.id.hide_eng_btn)
        hideKorButton = view.findViewById(R.id.hide_kor_btn)
        resetButton = view.findViewById(R.id.reset_btn)
        noCheckedWordText = view.findViewById(R.id.no_checked_word_text)
        checkAllButton = view.findViewById(R.id.check_all_btn)
    }

    private fun toggleEditButtonsVisibility(hasSelectedWords: Boolean) {
        if (hasSelectedWords) {
            if (uncheckButton.visibility == View.GONE) {
                uncheckButton.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(100)
                        .setListener(null)
                }
            }
        } else {
            if (uncheckButton.visibility == View.VISIBLE) {
                uncheckButton.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            uncheckButton.visibility = View.GONE
                        }
                    })
            }
        }
    }

    private fun updateCheckedStatus() {
        val selectedWords = adapter.getSelectedWords()
        val userId = auth.currentUser?.uid ?: return

        checkAllButton.setImageResource(R.drawable.checkbox_unchecked)

        var updateCount = selectedWords.size
        selectedWords.forEach { word ->
            database.updateWordToUnchecked(word.id, userId) {
                updateCount--
                if (updateCount == 0) {
                    adapter.clearSelection()
                    toggleEditButtonsVisibility(false)
                    refreshData()
                }
            }
        }
    }

    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return

        database.getCheckedWords(userId, requireContext()) { checkedWords ->
            if (checkedWords.isEmpty()) {
                noCheckedWordText.visibility = View.VISIBLE
                recyclerView.adapter = null
            } else {
                noCheckedWordText.visibility = View.GONE
                adapter = WordAdapter(
                    checkedWords,
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
            }
            uncheckButton.visibility = View.GONE
        }
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
        checkAllButton.setImageResource(allCheckedImage)
    }
}
