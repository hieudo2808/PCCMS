package com.astral.express.pccms.report.repository;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RevenueReportRepository {

    private static final String SELECT_REVENUE_ROWS = """
            SELECT
                report_date,
                category_code,
                service_id,
                service_name,
                revenue_vnd,
                invoice_count
            FROM v_revenue_by_service_day
            WHERE report_date BETWEEN :fromDate AND :toDate
            """;

    private static final String ORDER_BY_REPORT_COLUMNS = """
            ORDER BY report_date ASC, category_code ASC, service_name ASC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<RevenueSummaryRow> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        MapSqlParameterSource parameters = baseParameters(fromDate, toDate);
        return jdbcTemplate.query(
                SELECT_REVENUE_ROWS + ORDER_BY_REPORT_COLUMNS,
                parameters,
                this::mapRow
        );
    }

    public List<RevenueSummaryRow> findByDateRangeAndCategoryCode(
            LocalDate fromDate,
            LocalDate toDate,
            String categoryCode) {
        MapSqlParameterSource parameters = baseParameters(fromDate, toDate)
                .addValue("categoryCode", categoryCode);
        return jdbcTemplate.query(
                SELECT_REVENUE_ROWS
                        + "AND category_code = CAST(:categoryCode AS service_category_enum) "
                        + ORDER_BY_REPORT_COLUMNS,
                parameters,
                this::mapRow
        );
    }

    public List<RevenueSummaryRow> findByDateRangeAndServiceId(
            LocalDate fromDate,
            LocalDate toDate,
            UUID serviceId) {
        MapSqlParameterSource parameters = baseParameters(fromDate, toDate)
                .addValue("serviceId", serviceId);
        return jdbcTemplate.query(
                SELECT_REVENUE_ROWS
                        + "AND service_id = :serviceId "
                        + ORDER_BY_REPORT_COLUMNS,
                parameters,
                this::mapRow
        );
    }

    private MapSqlParameterSource baseParameters(LocalDate fromDate, LocalDate toDate) {
        return new MapSqlParameterSource()
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);
    }

    private RevenueSummaryRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        String categoryCode = resultSet.getString("category_code");
        UUID serviceId = resultSet.getObject("service_id", UUID.class);
        return new RevenueSummaryRow(
                resultSet.getObject("report_date", LocalDate.class),
                categoryCode == null ? null : ServiceCategory.valueOf(categoryCode),
                serviceId,
                resultSet.getString("service_name"),
                resultSet.getBigDecimal("revenue_vnd"),
                resultSet.getLong("invoice_count")
        );
    }
}

