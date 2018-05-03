package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_empty_day.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EmptyDayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_day)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = String.format(
                getString(R.string.emptyDay_label),
                DateFormat.getDateInstance(
                        DateFormat.MEDIUM,
                        Locale.getDefault()
                ).format(
                        SimpleDateFormat(
                                "yyyyMMdd",
                                Locale.US
                        ).parse(
                                intent.getStringExtra(
                                        EXTRA_SELECTED_DATE
                                )
                        )
                )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onClickNewEntry(view: View) {
        when (view) {
            currEntry_newEntry -> {
                val intent = Intent(this, NewEntryActivity::class.java).apply {
                    putExtra(EXTRA_SELECTED_DATE, intent.getStringExtra(EXTRA_SELECTED_DATE))
                }
                startActivity(intent)
            }
        }
    }
}
