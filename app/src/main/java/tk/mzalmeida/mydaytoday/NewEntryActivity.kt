package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import java.util.*
import kotlinx.android.synthetic.main.activity_new_entry.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import android.widget.RadioButton
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class NewEntryActivity : AppCompatActivity() {

    private var mIsNewEntry = true
    private var mCurrentEntryID: Long? = null
    private lateinit var mToday: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_entry)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()

        // Set flag for new entry
        mIsNewEntry = true

        // Set date formatter
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formatDate = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
        )

        // Check what kind of data the function received
        when {

        // Coming from empty day
            intent.getStringExtra(EXTRA_SELECTED_DATE) != null -> {
                title = String.format(getString(R.string.newEntry_label),formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mToday = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

        // Coming from existing entry
            intent.getParcelableExtra<MyDayData>(EXTRA_CURRENT_ENTRY_DATA) != null -> {
                mIsNewEntry = false
                val extra = intent.getParcelableExtra<MyDayData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.editEntry_label),formatDate.format(dateFormatter.parse(extra.date)))
                mToday = extra.date
                mCurrentEntryID = extra.entryID
                fillFormWithExtra(extra)
            }

        // Creating new entry
            else -> {
                mToday = dateFormatter.format(Date())
                title = String.format(getString(R.string.newEntry_labelToday),formatDate.format(dateFormatter.parse(mToday)))
            }
        }
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

    private fun fillFormWithExtra(data: MyDayData) {
        if (data.moodScore != -1)
            findViewById<RadioButton>(radioGroup_moodScoreBody.getChildAt(data.moodScore).id).isChecked = true

        newEntry_todayFocusBody.setText(data.todayFocus)
        newEntry_todayPrioritiesBody.setText(data.todayPriorities)
        newEntry_learnedTodayBody.setText(data.learnedToday)
        newEntry_avoidTomorrowBody.setText(data.avoidTomorrow)
        newEntry_thankfulForBody.setText(data.thankfulFor)
    }
    fun onClickSubmitEntry(view: View) {
                
        when (view) {
            newEntry_submitEntryButton -> {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setPositiveButton(R.string.entry_confirmEmptyFieldTrue) { dialog, _ ->
                    commitData()
                    dialog.dismiss()
                }

                alertDialogBuilder.setNegativeButton(R.string.entry_confirmEmptyFieldFalse) { dialog, _ ->
                    dialog.dismiss()
                }

                when (assertFields()) {
                // Mood score is empty
                    1 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmMoodScoreMessage)
                        alertDialogBuilder.create().show()
                    }

                // Today's Focus is empty
                    2 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmTodayFocusMessage)
                        alertDialogBuilder.create().show()
                    }

                // Today's Priorities is empty
                    3 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmTodayPrioritiesMessage)
                        alertDialogBuilder.create().show()
                    }

                // Learned Today is empty
                    4 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayMessage)
                        alertDialogBuilder.create().show()
                    }

                // Avoid Tomorrow is empty
                    5 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowMessage)
                        alertDialogBuilder.create().show()
                    }

                // Thankful For is empty
                    6 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForMessage)
                        alertDialogBuilder.create().show()
                    }

                // No empty fields
                    else -> commitData()
                }
            }
        }
    }

    private fun commitData() = launch(UI){
        // Initialize DB
        // Initialize data class
        val dbRow = MyDayData()

        // Set rows according to form fields
        dbRow.date = mToday
        dbRow.moodScore = getMoodScore()
        dbRow.todayFocus = newEntry_todayFocusBody.text.toString()
        dbRow.todayPriorities = newEntry_todayPrioritiesBody.text.toString()
        dbRow.learnedToday = newEntry_learnedTodayBody.text.toString()
        dbRow.avoidTomorrow = newEntry_avoidTomorrowBody.text.toString()
        dbRow.thankfulFor = newEntry_thankfulForBody.text.toString()


        val db = MyDayDatabase.getInstance(this@NewEntryActivity)

        if (mIsNewEntry) {
            launch(CommonPool) { db?.myDayDAO()?.addNewEntry(dbRow) }
        }
        else {
            dbRow.entryID = mCurrentEntryID!!
            launch(CommonPool) { db?.myDayDAO()?.updateCurrentEntry(dbRow) }
        }

        db?.destroyInstance()
        HomeActivity.mDBUFlag = true

        if (mIsNewEntry) {
            // Toast confirmation for new entry successful
            Toast.makeText(this@NewEntryActivity, getString(R.string.newEntry_submitSuccessNew), Toast.LENGTH_LONG).show()

            // Return to home activity
            val intent = Intent(this@NewEntryActivity, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivityIfNeeded(intent, 0)
            finish()
        }
        else {
            // Toast confirmation for entry update successful
            Toast.makeText(this@NewEntryActivity, getString(R.string.newEntry_submitSuccessUpdate), Toast.LENGTH_LONG).show()

            // Return to CurrentEntry activity
            val intent = Intent(this@NewEntryActivity, CurrentEntryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.apply {
                putExtra(EXTRA_ENTRY_ID, mCurrentEntryID.toString())
            }
            startActivityIfNeeded(intent, 0)
            finish()
        }
    }

    private fun assertFields(): Int {
        return when {
            getMoodScore() == -1                                -> 1
            newEntry_todayFocusBody.text.toString() == ""       -> 2
            newEntry_todayPrioritiesBody.text.toString() == ""  -> 3
            newEntry_learnedTodayBody.text.toString() == ""     -> 4
            newEntry_avoidTomorrowBody.text.toString() == ""    -> 5
            newEntry_thankfulForBody.text.toString() == ""      -> 6
            else                                                -> -1

        }
    }

    // Better.
    private fun getMoodScore(): Int {
        return radioGroup_moodScoreBody.indexOfChild(findViewById<View>(radioGroup_moodScoreBody.checkedRadioButtonId))
    }
}
