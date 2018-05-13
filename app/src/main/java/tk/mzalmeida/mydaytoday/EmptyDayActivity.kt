package tk.mzalmeida.mydaytoday

import android.app.Activity
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
                                STRING_DATE_FORMAT,
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_EMPTY_DAY_ENTRY_ADDED -> {
                if (resultCode == Activity.RESULT_OK)
                    finish()
            }
        }
    }

    fun onClickNewEntry(view: View) {
        when (view) {
            currEntry_newEntry -> {
                val intent = Intent(this, EntryHandlerActivity::class.java).apply {
                    putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_NEW_ENTRY)
                    putExtra(EXTRA_SELECTED_DATE, intent.getStringExtra(EXTRA_SELECTED_DATE))
                }
                startActivityForResult(intent, REQUEST_EMPTY_DAY_ENTRY_ADDED)
            }
        }
    }
}
