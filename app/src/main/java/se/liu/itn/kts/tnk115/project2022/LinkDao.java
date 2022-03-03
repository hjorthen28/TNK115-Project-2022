package se.liu.itn.kts.tnk115.project2022;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface LinkDao {
    @Query("SELECT * FROM link ORDER BY source ASC, destination ASC")
    List<Link> getAllLinks();

    @Query("SELECT * FROM link WHERE source = :s AND destination = :d")
    public Link getNode(int s, int d);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAllLinks(Link... link);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertLink(Link link);

    @Delete
    public void deleteAllLinks(Link... link);

    @Delete
    public void deleteLink(Link link);
}
