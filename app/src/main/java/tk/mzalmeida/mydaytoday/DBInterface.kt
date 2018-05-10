/*
 * Handles all database logic and interactions
 */

package tk.mzalmeida.mydaytoday

import android.arch.persistence.room.*
import android.arch.persistence.room.ForeignKey.CASCADE
import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Data types block
 */
// Data type for core day data
@Entity(tableName = "my_day_core_data")
data class MyDayCoreData(
        @PrimaryKey(autoGenerate = true)        var entryID: Long,
        @ColumnInfo(name = "date")              var date: String,
        @ColumnInfo(name = "mood_score")        var moodScore: Int,
        @ColumnInfo(name = "focus")             var dayFocus: String,
        @ColumnInfo(name = "priorities")        var dayPriorities: String,
        @ColumnInfo(name = "learned_today")     var learnedToday: String,
        @ColumnInfo(name = "avoid_tomorrow")    var avoidTomorrow: String,
        @ColumnInfo(name = "thankful_for")      var thankfulFor: String,
        @ColumnInfo(name = "conclude_flag")     var concludeFlag: Boolean
){
    // Constructor to simplify object instantiation
    @Ignore
    constructor(): this(
            0,"",
            -1,"",
            "","",
            "","",
            true
    )
}

// Entity declaration for data type Goals
@Entity(tableName = "my_day_goals_data",
        foreignKeys = [ForeignKey(
                entity = MyDayCoreData::class,
                parentColumns = ["entryID"],
                childColumns = ["dayID"],
                onDelete = CASCADE
        )],
        indices = [Index("dayID")]
)
@Parcelize

// Data type for goals data
data class MyDayGoalsData(
        @PrimaryKey(autoGenerate = true)        var goalID: Long,
        @ColumnInfo(name = "dayID")             var dayID: Long,
        @ColumnInfo(name = "goal_body")         var goalBody: String,
        @ColumnInfo(name = "goal_completed")    var goalCompleted: Boolean
): Parcelable {
    @Ignore
    constructor(): this(0,0,"",false)
}

// Minimal version of core data type for calendar only
data class CalendarDayData (
        var entryID: Long,
        var date: String,
        @ColumnInfo(name = "mood_score") var moodScore: Int,
        @ColumnInfo(name = "conclude_flag") var concludeFlag: Boolean
)

// Data type for full entry, with support for parcelable
@Parcelize
data class MyDayEntryData(
        var entryID: Long,
        var date: String,
        var moodScore: Int,
        var todayFocus: String,
        var todayPriorities: String,
        var todayGoals: List<MyDayGoalsData>,
        var learnedToday: String,
        var avoidTomorrow: String,
        var thankfulFor: String,
        var concludeFlag: Boolean
): Parcelable {

    // Constructor to simplify object instantiation
    constructor(): this(
            0,"",
            -1,"",
            "", listOf(MyDayGoalsData()),
            "","",
            "",true
    )
}


/**
 * DAO block
 */
// Main DAO for database
@Dao
abstract class MyDayDAO {

    /**
     * Public functions block
     */
    @Transaction
    open fun getEntry(entryID: Long): MyDayEntryData {
        val result = MyDayEntryData()
        val coreQuery = getCoreData(entryID)
        val goalsQuery = getGoalsData(entryID)

        result.entryID = coreQuery.entryID
        result.date = coreQuery.date
        result.moodScore = coreQuery.moodScore
        result.todayFocus = coreQuery.dayFocus
        result.todayPriorities = coreQuery.dayPriorities
        result.todayGoals = goalsQuery
        result.learnedToday = coreQuery.learnedToday
        result.avoidTomorrow = coreQuery.avoidTomorrow
        result.thankfulFor = coreQuery.thankfulFor
        result.concludeFlag = coreQuery.concludeFlag

        return result
    }

    @Transaction
    open fun addEntry(entry: MyDayEntryData) {
        val entryCoreData = parseCoreData(entry)
        val entryGoalsData = entry.todayGoals

        val newEntryID = insertCoreData(entryCoreData)
        for (line in entryGoalsData)
            line.dayID = newEntryID

        insertGoalsData(entryGoalsData)
    }

    @Transaction
    open fun deleteEntry(entry: MyDayEntryData) {
        deleteGoalsData(entry.entryID)
        deleteCoreData(entry.entryID)
    }

    // Transaction updates core fields, updates existing goal fields, adds new goal fields
    // and removes deleted goal fields by IDs in goalsToDelete
    @Transaction
    open fun updateEntry(entry: MyDayEntryData, goalsToDelete: List<Long>) {
        val goalsToUpdate = mutableListOf<MyDayGoalsData>()
        val goalsToInsert = mutableListOf<MyDayGoalsData>()

        for (line in entry.todayGoals) {
            when (line.goalID) {
                0.toLong() -> goalsToInsert.add(line)
                else -> goalsToUpdate.add(line)
            }
        }

        updateCoreData(parseCoreData(entry))

        updateGoalsData(goalsToUpdate)
        insertGoalsData(goalsToInsert)
        for (line in goalsToDelete)
            deleteGoalByID(line)
    }

    // Auxiliary function to parse full entry into core data only
    private fun parseCoreData(entry: MyDayEntryData): MyDayCoreData {
        val result = MyDayCoreData()

        result.entryID = entry.entryID
        result.date = entry.date
        result.moodScore = entry.moodScore
        result.dayFocus = entry.todayFocus
        result.dayPriorities = entry.todayPriorities
        result.learnedToday = entry.learnedToday
        result.avoidTomorrow = entry.avoidTomorrow
        result.thankfulFor = entry.thankfulFor
        result.concludeFlag = entry.concludeFlag

        return result
    }

    // Calendar query // TODO: Optimize query to return only new or updated values
    @Query("SELECT entryID, date, mood_score, conclude_flag FROM my_day_core_data")
    abstract fun getCalendarData(): List<CalendarDayData>


    /**
     * Core data queries block
     */
    @Query("SELECT * FROM my_day_core_data WHERE entryID = :entryID LIMIT 1")
    protected abstract fun getCoreData(entryID: Long): MyDayCoreData

    @Insert
    protected abstract fun insertCoreData(data: MyDayCoreData): Long

    @Update
    protected abstract fun updateCoreData(data: MyDayCoreData)

    // Replacement for @Delete
    @Query("DELETE FROM my_day_core_data WHERE entryID = :entryID")
    protected abstract fun deleteCoreData(entryID: Long)


    /**
     * Goals data queries block
     */
    @Query("SELECT * FROM my_day_goals_data WHERE dayID = :dayID")
    protected abstract fun getGoalsData(dayID: Long): List<MyDayGoalsData>

    @Insert
    protected abstract fun insertGoalsData(data: List<MyDayGoalsData>)

    @Update
    protected abstract fun updateGoalsData(data: List<MyDayGoalsData>)

    // Replacement for @Delete
    @Query("DELETE FROM my_day_goals_data WHERE dayID = :dayID")
    protected abstract fun deleteGoalsData(dayID: Long)

    @Query("DELETE FROM my_day_goals_data WHERE goalID = :goalID")
    protected abstract fun deleteGoalByID(goalID: Long)
}


/**
 * Database block
 */
// Database class
@Database(entities = [MyDayCoreData::class, MyDayGoalsData::class], version = 1)
abstract class MyDayDatabase : RoomDatabase() {

    // DAO initializer
    abstract fun myDayDAO(): MyDayDAO

    companion object {
        private var dbInstance: MyDayDatabase? = null

        // Public instance getter for database
        fun getInstance(context: Context): MyDayDatabase? {
            if (dbInstance == null){
                synchronized(MyDayDatabase::class) {
                    dbInstance = Room.databaseBuilder(
                            context.applicationContext,
                            MyDayDatabase::class.java,
                            "my-day.db"
                    ).allowMainThreadQueries(
                    ).build()
                }
            }
            return dbInstance
        }
    }

    // Public instance destroyer for database
    fun destroyInstance() {
        if (dbInstance?.isOpen == true){
            dbInstance?.close()
        }
        dbInstance = null
    }
}