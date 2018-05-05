package tk.mzalmeida.mydaytoday

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.util.*

class DisabledDecorator(private val context: Context) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.isAfter(CalendarDay.from(Calendar.getInstance(Locale.getDefault())))
    }

    override fun decorate(view: DayViewFacade) {
        view.setDaysDisabled(true)
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.disabled_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_disabled_day)!!)
    }
}