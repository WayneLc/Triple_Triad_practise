package com.tripletriad.repo;
import com.tripletriad.model.Deck; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface DeckRepository extends JpaRepository<Deck,Long> { List<Deck> findByPlayerId(Long playerId); }
