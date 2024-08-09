package com.example.dictionary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

class WordAdapter(
    private val items: List<Word>,
    private val onCheckChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private val selectedWords = mutableSetOf<Word>()
    private val hiddenEnglishWords = mutableSetOf<String>()
    private val hiddenKoreanWords = mutableSetOf<String>()

    class WordViewHolder(wordView: View) : RecyclerView.ViewHolder(wordView) {
        val checkBox: MaterialCheckBox = wordView.findViewById(R.id.check_btn)
        val engText: TextView = wordView.findViewById(R.id.eng_text)
        val korText: TextView = wordView.findViewById(R.id.kor_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = items[position]

        // 영어 텍스트 설정
        holder.engText.text = if (hiddenEnglishWords.contains(word.id)) "" else word.english
        // 한국어 텍스트 설정
        holder.korText.text = if (hiddenKoreanWords.contains(word.id)) "" else word.korean

        holder.checkBox.setOnCheckedChangeListener(null) // 리스너 초기화
        holder.checkBox.isChecked = selectedWords.contains(word)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedWords.add(word)
            } else {
                selectedWords.remove(word)
            }
            onCheckChanged(selectedWords.isNotEmpty())
        }

        holder.engText.setOnClickListener {
            if (holder.engText.text.isEmpty()) {
                holder.engText.text = word.english
                hiddenEnglishWords.remove(word.id)
            }
        }

        holder.korText.setOnClickListener {
            if (holder.korText.text.isEmpty()) {
                holder.korText.text = word.korean
                hiddenKoreanWords.remove(word.id)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun getSelectedWords(): List<Word> = selectedWords.toList()

    fun clearSelection() {
        selectedWords.clear()
        notifyDataSetChanged()
    }

    fun hideEnglishText() {
        hiddenEnglishWords.addAll(items.map { it.id }) // 모든 영어 텍스트를 숨김
        notifyDataSetChanged()
    }

    fun hideKoreanText() {
        hiddenKoreanWords.addAll(items.map { it.id }) // 모든 한국어 텍스트를 숨김
        notifyDataSetChanged()
    }

    fun resetWords() {
        hiddenEnglishWords.clear()
        hiddenKoreanWords.clear()
        notifyDataSetChanged()
    }
}


