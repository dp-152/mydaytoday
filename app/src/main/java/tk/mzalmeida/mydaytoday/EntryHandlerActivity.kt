package tk.mzalmeida.mydaytoday

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_entry_handler.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EntryHandlerActivity : AppCompatActivity() {

    /**
     * Global variables block
     */
    private var mCurrentEntryID: Long? = null
    private var mEntryType = 0
    private var mDeleteButtonDisabled = true
    private var mFocusedGoalIndex = 0

    private lateinit var mThisDay: String
    private lateinit var mInflater: LayoutInflater

    private val mDeletedGoals = mutableListOf<Long>()

    companion object {
        private const val GOAL_ROW_CHILD_CHECKBOX   = 0
        private const val GOAL_ROW_CHILD_EDITTEXT   = 1
        private const val GOAL_ROW_CHILD_DELBTN     = 2
        private const val GOAL_ROW_CHILD_IDHOLDER   = 3
    }

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
        val dateFormatter = SimpleDateFormat(STRING_DATE_FORMAT, Locale.US)
        val formatDate = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
        )

        when (mEntryType) {

            IS_NEW_ENTRY -> {
                inflateGoalRow()
                disableDeleteGoalButton() // Called to disable delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.newEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_UPDATE -> {
                inflateGoalRow()
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

                inflateGoalRow()
                disableDeleteGoalButton() // Called to disable delete goal button since there will be only one goal field available on init
                title = String.format(getString(R.string.tomorrowEntry_label), formatDate.format(dateFormatter.parse(intent.getStringExtra(EXTRA_SELECTED_DATE))))
                mThisDay = intent.getStringExtra(EXTRA_SELECTED_DATE)
            }

            IS_CONCLUDE -> {
                entryHandler_todayFocusBody.visibility = View.GONE
                entryHandler_todayPrioritiesBody.visibility = View.GONE

                entryHandler_todayFocusBodyStatic.visibility = View.VISIBLE
                entryHandler_todayPrioritiesBodyStatic.visibility = View.VISIBLE

                inflateGoalRow()
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
            entryHandler_todayFocusBodyStatic.text = extra.dayFocus
            entryHandler_todayPrioritiesBodyStatic.text = extra.dayPriorities
        } else {
            entryHandler_todayFocusBody.setText(extra.dayFocus)
            entryHandler_todayPrioritiesBody.setText(extra.dayPriorities)
        }
        setGoals(extra.dayGoals)
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

            (goalsRow.getChildAt(GOAL_ROW_CHILD_CHECKBOX) as CheckBox).isChecked = line.goalCompleted

            // When entry type = conclude, replaces EditText with static text
            if (mEntryType == IS_CONCLUDE) {
                (goalsRow.getChildAt(GOAL_ROW_CHILD_CHECKBOX) as CheckBox).text = line.goalBody
                (goalsRow.getChildAt(GOAL_ROW_CHILD_EDITTEXT) as EditText).visibility = View.GONE
            } else
                (goalsRow.getChildAt(GOAL_ROW_CHILD_EDITTEXT) as EditText).setText(line.goalBody)

            (goalsRow.getChildAt(GOAL_ROW_CHILD_IDHOLDER) as TextView).text = line.goalID.toString()
        }
        disableDeleteGoalButton()
    }

    /**
     * Internal functions - Getters block
     */
    private fun getUIData(): MyDayEntryData {
        val result = MyDayEntryData()

        result.date = mThisDay
        result.dayFocus = entryHandler_todayFocusBody.text.toString()
        result.dayPriorities = entryHandler_todayPrioritiesBody.text.toString()
        result.dayGoals = getGoals(entryHandler_goalsBody)
        result.mustConcludeFlag = true

        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE, IS_CONCLUDE -> {
                result.moodScore = getMoodScore()
                result.learnedToday = entryHandler_learnedTodayBody.text.toString()
                result.avoidTomorrow = entryHandler_avoidTomorrowBody.text.toString()
                result.thankfulFor = entryHandler_thankfulForBody.text.toString()
                result.mustConcludeFlag = false
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
            val typedBody = (row.getChildAt(GOAL_ROW_CHILD_EDITTEXT) as EditText).text.toString()
            val staticBody = (row.getChildAt(GOAL_ROW_CHILD_CHECKBOX) as CheckBox).text.toString()

            add.goalCompleted = (row.getChildAt(GOAL_ROW_CHILD_CHECKBOX) as CheckBox).isChecked
            add.goalBody = when {
                typedBody.isNotBlank() -> typedBody
                staticBody.isNotBlank() -> staticBody
                else -> STRING_EMPTY
            }

            when (mEntryType) {
                IS_UPDATE, IS_CONCLUDE -> {
                    val idString = (row.getChildAt(GOAL_ROW_CHILD_IDHOLDER) as TextView).text.toString()
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
    // Returns false if any goal within list is marked as completed
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

    // Tints delete button with disabled color when there is only one empty goal field visible
    private fun disableDeleteGoalButton() {
        val firstRow: ConstraintLayout? = entryHandler_goalsBody.getChildAt(0) as ConstraintLayout

        if (firstRow != null) {
            val editText = firstRow.getChildAt(GOAL_ROW_CHILD_EDITTEXT) as EditText
            val delButton = (firstRow.getChildAt(GOAL_ROW_CHILD_DELBTN) as ImageButton)
            when {
                editText.text.isNotEmpty() -> {
                    mDeleteButtonDisabled = false
                    ViewCompat.setBackgroundTintList(
                            delButton, ContextCompat.getColorStateList(
                            this@EntryHandlerActivity,
                            R.color.delete_button_background_tint)
                    )

                }
                entryHandler_goalsBody.childCount == 1 -> {
                    mDeleteButtonDisabled = true
                    ViewCompat.setBackgroundTintList(
                            delButton, ContextCompat.getColorStateList(
                            this@EntryHandlerActivity,
                            R.color.disabled_button_background_tint)
                    )

                }
                else -> {
                    mDeleteButtonDisabled = false
                    ViewCompat.setBackgroundTintList(
                            delButton, ContextCompat.getColorStateList(
                            this@EntryHandlerActivity,
                            R.color.delete_button_background_tint)
                    )
                }
            }
        }
    }

    private fun inflateGoalRow(focus: Boolean = false) {
        val row = mInflater.inflate(R.layout.inflate_entry_handler_submodule_goals, entryHandler_goalsBody, false) as ConstraintLayout
        if (mEntryType == IS_TOMORROW)
            row.getChildAt(GOAL_ROW_CHILD_CHECKBOX).visibility = View.GONE

        val rowEditText = (row.getChildAt(GOAL_ROW_CHILD_EDITTEXT) as EditText)
        rowEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                disableDeleteGoalButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        rowEditText.setOnFocusChangeListener { v, _ ->
            mFocusedGoalIndex = (v.parent.parent as ViewGroup?)?.indexOfChild(v.parent as View?) ?: 0
        }
        if (focus)
            row.getChildAt(GOAL_ROW_CHILD_EDITTEXT).requestFocus()

        entryHandler_goalsBody.addView(row)
    }

    /**
     * Internal functions - Submit block
     */
    // Asserts data in each field. On field empty, pops a prompt. On prompt = yes or no field empty, calls submitEntry()
    private fun assertEntry() {
        val entry = getUIData()
        val alertDialog = AlertDialog.Builder(this@EntryHandlerActivity)
        alertDialog.setPositiveButton(R.string.entry_confirmEmptyFieldTrue) { dialog, _ ->
            submitEntry(entry)
            dialog.dismiss()
        }

        alertDialog.setNegativeButton(R.string.entry_confirmEmptyFieldFalse) { dialog, _ ->
            dialog.dismiss()
        }

        when (mEntryType) {
            IS_NEW_ENTRY, IS_UPDATE -> {
                when {
                // Mood score is empty
                    entry.moodScore == -1 -> {
                        alertDialog.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialog.create().show()
                    }

                // Today's Focus is empty
                    entry.dayFocus.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialog.create().show()
                    }

                // Today's Priorities is empty
                    entry.dayPriorities.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialog.create().show()
                    }

                // Goals list is empty
                    entry.dayGoals.isEmpty() -> {
                        alertDialog.setMessage(R.string.entry_confirmGoalsEmptyMessage)
                        alertDialog.create().show()
                    }

                // Learned Today is empty
                    entry.learnedToday.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialog.create().show()
                    }

                // Avoid Tomorrow is empty
                    entry.avoidTomorrow.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialog.create().show()
                    }

                // Thankful For is empty
                    entry.thankfulFor.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialog.create().show()
                    }

                    else -> submitEntry(entry)
                }

            }

            IS_TOMORROW -> {
                when {
                // Tomorrow Focus is empty
                    entry.dayFocus.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmFocusEmptyMessage)
                        alertDialog.create().show()
                    }

                // Tomorrow Priorities is empty
                    entry.dayPriorities.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmPrioritiesEmptyMessage)
                        alertDialog.create().show()

                    }

                // Goals list is empty
                    entry.dayGoals.isEmpty() -> {
                        alertDialog.setMessage(R.string.entry_confirmGoalsEmptyMessage)
                        alertDialog.create().show()
                    }

                    else -> submitEntry(entry)
                }
            }

            IS_CONCLUDE -> {
                when {
                // Mood score is empty
                    entry.moodScore == -1 -> {
                        alertDialog.setMessage(R.string.entry_confirmMoodScoreEmptyMessage)
                        alertDialog.create().show()
                    }

                // Goals list has no fulfilled goals
                    goalUnchecked(entry.dayGoals) -> {
                        alertDialog.setMessage(R.string.entry_confirmGoalsUncheckedMessage)
                        alertDialog.create().show()
                    }

                // Conclude Learned Today is empty
                    entry.learnedToday.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmLearnedTodayEmptyMessage)
                        alertDialog.create().show()
                    }

                // Conclude Avoid Tomorrow is empty
                    entry.avoidTomorrow.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmAvoidTomorrowEmptyMessage)
                        alertDialog.create().show()
                    }

                // Conclude Thankful For is empty
                    entry.thankfulFor.isBlank() -> {
                        alertDialog.setMessage(R.string.entry_confirmThankfulForEmptyMessage)
                        alertDialog.create().show()
                    }

                    else -> submitEntry(entry)
                }
            }
        }
    }

    // Submit entry to database in background thread
    private fun submitEntry(entry: MyDayEntryData) = launch(UI) {

        // Evaluate entry type and launch add or update
        when (mEntryType) {
            IS_NEW_ENTRY, IS_TOMORROW -> {
                launch(CommonPool) {
                    val db = MyDayDatabase.getInstance(this@EntryHandlerActivity)
                    db?.myDayDAO()?.addEntry(entry)
                    db?.destroyInstance()
                }
            }

            IS_UPDATE, IS_CONCLUDE -> {
                entry.entryID = mCurrentEntryID!!
                launch(CommonPool) {
                    val db = MyDayDatabase.getInstance(this@EntryHandlerActivity)
                    db?.myDayDAO()?.updateEntry(entry, mDeletedGoals)
                    db?.destroyInstance()
                } // Receives auxiliary list for deleted goals
            }
        }
        HomeActivity.mDBUFlag = true

        // Send toast and move to different activity
        when (mEntryType) {

            IS_NEW_ENTRY, IS_TOMORROW -> {
                // Toast confirmation for new entry successful
                Toast.makeText(this@EntryHandlerActivity, getString(R.string.newEntry_submitSuccessToast), Toast.LENGTH_LONG).show()

                // Set result for Empty Day to finish activity on return
                setResult(Activity.RESULT_OK)
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
                inflateGoalRow()
                disableDeleteGoalButton()
            }
        }
    }

    fun onClickDeleteGoal(view: View) {
        if (!mDeleteButtonDisabled) {

            val goalLine = view.parent as View
            val goalRoot = goalLine.parent as ViewGroup
            val idHolder = (goalLine as ConstraintLayout).getChildAt(GOAL_ROW_CHILD_IDHOLDER) as TextView
            val goalLineIndex = goalRoot.indexOfChild(goalLine)
            val childCount = entryHandler_goalsBody.childCount

            goalRoot.removeView(goalLine)

            if (childCount > 1) {

                if (idHolder.text.isNotBlank() && idHolder.text != STRING_ZERO)
                    mDeletedGoals.add(idHolder.text.toString().toLong())

                when (mFocusedGoalIndex) {

                    0 -> {
                        (goalRoot.getChildAt(0) as ConstraintLayout)
                                .getChildAt(GOAL_ROW_CHILD_EDITTEXT)
                                .requestFocus()

                    }
                    goalLineIndex -> {
                        (goalRoot.getChildAt(mFocusedGoalIndex - 1) as ConstraintLayout)
                                .getChildAt(GOAL_ROW_CHILD_EDITTEXT)
                                .requestFocus()
                    }
                }

                if (goalLineIndex in 0..mFocusedGoalIndex)
                    --mFocusedGoalIndex
                disableDeleteGoalButton()
            }
            else {
                if (idHolder.text.isNotBlank() && idHolder.text != STRING_ZERO)
                    mDeletedGoals.add(idHolder.text.toString().toLong())
                inflateGoalRow(true)
                disableDeleteGoalButton()
            }
        }
    }
}
