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
    public Link getLink(int s, int d);

    @Query("SELECT distance FROM link ORDER BY distance DESC LIMIT 1")
    public int getMaxDist();

    @Query("SELECT distance FROM link ORDER BY distance ASC LIMIT 1")
    public int getMinDist();

    @Query("SELECT air FROM link ORDER BY air DESC LIMIT 1")
    public int getMaxAir();

    @Query("SELECT air FROM link ORDER BY air ASC LIMIT 1")
    public int getMinAir();

    @Query("SELECT elevationcategorized FROM link ORDER BY elevationcategorized DESC LIMIT 1")
    public int getMaxElev();

    @Query("SELECT elevationcategorized FROM link ORDER BY elevationcategorized ASC LIMIT 1")
    public int getMinElev();

    @Query("SELECT pavementquality FROM link ORDER BY pavementquality DESC LIMIT 1")
    public int getMaxPave();

    @Query("SELECT pavementquality FROM link ORDER BY pavementquality ASC LIMIT 1")
    public int getMinPave();

    @Query("SELECT pedpavequality FROM link ORDER BY pedpavequality DESC LIMIT 1")
    public int getMaxPed();

    @Query("SELECT pedpavequality FROM link ORDER BY pedpavequality ASC LIMIT 1")
    public int getMinPed();

    @Query("SELECT wcpavequality FROM link ORDER BY wcpavequality DESC LIMIT 1")
    public int getMaxWC();

    @Query("SELECT wcpavequality FROM link ORDER BY wcpavequality ASC LIMIT 1")
    public int getMinWC();

    @Query("SELECT temperature FROM link ORDER BY temperature DESC LIMIT 1")
    public double getMaxTemp();

    @Query("SELECT temperature FROM link ORDER BY temperature ASC LIMIT 1")
    public double getMinTemp();

    @Query("SELECT noise FROM link ORDER BY noise DESC LIMIT 1")
    public double getMaxNoise();

    @Query("SELECT noise FROM link ORDER BY noise ASC LIMIT 1")
    public double getMinNoise();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAllLinks(Link... link);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertLink(Link link);

    @Delete
    public void deleteAllLinks(Link... link);

    @Delete
    public void deleteLink(Link link);
}
