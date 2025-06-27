package com.cobaltkeep.tournament.repository;

import com.cobaltkeep.tournament.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByFirstNameAndLastName(String firstName, String lastName);
    
    @Query("SELECT p FROM Player p WHERE p NOT IN (SELECT DISTINCT p2 FROM Tournament t JOIN t.players p2 WHERE t.id = :tournamentId)")
    List<Player> findAvailablePlayers(@Param("tournamentId") Long tournamentId);
}