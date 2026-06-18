package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
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
class AppointmentReceptionQueryRepositoryTest {
    @Mock
    private JdbcTemplate jdbc;

    @Test
    void listAppointments_shouldBuildSqlWithoutUntypedNullPredicates() {
        AppointmentReceptionQueryRepository repository = new AppointmentReceptionQueryRepository(jdbc);
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willReturn(List.of());

        repository.listAppointments(null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), argsCaptor.capture());
        assertThat(sqlCaptor.getValue()).doesNotContain("? IS NULL");
        assertThat(argsCaptor.getValue()).containsExactly("", "%%", "%%", "%%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listAppointments_shouldMapRowsToResponses() throws Exception {
        AppointmentReceptionQueryRepository repository = new AppointmentReceptionQueryRepository(jdbc);
        UUID id = UUID.randomUUID();
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getObject("id", UUID.class)).willReturn(id);
        given(resultSet.getString("status_code")).willReturn("PENDING");
        given(resultSet.getString("order_code")).willReturn("SO-1");
        given(resultSet.getString("owner_name")).willReturn("Owner");
        given(resultSet.getString("pet_name")).willReturn("Pet");
        given(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).willAnswer(invocation -> {
            RowMapper<AppointmentReceptionResponse> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        List<AppointmentReceptionResponse> result = repository.listAppointments("Owner", "PENDING");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).ownerName()).isEqualTo("Owner");
    }
}
