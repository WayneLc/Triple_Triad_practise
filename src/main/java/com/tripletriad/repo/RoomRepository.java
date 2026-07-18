package com.tripletriad.repo;
import com.tripletriad.model.Room; import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomRepository extends JpaRepository<Room,Long> {}
