package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*
import kotlinx.android.synthetic.main.activity_entry_handler.*
import kotlinx.android.synthetic.main.inflate_entry_type_new_or_update.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import kotlinx.android.synthetic.main.inflate_entry_type_today_conclude.*
import kotlinx.android.synthetic.main.inflate_entry_type_tomorrow.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class EntryHandlerActivity : AppCompatActivity() {

    // TODO: Reformat entry fields and inflated layouts - use hide instead of inflate
    /**
     * Global variables block
     */
    private var mCurrentEntryID: Long? = null
    private var mEntryType = 0

    private lateinit var mThisDay: String
    private lateinit var mInflater: LayoutInflater

    private val mDeletedGoals = mutableListOf<Long>()

    /**
     * Overrides block
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_handler)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mInflater = LayoutInflater.from(this@EntryHandlerActivity)
    }

    override fun onResume() {
        super.onResume()

        // Set date formatter
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formatDate = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
        )

        // Check which entry type the activity is operating on
        mEntryType = intent.getIntExtra(EXTRA_ENTRY_TYPE_FLAG, 0)

        when (mEntryType) {

            IS_NEW_ENTRY -> {
                mInflater.inflate(R.layout.inflate_entry_type_new_or_update, constraintLayoutRoot, true)
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, newEntry_goalsBody, true)
                shouldHideDeleteGoalButton() // Called to hide delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.newEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_UPDATE -> {
                mInflater.inflate(R.layout.inflate_entry_type_new_or_update, constraintLayoutRoot, true)
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, newEntry_goalsBody, true)
                val entryDataExtra = intent.getParcelableExtra<MyDayEntryData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.editEntry_label), formatDate.format(dateFormatter.parse(entryDataExtra.date)))
                mThisDay = entryDataExtra.date
                mCurrentEntryID = entryDataExtra.entryID
                setUIData(entryDataExtra)
            }

            IS_TOMORROW -> {
                mInflater.inflate(R.layout.inflate_entry_type_tomorrow, constraintLayoutRoot, true)
                mInflater.inflate(R.layout.inflate_entry_submodule_goals_no_checkbox, tomorrowEntry_goalsBody, true)
                shouldHideDeleteGoalButton() // Called to hide delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.tomorrowEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_CONCLUDE -> {
                mInflater.inflate(R.layout.inflate_entry_type_today_conclude, constraintLayoutRoot, true)
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, concludeEntry_goalsBody, true)
                val entryDataExtra = intent.getParcelableExtra<MyDayEntryData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.concludeEntry_label), formatDate.format(dateFormatter.parse(entryDataExtra.date)))
                mThisDay = entryDataExtra.date
                mCurrentEntryID = entryDataExtra.entryID
                setUIData(entryDataExtra)
            }

            else -> finish()
        }
    }

    // Override for back button
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Internal functions - Setters block
     */
    private fun setUIData(extra: MyDayEntryData) {
        when (mEntryType) {

            IS_UPDATE -> {
                if (extra.moodScore != -1)
                    findViewById<RadioButton>(newEntry_moodScoreRadioGroup.getChildAt(extra.moodScore).id).isChecked = true

                newEntry_todayFocusBody.setText(extra.todayFocus)
                newEntry_todayPrioritiesBody.setText(extra.todayPriorities)
                setGoals(extra.todayGoals, newEntry_goalsBody)
                newEntry_learnedTodayBody.setText(extra.learnedToday)
                newEntry_avoidTomorrowBody.setText(extra.avoidTomorrow)
                newEntry_thankfulForBody.setText(extra.thankfulFor)
            }

            IS_CONCLUDE -> {
                concludeEntry_todayFocusBody.text = extra.todayFocus
                concludeEntry_todayPrioritiesBody.text = extra.todayPriorities
                setGoals(extra.todayGoals, concludeEntry_goalsBody)
                concludeEntry_learnedTodayBody.setText(extra.learnedToday)
                concludeEntry_avoidTomorrowBody.setText(extra.avoidTomorrow)
                concludeEntry_thankfulForBody.setText(extra.thankfulFor)
            }
        }
    }

    private fun setGoals(goals: List<MyDayGoalsData>, goalsRoot: LinearLayout) {

        for ((index, line) in goals.withIndex()) {
            // Prevents inflating on index 0 to avoid empty field at the bottom
            if (index != 0)
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, goalsRoot, true)

            val goalsRow = goalsRoot.getChildAt(index) as ConstraintLayout

            /**
             * Row items:
             *      0 = Checkbox for goal completed;
             *      1 = EditText for goal body;
             *      2 = ImageButton for deleting current row;
             *      3 = Invisible TextView for storing goal ID
             */
            (goalsRow.getChildAt(0) as CheckBox).isChecked = line.goalCompleted
            (goalsRow.getChildAt(1) as EditText).setText(line.goalBody)
            (goalsRow.getChildAt(3) as TextView).text = line.goalID.toString()
        }
        shouldHideDeleteGoalButton()
    }

    /**
     * Internal functions - Getters block
     */
    private fun getUIData(): MyDayEntryData {
        val result = MyDayEntryData()
        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> {
                result.date = mThisDay
                result.moodScore = getMoodScore(newEntry_moodScoreRadioGroup)
                result.todayFocus = newEntry_todayFocusBody.text.toString()
                result.todayPriorities = newEntry_todayPrioritiesBody.text.toString()
                result.todayGoals = getGoals(newEntry_goalsBody)
                result.learnedToday = newEntry_learnedTodayBody.text.toString()
                result.avoidTomorrow = newEntry_avoidTomorrowBody.text.toString()
                result.thankfulFor = newEntry_thankfulForBody.text.toString()
                result.concludeFlag = false
            }

            IS_TOMORROW -> {
                result.date = mThisDay
                result.todayFocus = tomorrowEntry_tomorrowFocusBody.text.toString()
                result.todayPriorities = tomorrowEntry_tomorrowPrioritiesBody.text.toString()
                result.todayGoals = getGoals(tomorrowEntry_goalsBody)
                result.concludeFlag = true
            }

            IS_CONCLUDE -> {
                result.entryID = mCurrentEntryID!!
                result.date = mThisDay
                result.moodScore = getMoodScore(concludeEntry_moodScoreRadioGroup)
                result.todayFocus = concludeEntry_todayFocusBody.text.toString()
                result.todayPriorities = concludeEntry_todayPrioritiesBody.text.toString()
                result.todayGoals = getGoals(concludeEntry_goalsBody)
                result.learnedToday = concludeEntry_learnedTodayBody.text.toString()
                result.avoidTomorrow = concludeEntry_avoidTomorrowBody.text.toString()
                result.thankfulFor = concludeEntry_thankfulForBody.text.toString()
                result.concludeFlag = false
            }
        }
        return result
    }

    private fun getMoodScore(moodScoreBody: RadioGroup): Int {
        return moodScoreBody.indexOfChild(findViewById<View>(moodScoreBody.checkedRadioButtonId))
    }

    // Receives a root for goals, returns a list containing data from all fields
    private fun getGoals(goalsBody: LinearLayout): List<MyDayGoalsData> {
        val result = mutableListOf<MyDayGoalsData>()

        for (i in 0..(goalsBody.childCount - 1)) {
            val row = goalsBody.getChildAt(i) as ConstraintLayout
            val add = MyDayGoalsData()

            add.goalCompleted = (row.getChildAt(0) as CheckBox).isChecked
            add.goalBody = (row.getChildAt(1) as EditText).text.toString()

            when (mEntryType) {
                IS_UPDATE, IS_CONCLUDE -> {
                    val idString = (row.getChildAt(3) as TextView).text.toString()
                    if (idString.isNotEmpty())
                        add.goalID = idString.toLong()
                    add.dayID = mCurrentEntryID!!
                }
            }

            // Adds to list only if goal contains a body
            if (add.goalBody.isNotEmpty())
                result.add(add)
        }
        return result
    }

    /**
     * Internal functions - Auxiliary functions block
     */
    // Receives a root for goals, returns false if any goal checkbox within root is checked
    private fun goalUnchecked(goalsList: List<MyDayGoalsData>): Boolean {
        var result = true
        for (line in goalsList) {
            if (line.goalCompleted) {
                result = false
                break
            }
        }
        return result
    }

    // Hides delete button when there is only one goal field available
    private fun shouldHideDeleteGoalButton() {
        val viewRoot = when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> newEntry_goalsBody
            IS_TOMORROW -> tomorrowEntry_goalsBody
            IS_CONCLUDE -> concludeEntry_goalsBody
            else -> null
        }

        if (viewRoot!!.childCount == 1)
            ((viewRoot.getChildAt(0) as ConstraintLayout).getChildAt(2) as ImageButton).visibility = View.INVISIBLE
        else
            ((viewRoot.getChildAt(0) as ConstraintLayout).getChildAt(2) as ImageButton).visibility = View.VISIBLE


    }

    /**
     * Internal functions - Submit block
     */
    // Asserts data in each field. On field empty, pops a prompt. On prompt = yes or no field empty, calls submitEntry()
    private fun assertEntry() {
        val entry = getUIData()
        val alertDialogBuilder = AlertDialog.Builder(this@EntryHandlerActivity)
        alertDialogBuilder.setPositiveButton(R.string.entry_confirmEmptyFieldTrue) { dialog, _ ->
            submitEntry(entry)
            dialog.dismiss()
        }

        alertDialogBuilder.setNegativeButton(R.string.entry_confirmEmptyFieldFalse) { dialog, _ ->
            dialog.dismiss()
        }

        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> {
                when {
                    // Mood score is empty
                    entry.moodScore == -1 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Today's Focus is empty
                    entry.todayFocus.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Today's Priorities is empty
                    entry.todayPriorities.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Goals list is empty
                    entry.todayGoals.isEmpty() -> {
                        alertDialogBuilder.setMessage("SomeString") // TODO: String for goals empty
                        alertDialogBuilder.create().show()
                    }

                    // Learned Today is empty
                    entry.learnedToday.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Avoid Tomorrow is empty
                    entry.avoidTomorrow.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Thankful For is empty
                    entry.thankfulFor.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    else -> submitEntry(entry)
                }

            }

            IS_TOMORROW -> {
                when {
                    // Tomorrow Focus is empty
                    entry.todayFocus.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Tomorrow Priorities is empty
                    entry.todayPriorities.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()

                    }

                    // Goals list is empty
                    entry.todayGoals.isEmpty() -> {
                        alertDialogBuilder.setMessage("SomeString") // TODO: String for goals empty
                        alertDialogBuilder.create().show()
                    }

                    else -> submitEntry(entry)
                }
            }

            IS_CONCLUDE -> {
                when {
                    // Mood score is empty
                    entry.moodScore == -1 -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Goals list has no fulfilled goals
                    goalUnchecked(entry.todayGoals) -> {
                        alertDialogBuilder.setMessage("SomeString-unchecked") // TODO: String for no goal checked
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Learned Today is empty
                    entry.learnedToday.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Avoid Tomorrow is empty
                    entry.avoidTomorrow.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Thankful For is empty
                    entry.thankfulFor.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    else -> submitEntry(entry)
                }
            }
        }
    }

    // Submit entry to database in background thread
    private fun submitEntry(entry: MyDayEntryData) = launch(UI){

        // Initialize DB
        val db = MyDayDatabase.getInstance(this@EntryHandlerActivity)

        // Evaluate entry type and launch add or update
        when (mEntryType) {
            IS_NEW_ENTRY, IS_TOMORROW -> {
                launch(CommonPool) { db?.myDayDAO()?.addEntry(entry) }
            }

            IS_UPDATE, IS_CONCLUDE -> {
                entry.entryID = mCurrentEntryID!!
                launch(CommonPool) { db?.myDayDAO()?.updateEntry(entry, mDeletedGoals) } // Receives auxiliary list for deleted goals
            }
        }
        // Destroy database instance to prevent leaks and flip Database Updated flag
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


    /**
     * Public functions - Button listeners block
     */
    fun onClickListenerEntryHandler(view: View) {

        when (view) {
            entry_submitEntryButton -> {
                assertEntry()
            }

            newEntry_newGoalButton -> {
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, newEntry_goalsBody, true)
                shouldHideDeleteGoalButton()
            }

            concludeEntry_newGoalButton -> {
                mInflater.inflate(R.layout.inflate_entry_submodule_goals, concludeEntry_goalsBody, true)
                shouldHideDeleteGoalButton()
            }

            tomorrowEntry_newGoalButton -> {
                mInflater.inflate(R.layout.inflate_entry_submodule_goals_no_checkbox, tomorrowEntry_goalsBody, true)
                shouldHideDeleteGoalButton()
            }
        }
    }

    fun onClickDeleteGoal(view: View) {
        val goalLine = view.parent as View
        val goalRoot = goalLine.parent as ViewGroup
        val idHolder = (goalLine as ConstraintLayout).getChildAt(3) as TextView?
        if (goalRoot.childCount > 1) {
            if (idHolder != null && idHolder.text != "0")
                mDeletedGoals.add(idHolder.text.toString().toLong())
            goalRoot.removeView(goalLine)
            shouldHideDeleteGoalButton()
        }
    }

}
