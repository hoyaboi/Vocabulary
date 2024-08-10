package com.example.dictionary.tab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.Vocab
import com.example.dictionary.WordsActivity
import com.example.dictionary.tab.adapter.VocabAdapter
import com.example.dictionary.tab.adapter.WordAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class VocabularyFragment : Fragment() {

    private lateinit var adapter: VocabAdapter
    private lateinit var database: Database
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var addVocabButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vocabulary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        database = Database()

        refreshData()

        addVocabButton.setOnClickListener {
            showAddVocabDialog()
        }
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.vocab_list)
        addVocabButton = view.findViewById(R.id.add_vocab_btn)
    }

    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return
        database.getVocabsForUser(userId, requireContext()) { vocabList ->
            adapter = VocabAdapter(vocabList) { vocab ->
                openVocab(vocab)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun showAddVocabDialog() {
        // 단어장 추가를 위한 다이얼로그 표시
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_vocab, null)

        builder.setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val vocabNameInput = dialogView.findViewById<TextInputEditText>(R.id.vocab_name_input)
                val vocabName = vocabNameInput.text.toString()

                if (vocabName.isNotEmpty()) {
                    addVocab(vocabName)
                } else {
                    Toast.makeText(requireContext(), "단어장 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }

    private fun addVocab(vocabName: String) {
        val userId = auth.currentUser?.uid ?: return

        database.createVocab(vocabName, userId, requireContext()) {
            refreshData()
        }
    }

    private fun openVocab(vocab: Vocab) {
        val intent = Intent(requireContext(), WordsActivity::class.java)
        intent.putExtra("vocabId", vocab.id)
        intent.putExtra("vocabName", vocab.name)
        startActivity(intent)
    }
}

