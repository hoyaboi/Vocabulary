package hoya.studio.vocabulary

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hoya.studio.vocabulary.R

class RemoveAd : AppCompatActivity() {
    private lateinit var originPriceText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_ad)
        // 상태표시줄 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupViews()


    }

    private fun setupViews() {
        originPriceText = findViewById(R.id.org_price_text)
        originPriceText.paintFlags = originPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    }
}