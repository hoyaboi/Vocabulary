package hoya.studio.vocabulary.tab.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hoya.studio.vocabulary.R
import hoya.studio.vocabulary.Vocab

class VocabAdapter(
    private val vocabList: List<Vocab>,
    private val onVocabClicked: (Vocab) -> Unit,
    private val onVocabLongClicked: (Vocab) -> Unit
) : RecyclerView.Adapter<VocabAdapter.VocabViewHolder>() {

    class VocabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vocabNameText: TextView = itemView.findViewById(R.id.vocab_name_text)
        val wordsCountText: TextView = itemView.findViewById(R.id.words_count_text)
        val creatorText: TextView = itemView.findViewById(R.id.creator_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vocabs, parent, false)
        return VocabViewHolder(view)
    }

    override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
        val vocab = vocabList[position]
        holder.vocabNameText.text = vocab.name
        holder.creatorText.text = vocab.owner
        holder.wordsCountText.text = "${vocab.words.size} 단어"

        // 첫 번째 아이템에만 상단에 margin 추가
        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        if (position == 0) {
            layoutParams.topMargin = 15.dpToPx(holder.itemView.context)
        }
        holder.itemView.layoutParams = layoutParams

        holder.itemView.setOnClickListener {
            onVocabClicked(vocab)
        }

        holder.itemView.setOnLongClickListener {
            onVocabLongClicked(vocab)
            true
        }
    }

    override fun getItemCount(): Int = vocabList.size

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
