package tk.mzalmeida.mydaytoday

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import kotlin.collections.HashSet


class MoodDecorator0(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.mood0_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_0)!!)
    }
}

class MoodDecorator1(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.mood1_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_1)!!)
    }
}

class MoodDecorator2(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.mood2_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_2)!!)
    }
}

class MoodDecorator3(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.mood3_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_3)!!)
    }
}

class MoodDecorator4(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.mood4_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_4)!!)
    }

}

class MoodDecoratorNull(decorate: Collection<CalendarDay>, val context: Context) : DayViewDecorator {

    private val decorate: HashSet<CalendarDay> = HashSet(decorate)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return decorate.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(StyleSpan(Typeface.BOLD))
        view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.moodNull_Text)))
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_mood_null)!!)
    }

}
