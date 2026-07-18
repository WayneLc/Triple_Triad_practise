package com.tripletriad.repo;
import com.tripletriad.model.Battle; import org.springframework.data.jpa.repository.JpaRepository;
public interface BattleRepository extends JpaRepository<Battle,Long> {}
