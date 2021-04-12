package kr.co.pflogistics

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface DataDao {
    @Query("SELECT * FROM pflogis")
    fun getAll(): LiveData<List<Data>>

    @Query("SELECT * FROM pflogis WHERE seq IN (:seqArrs)")
    fun getSeq(seqArrs: IntArray): List<Data>

    @Query("SELECT * FROM pflogis WHERE addr LIKE :addr LIMIT 1")
    fun getAddr(addr: String): Data

    @Insert(onConflict = REPLACE)
    fun insert(data: Data)

    @Delete
    fun delete(data: Data)

}