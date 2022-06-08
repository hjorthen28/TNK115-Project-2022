package se.liu.itn.kts.tnk115.project2022;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Link.class}, version = 1)
public abstract class LinkDatabase extends RoomDatabase {
    public abstract LinkDao userDao();
}
