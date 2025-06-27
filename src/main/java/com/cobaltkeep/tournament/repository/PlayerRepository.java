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
    @Query("SELECT p FROM Player p WHERE p NOT IN (SELECT t.players FROM Tournament t WHERE t.id = ?1)")
    List<Player> findAvailablePlayers(Long tournamentId);
    @Query("SELECT p FROM Player p WHERE p NOT IN (SELECT tp FROM Tournament t JOIN t.players tp WHERE t.id = :tournamentId)")
    List<Player> findPlayersNotInTournament(@Param("tournamentId") Long tournamentId);
}