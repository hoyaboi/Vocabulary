package com.example.dictionary.tab

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.words_list)
        uncheckButton = view.findViewById(R.id.uncheck_btn)
        hideEngButton = view.findViewById(R.id.hide_eng_btn)
        hideKorButton = view.findViewById(R.id.hide_kor_btn)
        resetButton = view.findViewById(R.id.reset_btn)
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

        var updateCount = selectedWords.size
        selectedWords.forEach { word ->
//            Log.d("selectedWords", "selectedWords: $word")
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
            adapter = WordAdapter(checkedWords, onCheckChanged = { hasSelectedWords ->
                toggleEditButtonsVisibility(hasSelectedWords)
            }, isCheckBoxEnabled = true)
            recyclerView.adapter = adapter
        }
    }


    override fun onResume() {
        super.onResume()
        refreshData()  // 화면이 다시 보일 때마다 데이터 갱신
    }
}