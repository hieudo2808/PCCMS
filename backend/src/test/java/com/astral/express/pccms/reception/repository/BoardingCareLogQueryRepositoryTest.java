package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardingCareLogQueryRepositoryTest {
    @Mock
    private JdbcTemplate jdbc;

    @Test
    void listBookings_shouldBuildSqlWithoutUntypedNullPredicates() {
        BoardingCareLogQueryRepository repository = new BoardingCareLogQueryRepository(jdbc);
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willReturn(List.of());

        repository.listBookings(null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), argsCaptor.capture());
        assertThat(sqlCaptor.getValue()).doesNotContain("? IS NULL");
        assertThat(argsCaptor.getValue()).containsExactly("", "%%", "%%", "%%");
    }

    @Test
    void listCareLogs_shouldBuildSqlWithoutUntypedNullPredicates() {
        BoardingCareLogQueryRepository repository = new BoardingCareLogQueryRepository(jdbc);
        UUID userId = UUID.randomUUID();
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willReturn(List.of());

        repository.listCareLogs(userId, null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), argsCaptor.capture());
        assertThat(sqlCaptor.getValue()).doesNotContain("? IS NULL");
        assertThat(argsCaptor.getValue()).containsExactly(userId, userId, userId);
    }

    @Test
    void listBookings_shouldMapRowsToResponses() throws Exception {
        BoardingCareLogQueryRepository repository = new BoardingCareLogQueryRepository(jdbc);
        UUID id = UUID.randomUUID();
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willAnswer(invocation -> {
            RowMapper<BoardingBookingResponse> mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", UUID.class)).thenReturn(id);
            when(rs.getString(anyString())).thenAnswer(answer -> switch ((String) answer.getArgument(0)) {
                case "booking_code" -> "B-1";
                case "status_code" -> "RESERVED";
                case "pet_name" -> "Pet";
                default -> null;
            });
            return List.of(mapper.mapRow(rs, 0));
        });

        List<BoardingBookingResponse> result = repository.listBookings("Pet", "RESERVED");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).bookingCode()).isEqualTo("B-1");
    }

    @Test
    void listCareLogs_shouldMapRowsToResponses() throws Exception {
        BoardingCareLogQueryRepository repository = new BoardingCareLogQueryRepository(jdbc);
        UUID id = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willAnswer(invocation -> {
            RowMapper<CareLogResponse> mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", UUID.class)).thenReturn(id);
            when(rs.getObject("session_id", UUID.class)).thenReturn(sessionId);
            when(rs.getObject("pet_id", UUID.class)).thenReturn(petId);
            when(rs.getString(anyString())).thenAnswer(answer -> switch ((String) answer.getArgument(0)) {
                case "pet_name" -> "Pet";
                default -> null;
            });
            when(rs.getBoolean("can_edit")).thenReturn(true);
            when(rs.getBoolean("can_delete")).thenReturn(true);
            return List.of(mapper.mapRow(rs, 0));
        });

        List<CareLogResponse> result = repository.listCareLogs(UUID.randomUUID(), sessionId, petId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).petName()).isEqualTo("Pet");
        assertThat(result.get(0).canEdit()).isTrue();
    }
}
