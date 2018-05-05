package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import java.util.*
import kotlinx.android.synthetic.main.activity_entry_handler.*
import kotlinx.android.synthetic.main.inflate_entry_type_new_or_update.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import android.widget.RadioButton
import kotlinx.android.synthetic.main.inflate_entry_type_today_conclude.*
import kotlinx.android.synthetic.main.inflate_entry_type_tomorrow.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class EntryHandlerActivity : AppCompatActivity() {

    //private var mIsNewEntry = true
    private var mCurrentEntryID: Long? = null
    private var mEntryType = 0
    private lateinit var mThisDay: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_handler)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()

        // Set date formatter
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formatDate = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
        )

        // Check what kind of data the function received
        mEntryType = intent.getIntExtra(EXTRA_ENTRY_TYPE_FLAG, 0)
        val inflater = LayoutInflater.from(this@EntryHandlerActivity)

        when (mEntryType) {

            IS_NEW_ENTRY -> {
                inflater.inflate(R.layout.inflate_entry_type_new_or_update, constraintLayoutRoot, true)
                title = String.format(getString(R.string.newEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_UPDATE -> {
                inflater.inflate(R.layout.inflate_entry_type_new_or_update, constraintLayoutRoot, true)
                val extra = intent.getParcelableExtra<MyDayData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.editEntry_label), formatDate.format(dateFormatter.parse(extra.date)))
                mThisDay = extra.date
                mCurrentEntryID = extra.entryID
                fillFormWithExtra(extra)
            }

            // TODO: Implement tomorrow
            IS_TOMORROW -> {
                inflater.inflate(R.layout.inflate_entry_type_tomorrow, constraintLayoutRoot, true)
                title = String.format(getString(R.string.tomorrowEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            // TODO: Implement today as conclude entry
            IS_CONCLUDE -> {
                inflater.inflate(R.layout.inflate_entry_type_today_conclude, constraintLayoutRoot, true)
                val extra = intent.getParcelableExtra<MyDayData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.concludeEntry_label), formatDate.format(dateFormatter.parse(extra.date)))
                mThisDay = extra.date
                mCurrentEntryID = extra.entryID
                fillFormWithExtra(extra)
            }

            else -> finish()
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
        when (mEntryType) {

            IS_UPDATE -> {
                if (data.moodScore != -1)
                    findViewById<RadioButton>(newEntry_moodScoreRadioGroup.getChildAt(data.moodScore).id).isChecked = true

                newEntry_todayFocusBody.setText(data.todayFocus)
                newEntry_todayPrioritiesBody.setText(data.todayPriorities)
                newEntry_learnedTodayBody.setText(data.learnedToday)
                newEntry_avoidTomorrowBody.setText(data.avoidTomorrow)
                newEntry_thankfulForBody.setText(data.thankfulFor)
            }

            IS_CONCLUDE -> {
                concludeEntry_todayFocusBody.text = data.todayFocus
                concludeEntry_todayPrioritiesBody.text = data.todayPriorities
                concludeEntry_learnedTodayBody.setText(data.learnedToday)
                concludeEntry_avoidTomorrowBody.setText(data.avoidTomorrow)
                concludeEntry_thankfulForBody.setText(data.thankfulFor)
            }
        }
    }

    private fun commitData() = launch(UI){
        // Initialize data class
        val dbRow = getDataFromUI()

        // Initialize DB
        val db = MyDayDatabase.getInstance(this@EntryHandlerActivity)

        when (mEntryType) {
            IS_NEW_ENTRY, IS_TOMORROW -> {
                launch(CommonPool) { db?.myDayDAO()?.addNewEntry(dbRow) }
            }

            IS_UPDATE, IS_CONCLUDE -> {
                dbRow.entryID = mCurrentEntryID!!
                launch(CommonPool) { db?.myDayDAO()?.updateCurrentEntry(dbRow) }
            }
        }
        db?.destroyInstance()
        HomeActivity.mDBUFlag = true

        // Send toast and move to different activity
        when (mEntryType) {

            IS_NEW_ENTRY, IS_TOMORROW -> {
                // Toast confirmation for new entry successful
                Toast.makeText(this@EntryHandlerActivity, getString(R.string.newEntry_submitSuccessToast), Toast.LENGTH_LONG).show()

                // Return to home activity
                val intent = Intent(this@EntryHandlerActivity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivityIfNeeded(intent, 0)
                finish()
            }

            IS_UPDATE, IS_CONCLUDE -> {
                // Toast confirmation for entry update successful
                Toast.makeText(this@EntryHandlerActivity, getString(R.string.updateEntry_submitSuccessToast), Toast.LENGTH_LONG).show()

                // Return to CurrentEntry activity
                val intent = Intent(this@EntryHandlerActivity, CurrentEntryActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                intent.apply {
                    putExtra(EXTRA_ENTRY_ID, mCurrentEntryID)
                }
                startActivityIfNeeded(intent, 0)
                finish()
            }
        }
    }

    private fun getDataFromUI(): MyDayData {
        val result = MyDayData()
        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> {
                result.date = mThisDay
                result.moodScore = getMoodScore()
                result.todayFocus = newEntry_todayFocusBody.text.toString()
                result.todayPriorities = newEntry_todayPrioritiesBody.text.toString()
                result.learnedToday = newEntry_learnedTodayBody.text.toString()
                result.avoidTomorrow = newEntry_avoidTomorrowBody.text.toString()
                result.thankfulFor = newEntry_thankfulForBody.text.toString()
                result.concludeFlag = false
            }

            IS_TOMORROW -> { // TODO: Implement layout objects
                result.date = mThisDay
                result.todayFocus = tomorrowEntry_tomorrowFocusBody.text.toString()
                result.todayPriorities = tomorrowEntry_tomorrowPrioritiesBody.text.toString()
                result.concludeFlag = true
            }

            IS_CONCLUDE -> {
                result.entryID = mCurrentEntryID!!
                result.date = mThisDay
                result.moodScore = getMoodScore()
                result.todayFocus = concludeEntry_todayFocusBody.text.toString()
                result.todayPriorities = concludeEntry_todayPrioritiesBody.text.toString()
                result.learnedToday = concludeEntry_learnedTodayBody.text.toString()
                result.avoidTomorrow = concludeEntry_avoidTomorrowBody.text.toString()
                result.thankfulFor = concludeEntry_thankfulForBody.text.toString()
                result.concludeFlag = false
            }
        }
        return result
    }

    private fun getMoodScore(): Int {
        return when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> newEntry_moodScoreRadioGroup.indexOfChild(findViewById<View>(newEntry_moodScoreRadioGroup.checkedRadioButtonId))
            IS_CONCLUDE -> concludeEntry_moodScoreRadioGroup.indexOfChild(findViewById<View>(concludeEntry_moodScoreRadioGroup.checkedRadioButtonId))
            else -> -1
        }
    }

    private fun submitEntry() {

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setPositiveButton(R.string.entry_confirmEmptyFieldTrue) { dialog, _ ->
            commitData()
            dialog.dismiss()
        }

        alertDialogBuilder.setNegativeButton(R.string.entry_confirmEmptyFieldFalse) { dialog, _ ->
            dialog.dismiss()
        }
        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> {
                when {
                    // Mood score is empty
                    getMoodScore() == -1 && mEntryType != IS_TOMORROW -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Today's Focus is empty
                    newEntry_todayFocusBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Today's Priorities is empty
                    newEntry_todayPrioritiesBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Learned Today is empty
                    newEntry_learnedTodayBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Avoid Tomorrow is empty
                    newEntry_avoidTomorrowBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Thankful For is empty
                    newEntry_thankfulForBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialogBuilder.create().show()
                    }
                    else -> commitData()
                }

            }

            IS_TOMORROW -> {
                when {
                    // Tomorrow Focus is empty
                    tomorrowEntry_tomorrowFocusBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Tomorrow Priorities is empty
                    tomorrowEntry_tomorrowPrioritiesBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()

                    }
                    else -> commitData()
                }
            }

            IS_CONCLUDE -> {
                when {
                    // Mood score is empty
                    getMoodScore() == -1 && mEntryType != IS_TOMORROW -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialogBuilder.create().show()
                    }
                    // Conclude Learned Today is empty
                    concludeEntry_learnedTodayBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()

                    }

                    // Conclude Avoid Tomorrow is empty
                    concludeEntry_avoidTomorrowBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()

                    }

                    // Conclude Thankful For is empty
                    concludeEntry_thankfulForBody.text.toString() == "" -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialogBuilder.create().show()

                    }
                    else -> commitData()
                }
            }
        }
    }

    fun onClickListenerEntryHandler(view: View) {

        // TODO: Refactor to comply with newly implemented entry type flags

        when (view) {
            newEntry_submitEntryButton -> {
                submitEntry()
            }
        }
    }

}
