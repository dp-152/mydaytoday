package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*
import kotlinx.android.synthetic.main.activity_entry_handler.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class EntryHandlerActivity : AppCompatActivity() {

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

        entryHandler_layoutRoot.visibility = View.INVISIBLE
        // Check which entry type the activity is operating on
        mEntryType = intent.getIntExtra(EXTRA_ENTRY_TYPE_FLAG, 0)
        initializeUI()
        entryHandler_layoutRoot.visibility = View.VISIBLE
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
     * Internal functions - Core block
     */
    private fun initializeUI() {
        // Set date formatter
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formatDate = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
        )

        when (mEntryType) {

            IS_NEW_ENTRY -> {
                mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, true)
                shouldDisableDeleteGoalButton() // Called to hide delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.newEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_UPDATE -> {
                mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, true)
                val entryDataExtra = intent.getParcelableExtra<MyDayEntryData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.editEntry_label), formatDate.format(dateFormatter.parse(entryDataExtra.date)))
                mThisDay = entryDataExtra.date
                mCurrentEntryID = entryDataExtra.entryID
                setUIData(entryDataExtra)
            }

            IS_TOMORROW -> {
                entryHandler_moodScoreRadioGroup.visibility = View.GONE
                entryHandler_learnedTodayContainer.visibility = View.GONE
                entryHandler_avoidTomorrowContainer.visibility = View.GONE
                entryHandler_thankfulForContainer.visibility = View.GONE

                entryHandler_todayFocusBody.hint = getString(R.string.tomorrowEntry_tomorrowFocusHint)
                entryHandler_todayPrioritiesBody.hint = getString(R.string.tomorrowEntry_tomorrowPrioritiesHint)

                mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals_no_checkbox, entryHandler_goalsBody, true)
                shouldDisableDeleteGoalButton() // Called to hide delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.tomorrowEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_CONCLUDE -> {
                entryHandler_todayFocusBody.visibility = View.GONE
                entryHandler_todayPrioritiesBody.visibility = View.GONE

                entryHandler_todayFocusBodyStatic.visibility = View.VISIBLE
                entryHandler_todayPrioritiesBodyStatic.visibility = View.VISIBLE

                mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, true)
                val entryDataExtra = intent.getParcelableExtra<MyDayEntryData>(EXTRA_CURRENT_ENTRY_DATA)
                title = String.format(getString(R.string.concludeEntry_label), formatDate.format(dateFormatter.parse(entryDataExtra.date)))
                mThisDay = entryDataExtra.date
                mCurrentEntryID = entryDataExtra.entryID
                setUIData(entryDataExtra)
            }

            else -> finish()
        }
    }

    /**
     * Internal functions - Setters block
     */
    private fun setUIData(extra: MyDayEntryData) {
        if (extra.moodScore != -1)
            findViewById<RadioButton>(entryHandler_moodScoreRadioGroup.getChildAt(extra.moodScore).id).isChecked = true

        if (mEntryType == IS_CONCLUDE) {
            entryHandler_todayFocusBodyStatic.text = extra.todayFocus
            entryHandler_todayPrioritiesBodyStatic.text = extra.todayPriorities
        }
        else {
            entryHandler_todayFocusBody.setText(extra.todayFocus)
            entryHandler_todayPrioritiesBody.setText(extra.todayPriorities)
        }
        setGoals(extra.todayGoals)
        entryHandler_learnedTodayBody.setText(extra.learnedToday)
        entryHandler_avoidTomorrowBody.setText(extra.avoidTomorrow)
        entryHandler_thankfulForBody.setText(extra.thankfulFor)

    }

    private fun setGoals(goals: List<MyDayGoalsData>) {

        for ((index, line) in goals.withIndex()) {
            // Disables inflating on index 0 to prevent empty field at the bottom
            if (index != 0)
                mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, true)

            val goalsRow = entryHandler_goalsBody.getChildAt(index) as ConstraintLayout

            /**
             * Row items:
             *      0 = Checkbox for goal completed input;
             *      1 = EditText for goal body input;
             *      2 = ImageButton for deleting current row;
             *      3 = Invisible TextView for storing goal ID
             */
            (goalsRow.getChildAt(0) as CheckBox).isChecked = line.goalCompleted

            // When entry type = conclude, replaces EditText with static text
            if (mEntryType == IS_CONCLUDE) {
                (goalsRow.getChildAt(0) as CheckBox).text = line.goalBody
                (goalsRow.getChildAt(1) as EditText).visibility = View.GONE
            }
            else
                (goalsRow.getChildAt(1) as EditText).setText(line.goalBody)

            (goalsRow.getChildAt(3) as TextView).text = line.goalID.toString()
        }
        shouldDisableDeleteGoalButton()
    }

    /**
     * Internal functions - Getters block
     */
    private fun getUIData(): MyDayEntryData {
        val result = MyDayEntryData()

        result.date = mThisDay
        result.todayFocus = entryHandler_todayFocusBody.text.toString()
        result.todayPriorities = entryHandler_todayPrioritiesBody.text.toString()
        result.todayGoals = getGoals(entryHandler_goalsBody)
        result.concludeFlag = true

        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE, IS_CONCLUDE -> {
                result.date = mThisDay
                result.moodScore = getMoodScore()
                result.todayFocus = entryHandler_todayFocusBody.text.toString()
                result.todayPriorities = entryHandler_todayPrioritiesBody.text.toString()
                result.todayGoals = getGoals(entryHandler_goalsBody)
                result.learnedToday = entryHandler_learnedTodayBody.text.toString()
                result.avoidTomorrow = entryHandler_avoidTomorrowBody.text.toString()
                result.thankfulFor = entryHandler_thankfulForBody.text.toString()
                result.concludeFlag = false
            }
        }
        return result
    }

    private fun getMoodScore(): Int {
        return entryHandler_moodScoreRadioGroup.indexOfChild(
                findViewById<View>(entryHandler_moodScoreRadioGroup.checkedRadioButtonId
                ))
    }

    // Receives a root for goals, returns a list containing data from all fields
    private fun getGoals(goalsBody: LinearLayout): List<MyDayGoalsData> {
        val result = mutableListOf<MyDayGoalsData>()

        for (i in 0..(goalsBody.childCount - 1)) {
            val row = goalsBody.getChildAt(i) as ConstraintLayout
            val add = MyDayGoalsData()
            val typedBody = (row.getChildAt(1) as EditText).text.toString()
            val staticBody = (row.getChildAt(0) as CheckBox).text.toString()

            add.goalCompleted = (row.getChildAt(0) as CheckBox).isChecked
            add.goalBody = when {
                typedBody.isNotBlank() -> typedBody
                staticBody.isNotBlank() -> staticBody
                else -> EMPTY_STRING
            }

            when (mEntryType) {
                IS_UPDATE, IS_CONCLUDE -> {
                    val idString = (row.getChildAt(3) as TextView).text.toString()
                    if (idString.isNotBlank())
                        add.goalID = idString.toLong()
                    add.dayID = mCurrentEntryID!!
                }
            }

            // Adds to list only if goal contains a body
            if (add.goalBody.isNotBlank())
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

    // Tints delete button with disabled color when there is only one goal field available
    private fun shouldDisableDeleteGoalButton() {

        if (entryHandler_goalsBody.childCount == 1)
            ((entryHandler_goalsBody.getChildAt(0) as ConstraintLayout)
                    .getChildAt(2) as ImageButton)
                    .backgroundTintList = ContextCompat.getColorStateList(
                            this@EntryHandlerActivity,
                            R.color.disabled_button_background_tint
                    )
        else
            ((entryHandler_goalsBody.getChildAt(0) as ConstraintLayout)
                    .getChildAt(2) as ImageButton)
                    .backgroundTintList = ContextCompat.getColorStateList(
                            this@EntryHandlerActivity,
                            R.color.delete_button_background_tint
                    )


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
                    entry.todayFocus.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Today's Priorities is empty
                    entry.todayPriorities.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Goals list is empty
                    entry.todayGoals.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmGoalsEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Learned Today is empty
                    entry.learnedToday.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Avoid Tomorrow is empty
                    entry.avoidTomorrow.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Thankful For is empty
                    entry.thankfulFor.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    else -> submitEntry(entry)
                }

            }

            IS_TOMORROW -> {
                when {
                    // Tomorrow Focus is empty
                    entry.todayFocus.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Tomorrow Priorities is empty
                    entry.todayPriorities.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialogBuilder.create().show()

                    }

                    // Goals list is empty
                    entry.todayGoals.isEmpty() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmGoalsEmptyMessage)
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
                        alertDialogBuilder.setMessage(R.string.entry_confirmGoalsUncheckedMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Learned Today is empty
                    entry.learnedToday.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Avoid Tomorrow is empty
                    entry.avoidTomorrow.isBlank() -> {
                        alertDialogBuilder.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialogBuilder.create().show()
                    }

                    // Conclude Thankful For is empty
                    entry.thankfulFor.isBlank() -> {
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
            entryHandler_submitEntryButton -> {
                assertEntry()
            }
            entryHandler_newGoalButton -> {
                when (mEntryType) {
                    IS_TOMORROW -> mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals_no_checkbox, entryHandler_goalsBody, true)
                    else -> mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, true)
                }
                shouldDisableDeleteGoalButton()
            }
        }
    }

    fun onClickDeleteGoal(view: View) {
        val goalLine = view.parent as View
        val goalRoot = goalLine.parent as ViewGroup
        val idHolder = (goalLine as ConstraintLayout).getChildAt(3) as TextView?
        if (goalRoot.childCount > 1) {
            if (idHolder!!.text.isNotBlank() && idHolder.text != "0")
                mDeletedGoals.add(idHolder.text.toString().toLong())
            goalRoot.removeView(goalLine)
            shouldDisableDeleteGoalButton()
        }
    }

}
