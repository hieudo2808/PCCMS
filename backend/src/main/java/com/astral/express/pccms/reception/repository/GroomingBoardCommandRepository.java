package com.astral.express.pccms.reception.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GroomingBoardCommandRepository {
    private final JdbcTemplate jdbc;

    public Optional<String> findTicketStatus(UUID ticketId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT status_code FROM grooming_tickets WHERE id = ?",
                    String.class,
                    ticketId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateTicketStatus(UUID ticketId, String statusCode, String internalNote) {
        jdbc.queryForObject("""
                UPDATE grooming_tickets
                SET status_code = ?::grooming_status_enum,
                    internal_note = COALESCE(?, internal_note),
                    started_at = CASE WHEN ? = 'IN_SERVICE' AND started_at IS NULL THEN now() ELSE started_at END,
                    completed_at = CASE WHEN ? = 'COMPLETED' THEN now() ELSE completed_at END,
                    updated_at = now()
                WHERE id = ?
                RETURNING id
                """, UUID.class, statusCode, internalNote, statusCode, statusCode, ticketId);
    }
}
