package se.liu.itn.kts.tnk115.project2022;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Node {
    @PrimaryKey
    public int id;

    @ColumnInfo(name = "latitude")
    public double lat;
    @ColumnInfo(name = "longitude")
    public double lng;

    public String toString() {
        return "Node:"+id+" Coordinates: ("+lat+" "+lng+")";
    }
}
