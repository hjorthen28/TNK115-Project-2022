package se.liu.itn.kts.tnk115.project2022;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface NodeDao {
    @Query("SELECT * FROM node ORDER BY id ASC")
    List<Node> getAllNodes();

    @Query("SELECT * FROM node WHERE id = :id")
    public Node getNode(int id);

    @Query("SELECT COUNT(id) FROM node")
    public int getLength();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAllNodes(Node... node);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertNode(Node node);

    @Delete
    public void deleteAllNodes(Node... node);

    @Delete
    public void deleteNode(Node node);
}
