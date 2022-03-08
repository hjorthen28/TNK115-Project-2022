package se.liu.itn.kts.tnk115.project2022;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(primaryKeys = {"source", "destination"})
public class Link {
    public int source;
    public int destination;

    @ColumnInfo(name = "distance")
    public int dist;
    public int air;
    @ColumnInfo(name = "elevationcategorized")
    public int elev;
    @ColumnInfo(name = "pavementquality")
    public int pave;
    @ColumnInfo(name = "pedpavequality")
    public int pedp;
    @ColumnInfo(name = "wcpavequality")
    public int wcpave;
    @ColumnInfo(name = "ttcog")
    public double ttcog;
    @ColumnInfo(name = "ttcyclepqcoeff")
    public double ttcycle;
    @ColumnInfo(name = "ttelevcoeff")
    public double ttelev;
    @ColumnInfo(name = "ttwcpqcoeff")
    public double ttwc;

    public String toString() {
        return "Source:"+source+" Destination:"+destination+" Distance"+dist;
    }
}
