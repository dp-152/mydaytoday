package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_current_entry.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val EXTRA_CURRENT_ENTRY_DATA = "com.example.mzalmeida.mydaytoday.CURRENT_ENTRY_DATA"

class CurrentEntryActivity : AppCompatActivity() {

    private var mEntryID: Long? = 0
    private lateinit var mEntry: MyDayData

    private val parseDate = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val formatDateMed = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_entry)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        asyncGetCurrentEntryData()
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

    private fun asyncGetCurrentEntryData() = launch(UI) {
        // Hide view before query is complete
        currEntry_scrollContainer.visibility = View.INVISIBLE

        // Run database instance
        val db = MyDayDatabase.getInstance(this@CurrentEntryActivity)
        mEntryID = intent.getStringExtra(EXTRA_ENTRY_ID)?.toLongOrNull()
        mEntry = async(CommonPool) { db?.myDayDAO()?.getSelectedDay(mEntryID!!)!! }.await()
        db!!.destroyInstance()

        // Set title on activity bar
        title = formatDateMed.format(parseDate.parse(mEntry.date))

        fillEntry()

        // Show view after populating the fields
        currEntry_scrollContainer.visibility = View.VISIBLE
    }

    private fun asyncDeleteCurrentEntryData() = launch(UI) {

        val db = MyDayDatabase.getInstance(this@CurrentEntryActivity)
        launch(CommonPool) { db?.myDayDAO()?.deleteCurrentEntry(mEntry) }
        db?.destroyInstance()

        HomeActivity.mDBUFlag = true
        Toast.makeText(
                this@CurrentEntryActivity,
                R.string.currentEntry_deleteConfirmationDone,
                Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun fillEntry() {

        currEntry_moodIcon.background = getMoodIcon()

        if (mEntry.todayFocus != "") {
            currEntry_todayFocusBody.text = mEntry.todayFocus
            currEntry_todayFocusBody.visibility = View.VISIBLE
            currEntry_todayFocusTitle.visibility = View.VISIBLE
        }
        else {
            currEntry_todayFocusBody.visibility = View.GONE
            currEntry_todayFocusTitle.visibility = View.GONE
        }
        if (mEntry.todayPriorities != "") {
            currEntry_todayPrioritiesBody.visibility = View.VISIBLE
            currEntry_todayPrioritiesTitle.visibility = View.VISIBLE
            currEntry_todayPrioritiesBody.text = mEntry.todayPriorities
        }
        else {
            currEntry_todayPrioritiesBody.visibility = View.GONE
            currEntry_todayPrioritiesTitle.visibility = View.GONE
        }
        if (mEntry.learnedToday != "") {
            currEntry_learnedTodayBody.visibility = View.VISIBLE
            currEntry_learnedTodayTitle.visibility = View.VISIBLE
            currEntry_learnedTodayBody.text = mEntry.learnedToday
        }
        else {
            currEntry_learnedTodayBody.visibility = View.GONE
            currEntry_learnedTodayTitle.visibility = View.GONE
        }
        if (mEntry.avoidTomorrow != "") {
            currEntry_avoidTomorrowBody.visibility = View.VISIBLE
            currEntry_avoidTomorrowTitle.visibility = View.VISIBLE
            currEntry_avoidTomorrowBody.text = mEntry.avoidTomorrow
        }
        else {
            currEntry_avoidTomorrowBody.visibility = View.GONE
            currEntry_avoidTomorrowTitle.visibility = View.GONE
        }
        if (mEntry.thankfulFor != "") {
            currEntry_thankfulForBody.visibility = View.VISIBLE
            currEntry_thankfulForTitle.visibility = View.VISIBLE
            currEntry_thankfulForBody.text = mEntry.thankfulFor
        }
        else {
            currEntry_thankfulForBody.visibility = View.GONE
            currEntry_thankfulForTitle.visibility = View.GONE
        }

    }

    private fun getMoodIcon(): Drawable {
        return when (mEntry.moodScore) {
            0 -> getDrawable(R.drawable.ic_mood_0_fill)
            1 -> getDrawable(R.drawable.ic_mood_1_fill)
            2 -> getDrawable(R.drawable.ic_mood_2_fill)
            3 -> getDrawable(R.drawable.ic_mood_3_fill)
            4 -> getDrawable(R.drawable.ic_mood_4_fill)
            else -> getDrawable(R.drawable.ic_mood_null)
        }
    }

    fun onClickListenerCurrentEntry(view: View) {
        when (view) {
            currEntry_editCurrEntry -> {
                val intent = Intent(this@CurrentEntryActivity, NewEntryActivity::class.java)
                intent.apply {
                    putExtra(EXTRA_CURRENT_ENTRY_DATA, mEntry)
                }
                startActivity(intent)
            }
            currEntry_deleteCurrEntry -> {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setMessage(R.string.currentEntry_deleteConfirmationMessage)

                alertDialogBuilder.setPositiveButton(R.string.currentEntry_deleteConfirmationTrue) { dialog, _ ->
                    asyncDeleteCurrentEntryData()
                    dialog.dismiss()
                }

                alertDialogBuilder.setNegativeButton(R.string.currentEntry_deleteConfirmationFalse) { dialog, _ ->
                    dialog.dismiss()
                }
                alertDialogBuilder.create().show()
            }
        }
    }
}
