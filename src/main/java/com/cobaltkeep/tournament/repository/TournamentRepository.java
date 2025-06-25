package com.cobaltkeep.tournament.repository;

import com.cobaltkeep.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("SELECT t FROM Tournament t LEFT JOIN FETCH t.players WHERE t.id = :id")
    Optional<Tournament> findByIdWithPlayers(@Param("id") Long id);
}