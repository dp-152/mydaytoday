package tk.mzalmeida.mydaytoday

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*
import java.text.SimpleDateFormat
import java.util.*
import com.prolificinteractive.materialcalendarview.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI


// TODO: implement menu (options, help/about)
class HomeActivity : AppCompatActivity() {

    private var mListEntries: List<CalendarDayData>? = null
    private var mConcludeToday: MyDayEntryData? = null

    private lateinit var mCurrentCalendar: Calendar
    private lateinit var mTomorrowCalendar: Calendar

    private val mFormatYearMonthDay = SimpleDateFormat("yyyyMMdd", Locale.US)

    companion object {
        // Public flag for DB Updated
        var mDBUFlag = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set flag to refresh calendar on create
        mDBUFlag = true

        // Setting up the calendar
        mCurrentCalendar = Calendar.getInstance(Locale.getDefault())

        mTomorrowCalendar = Calendar.getInstance(Locale.getDefault())
        mTomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1)

        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.add(Calendar.YEAR, 1)
        maxDateCalendar.set(Calendar.DAY_OF_MONTH, 0)

        // TODO: Dynamic minimum date
        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.set(2016, 0, 1)

        home_calendarView.state().edit()
                .setMaximumDate(maxDateCalendar)
                .setMinimumDate(minDateCalendar)
                .commit()
    }

    override fun onResume() {
        super.onResume()

        // Check if DB has been updated - Case true, refresh calendar
        if (mDBUFlag) {
            asyncGetCalendarData()
        }
    }

    // Custom listener for day selected
    inner class MyOnDateSelectedListener : OnDateSelectedListener {

        override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
            widget.clearSelection()
            val thisDateAsString: String = mFormatYearMonthDay.format(date.calendar.time)
            val thisDateInDBIndex = mListEntries!!.map { it.date == thisDateAsString }.indexOf(true)
            if (thisDateInDBIndex != -1) {
                val intent = Intent(this@HomeActivity, CurrentEntryActivity::class.java).apply {
                    putExtra(EXTRA_ENTRY_ID, mListEntries!![thisDateInDBIndex].entryID)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this@HomeActivity, EmptyDayActivity::class.java).apply {
                    putExtra(EXTRA_SELECTED_DATE, thisDateAsString)
                }
                startActivity(intent)
            }
        }
    }

    private fun asyncGetCalendarData() = launch(UI) {
        // TODO: Hide entire container
        home_rootLayout.visibility = View.INVISIBLE

        // TODO: On reload, retrieve only newly inserted or updated data
        var db = MyDayDatabase.getInstance(this@HomeActivity)
        mListEntries = async(CommonPool) { db?.myDayDAO()?.getCalendarData()!! }.await()
        db!!.destroyInstance()

        // Clear and update day decorators
        home_calendarView.removeDecorators()

        home_calendarView.addDecorators(
                DisabledDecorator(this@HomeActivity),
                MoodDecorator0(sortMoods(0).await(), this@HomeActivity),
                MoodDecorator1(sortMoods(1).await(), this@HomeActivity),
                MoodDecorator2(sortMoods(2).await(), this@HomeActivity),
                MoodDecorator3(sortMoods(3).await(), this@HomeActivity),
                MoodDecorator4(sortMoods(4).await(), this@HomeActivity),
                MoodDecoratorNull(sortMoods(-1).await(), this@HomeActivity)
        )

        // Set listener for day selected
        // TODO: Create date picker
        home_calendarView.setOnDateChangedListener(MyOnDateSelectedListener())

        // Hide new entry button if there is already an entry for today
        val todayEntry = mListEntries!!.map { it.date == mFormatYearMonthDay.format(mCurrentCalendar.timeInMillis) }
        val tomorrowEntry = mListEntries!!.map { it.date == mFormatYearMonthDay.format(mTomorrowCalendar.timeInMillis) }

        // If entry for next day does not exist in DB, display new entry button
        if (tomorrowEntry.indexOf(true) != -1)
            home_tomorrowEntryButton.visibility = View.GONE
        else home_tomorrowEntryButton.visibility = View.VISIBLE

        // If entry for today exists and update flag is set, display complete entry button
        if (todayEntry.indexOf(true) != -1 && mListEntries!![todayEntry.indexOf(true)].concludeFlag) {

            val mapToday = mListEntries!!.map { it.date == mFormatYearMonthDay.format(mCurrentCalendar.timeInMillis) }.indexOf(true)
            if (mapToday != -1) {
                db = MyDayDatabase.getInstance(this@HomeActivity)
                mConcludeToday = async(CommonPool) { db?.myDayDAO()?.getEntry(mListEntries!![mapToday].entryID)!! }.await()
                db!!.destroyInstance()
            }
            home_completeEntryButton.visibility = View.VISIBLE
        }
        else {
            mConcludeToday = null
            home_completeEntryButton.visibility = View.GONE
        }

        // Reset DB Updated flag
        mDBUFlag = false

        home_rootLayout.visibility = View.VISIBLE
    }

    private fun sortMoods(mood: Int) = async(CommonPool){
        val result = mutableListOf<CalendarDay>()

        for (line in mListEntries!!) {
            if (line.moodScore == mood) {
                val year = line.date.substring(0, 4).toInt()
                val month = line.date.substring(4, 6).toInt() - 1
                val day = line.date.substring(6, 8).toInt()
                result.add(CalendarDay.from(year, month, day))
            }
        }
        return@async result
    }

    fun onClickListenerHome(view: View) {
        // TODO: Handle date forwarding here
        val intent = Intent(this, EntryHandlerActivity::class.java)
        when (view) {
            home_tomorrowEntryButton -> {
                intent.apply { putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_TOMORROW) }
                intent.apply { putExtra(EXTRA_SELECTED_DATE, mFormatYearMonthDay.format(mTomorrowCalendar.timeInMillis)) }
            }
            home_completeEntryButton -> {
                intent.apply { putExtra(EXTRA_ENTRY_TYPE_FLAG, IS_CONCLUDE) }
                intent.apply { putExtra(EXTRA_CURRENT_ENTRY_DATA, mConcludeToday) }
            }
        }
        startActivity(intent)
    }
}
