package com.twinmind.voicerecorder.data.db

import androidx.room.TypeConverter

class EnumsTc {
    @TypeConverter fun csToString(v: ChunkStatus) = v.name
    @TypeConverter fun stringToCs(s: String) = ChunkStatus.valueOf(s)
    @TypeConverter fun ssToString(v: SummaryStatus) = v.name
    @TypeConverter fun stringToSs(s: String) = SummaryStatus.valueOf(s)
}
