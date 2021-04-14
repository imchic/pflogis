package kr.co.pflogistics

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface DataDao {
    @Query("SELECT seq, addr, lonlat, memo FROM pflogis GROUP BY addr ORDER BY seq DESC")
    fun getAll(): LiveData<List<Data>>

    @Insert(onConflict = REPLACE)
    suspend fun insert(vararg data: Data)

    @Delete
    fun delete(vararg data: Data)

    @Query("DELETE FROM pflogis")
    fun deleteAll()

}