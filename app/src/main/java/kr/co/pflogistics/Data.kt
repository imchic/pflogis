package kr.co.pflogistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pflogis")
data class Data(
    @PrimaryKey(autoGenerate = true) val seq: Int,
    val addr: String?,
    val lonlat: String?,
    val memo: String?
)