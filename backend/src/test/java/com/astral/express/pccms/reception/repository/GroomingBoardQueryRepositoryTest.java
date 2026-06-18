package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroomingBoardQueryRepositoryTest {
    @Mock
    private JdbcTemplate jdbc;

    @Test
    void listTickets_shouldBuildSqlWithoutUntypedNullPredicates() {
        GroomingBoardQueryRepository repository = new GroomingBoardQueryRepository(jdbc);
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willReturn(List.of());

        repository.listTickets(null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), argsCaptor.capture());
        assertThat(sqlCaptor.getValue()).doesNotContain("? IS NULL");
        assertThat(argsCaptor.getValue()).containsExactly("", "%%", "%%", "%%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listTickets_shouldMapRowsToResponses() throws Exception {
        GroomingBoardQueryRepository repository = new GroomingBoardQueryRepository(jdbc);
        UUID id = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getObject("id", UUID.class)).willReturn(id);
        given(resultSet.getObject("appointment_id", UUID.class)).willReturn(appointmentId);
        given(resultSet.getString("status_code")).willReturn("PENDING");
        given(resultSet.getString("order_code")).willReturn("SO-1");
        given(resultSet.getString("pet_name")).willReturn("Pet");
        given(resultSet.getString("owner_name")).willReturn("Owner");
        given(resultSet.getString("service_name")).willReturn("Spa");
        given(resultSet.getString("service_code")).willReturn("SPA");
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willAnswer(invocation -> {
            RowMapper<GroomingTicketResponse> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        List<GroomingTicketResponse> result = repository.listTickets("Owner", "PENDING");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).appointmentId()).isEqualTo(appointmentId);
    }
}
