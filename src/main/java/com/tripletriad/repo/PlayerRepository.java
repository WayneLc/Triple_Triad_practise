package com.tripletriad.repo;
import com.tripletriad.model.Player; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface PlayerRepository extends JpaRepository<Player,Long> { Optional<Player> findByUsername(String username); }
