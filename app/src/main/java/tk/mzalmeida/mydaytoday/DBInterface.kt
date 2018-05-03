/*
 * Handles all database logic and interactions
 */

package tk.mzalmeida.mydaytoday

import android.arch.persistence.room.*
import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// Data type for database
@Entity(tableName = "my_day_data") @Parcelize
data class MyDayData(
        @PrimaryKey(autoGenerate = true) var entryID: Long,
        @ColumnInfo(name = "date") var date: String,
        @ColumnInfo(name = "mood_score") var moodScore: Int, // Score between 0 and 5
        @ColumnInfo(name = "today_focus") var todayFocus: String, // Text box
        @ColumnInfo(name = "priorities") var todayPriorities: String, // Text formatted as list (!)
        @ColumnInfo(name = "learned_today") var learnedToday: String, // Text box
        @ColumnInfo(name = "avoid_tomorrow") var avoidTomorrow: String, // Text formatted as list
        @ColumnInfo(name = "thankful_for") var thankfulFor: String // Text formatted as list
) : Parcelable {
    @Ignore
    constructor(): this(
            0,"",
            3,"",
            "","",
            "",""
    )
}

data class PartialMyDayData (
        var entryID: Long,
        var date: String,
        @ColumnInfo(name = "mood_score") var moodScore: Int
)

@Dao
interface MyDayDAO {

    @Query("SELECT entryID, date, mood_score FROM my_day_data")
    fun getAllCalendarData() : List<PartialMyDayData>

    @Query("SELECT * FROM my_day_data WHERE entryID = :dayID LIMIT 1")
    fun getSelectedDay(dayID: Long): MyDayData

    @Query("SELECT * FROM my_day_data")
    fun getAllData(): List<MyDayData>

    @Insert
    fun addNewEntry(entry: MyDayData)

    @Update
    fun updateCurrentEntry(entry: MyDayData)

    @Delete
    fun deleteCurrentEntry(entry: MyDayData)
}

@Database(entities = [MyDayData::class], version = 1)
abstract class MyDayDatabase : RoomDatabase() {

    abstract fun myDayDAO(): MyDayDAO

    companion object {
        private var dbInstance: MyDayDatabase? = null

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

    fun destroyInstance() {
        if (dbInstance?.isOpen == true){
            dbInstance?.close()
        }
        dbInstance = null
    }
}