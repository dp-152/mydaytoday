package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_current_entry.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class CurrentEntryActivity : AppCompatActivity() {

    private var mEntryID: Long? = 0
    private var mCanConclude = true
    private lateinit var mEntry: MyDayEntryData

    private val mDateParser = SimpleDateFormat(STRING_DATE_FORMAT, Locale.US)
    private val mDateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())!!

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

        mEntryID = intent.getLongExtra(EXTRA_ENTRY_ID, 0)
        mEntry = async(CommonPool) {
            val db = MyDayDatabase.getInstance(this@CurrentEntryActivity)
            val result = db?.myDayDAO()?.getEntry(mEntryID!!)!!
            db.destroyInstance()
            return@async result
        }.await()

        // Set title on activity bar
        val entryDate = mDateParser.parse(mEntry.date)

        title = mDateFormatter.format(entryDate)

        ViewCompat.setBackgroundTintList(
                currEntry_editCurrEntry,
                ContextCompat.getColorStateList(
                        this@CurrentEntryActivity,
                        R.color.colorAccent
                )
        )
        currEntry_editCurrEntry.setImageDrawable(
                ContextCompat.getDrawable(
                        this@CurrentEntryActivity,
                        R.drawable.ic_edit_white_32px)
        )

        mCanConclude = true
        if (mEntry.mustConcludeFlag) {
            if (isDueConclude(entryDate)) {
                ViewCompat.setBackgroundTintList(
                        currEntry_editCurrEntry,
                        ContextCompat.getColorStateList(
                                this@CurrentEntryActivity,
                                R.color.conclude_button_background_tint)
                )
                currEntry_editCurrEntry.setImageDrawable(
                        ContextCompat.getDrawable(
                                this@CurrentEntryActivity,
                                R.drawable.ic_check_white_32dp)
                )
                mCanConclude = true
            }
            else {
                ViewCompat.setBackgroundTintList(
                        currEntry_editCurrEntry,
                        ContextCompat.getColorStateList(
                                this@CurrentEntryActivity,
                                R.color.colorAccent
                        )
                )
                currEntry_editCurrEntry.setImageDrawable(
                        ContextCompat.getDrawable(
                                this@CurrentEntryActivity,
                                R.drawable.ic_edit_white_32px)
                )
                mCanConclude = false
            }
        }

        fillEntry()

        // Show view after populating the fields
        currEntry_scrollContainer.visibility = View.VISIBLE
    }

    private fun asyncDeleteCurrentEntryData() = launch(UI) {
        launch(CommonPool) {
            val db = MyDayDatabase.getInstance(this@CurrentEntryActivity)
            db?.myDayDAO()?.deleteEntry(mEntry)
            db?.destroyInstance()
        }
        HomeActivity.mDBUFlag = true
        Toast.makeText(
                this@CurrentEntryActivity,
                R.string.currentEntry_deleteConfirmationDone,
                Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun fillEntry() {
        getMood()

        if (mEntry.dayFocus.isNotEmpty() ) {
            currEntry_todayFocusBody.text = mEntry.dayFocus
            currEntry_todayFocusBody.visibility = View.VISIBLE
            currEntry_todayFocusTitle.visibility = View.VISIBLE
        }
        else {
            currEntry_todayFocusBody.visibility = View.GONE
            currEntry_todayFocusTitle.visibility = View.GONE
        }
        if (mEntry.dayPriorities.isNotEmpty() ) {
            currEntry_todayPrioritiesBody.visibility = View.VISIBLE
            currEntry_todayPrioritiesTitle.visibility = View.VISIBLE
            currEntry_todayPrioritiesBody.text = mEntry.dayPriorities
        }
        else {
            currEntry_todayPrioritiesBody.visibility = View.GONE
            currEntry_todayPrioritiesTitle.visibility = View.GONE
        }
        if (mEntry.dayGoals.isNotEmpty()) {
            val inflater = LayoutInflater.from(this@CurrentEntryActivity)
            for ((index, line) in mEntry.dayGoals.withIndex()) {
                if (currEntry_goalsBody.getChildAt(index) == null)
                    inflater.inflate(R.layout.inflate_current_entry_submodule_goals, currEntry_goalsBody)
                val row = currEntry_goalsBody.getChildAt(index) as CheckBox
                row.isChecked = line.goalCompleted
                row.text = line.goalBody
            }
            currEntry_goalsBody.visibility = View.VISIBLE
            currEntry_goalsTitle.visibility = View.VISIBLE
        }
        else {
            currEntry_goalsBody.visibility = View.GONE
            currEntry_goalsTitle.visibility = View.GONE
        }
        if (mEntry.learnedToday.isNotEmpty() ) {
            currEntry_learnedTodayBody.visibility = View.VISIBLE
            currEntry_learnedTodayTitle.visibility = View.VISIBLE
            currEntry_learnedTodayBody.text = mEntry.learnedToday
        }
        else {
            currEntry_learnedTodayBody.visibility = View.GONE
            currEntry_learnedTodayTitle.visibility = View.GONE
        }
        if (mEntry.avoidTomorrow.isNotEmpty() ) {
            currEntry_avoidTomorrowBody.visibility = View.VISIBLE
            currEntry_avoidTomorrowTitle.visibility = View.VISIBLE
            currEntry_avoidTomorrowBody.text = mEntry.avoidTomorrow
        }
        else {
            currEntry_avoidTomorrowBody.visibility = View.GONE
            currEntry_avoidTomorrowTitle.visibility = View.GONE
        }
        if (mEntry.thankfulFor.isNotEmpty() ) {
            currEntry_thankfulForBody.visibility = View.VISIBLE
            currEntry_thankfulForTitle.visibility = View.VISIBLE
            currEntry_thankfulForBody.text = mEntry.thankfulFor
        }
        else {
            currEntry_thankfulForBody.visibility = View.GONE
            currEntry_thankfulForTitle.visibility = View.GONE
        }

    }

    private fun getMood() {
        val background: Drawable
        val description: String
        when (mEntry.moodScore) {
            0 -> {
                background = getDrawable(R.drawable.ic_mood_0_fill)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescription0))
            }
            1 -> {
                background = getDrawable(R.drawable.ic_mood_1_fill)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescription1))
            }
            2 -> {
                background = getDrawable(R.drawable.ic_mood_2_fill)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescription2))
            }
            3 -> {
                background = getDrawable(R.drawable.ic_mood_3_fill)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescription3))
            }
            4 -> {
                background = getDrawable(R.drawable.ic_mood_4_fill)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescription4))
            }
            else -> {
                background = getDrawable(R.drawable.ic_mood_null)
                description = String.format(
                        getString(R.string.currentEntry_moodScoreImageDescription),
                        getString(R.string.currentEntry_moodScoreDescriptionNull))
            }
        }

        currEntry_moodIcon.background = background
        currEntry_moodIcon.contentDescription = description
    }

    private fun isDueConclude(date: Date): Boolean {
        val source = Calendar.getInstance()
        source.time = date
        val today = Calendar.getInstance()

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today.get(Calendar.ERA) >= source.get(Calendar.ERA) &&
                today.get(Calendar.YEAR) >= source.get(Calendar.YEAR)&&
                today.get(Calendar.DAY_OF_YEAR) >= source.get(Calendar.DAY_OF_YEAR)
    }

    fun onClickListenerCurrentEntry(view: View) {
        when (view) {
            currEntry_editCurrEntry -> {
                if (mEntry.mustConcludeFlag) {
                    if (mCanConclude) {
                        val intent = Intent(this@CurrentEntryActivity, EntryHandlerActivity::class.java)
                        intent.apply { putExtra(EXTRA_CURRENT_ENTRY_DATA, mEntry) }
                        intent.apply { putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_CONCLUDE) }
                        startActivity(intent)
                    }
                    else {
                        val intent = Intent(this@CurrentEntryActivity, EntryHandlerActivity::class.java)
                        intent.apply { putExtra(EXTRA_CURRENT_ENTRY_DATA, mEntry) }
                        intent.apply { putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_EDIT_TOMORROW) }
                        startActivity(intent)
                    }
                }
                else {
                    val intent = Intent(this@CurrentEntryActivity, EntryHandlerActivity::class.java)
                    intent.apply { putExtra(EXTRA_CURRENT_ENTRY_DATA, mEntry) }
                    intent.apply { putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_EDIT_ENTRY) }
                    startActivity(intent)
                }
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
