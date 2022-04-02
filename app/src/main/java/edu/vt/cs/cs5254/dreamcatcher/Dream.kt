package edu.vt.cs.cs5254.dreamcatcher

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

//data class Dream(
//    val id: UUID = UUID.randomUUID(),
//    var title: String = "",
//    var date: Date = Date(),
//    var isFulfilled: Boolean = false,
//    var isDeferred: Boolean = false
//
//)
@Entity(tableName = "dream")
data class Dream(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isFulfilled: Boolean = false,
    var isDeferred: Boolean = false
)
