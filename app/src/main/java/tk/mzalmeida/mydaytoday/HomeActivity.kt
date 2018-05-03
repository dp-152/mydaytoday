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

const val EXTRA_ENTRY_ID: String = "com.example.mzalmeida.mydaytoday.ENTRY_ID"
const val EXTRA_SELECTED_DATE: String = "com.example.mzalmeida.mydaytoday.SELECTED_DATE"

// TODO: implement menu (options, help/about)
class HomeActivity : AppCompatActivity() {

    private var mListEntries: List<PartialMyDayData>? = null

    private lateinit var mCurrentCalendar: Calendar

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

        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.add(Calendar.YEAR, 1)
        maxDateCalendar.set(Calendar.DAY_OF_MONTH, 0)

        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.set(2016, 0, 1)

        calendarView.state().edit()
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
            val thisCalendar = date.calendar
            val selectedDate: String = mFormatYearMonthDay.format(thisCalendar.time)
            val thisDayIs = mListEntries!!.map { it.date == selectedDate }
            val thisDayIndex = thisDayIs.indexOf(true)
            if (thisDayIndex != -1) {
                val intent = Intent(this@HomeActivity, CurrentEntryActivity::class.java).apply {
                    putExtra(EXTRA_ENTRY_ID, mListEntries!![thisDayIndex].entryID.toString())
                }
                startActivity(intent)
            } else {
                val intent = Intent(this@HomeActivity, EmptyDayActivity::class.java).apply {
                    putExtra(EXTRA_SELECTED_DATE, selectedDate)
                }
                startActivity(intent)
            }
        }
    }

    private fun asyncGetCalendarData() = launch(UI) {
        calendarView.visibility = View.INVISIBLE

        // TODO: On reload, retrieve only newly inserted or updated data
        val db = MyDayDatabase.getInstance(this@HomeActivity)
        mListEntries = async(CommonPool) { db?.myDayDAO()?.getAllCalendarData()!! }.await()
        db!!.destroyInstance()

        // Clear and update day decorators
        calendarView.removeDecorators()

        calendarView.addDecorators(
                MoodDecorator0(sortMoods(0).await(), this@HomeActivity),
                MoodDecorator1(sortMoods(1).await(), this@HomeActivity),
                MoodDecorator2(sortMoods(2).await(), this@HomeActivity),
                MoodDecorator3(sortMoods(3).await(), this@HomeActivity),
                MoodDecorator4(sortMoods(4).await(), this@HomeActivity),
                MoodDecoratorNull(sortMoods(-1).await(), this@HomeActivity),
                DisabledDecorator(this@HomeActivity)
        )

        calendarView.visibility = View.VISIBLE

        // Set listener for day selected
        // TODO: Create date picker
        calendarView.setOnDateChangedListener(MyOnDateSelectedListener())

        // Hide new entry button if there is already an entry for today
        if (mListEntries!!.map { it.date == mFormatYearMonthDay.format(Date()) }.indexOf(true) != -1)
            floatingActionButton_newEntry.visibility = View.INVISIBLE
        else floatingActionButton_newEntry.visibility = View.VISIBLE

        // Reset DB Updated flag
        mDBUFlag = false
    }

    fun onClickListenerHome(view: View) {
        when (view) {
            floatingActionButton_newEntry -> {
                val intent = Intent(this, NewEntryActivity::class.java)
                startActivity(intent)
            }
        }
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


}
