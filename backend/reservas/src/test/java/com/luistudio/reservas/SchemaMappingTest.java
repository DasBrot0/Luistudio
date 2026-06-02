package com.luistudio.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.CampusScheduleEntity;
import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoleEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomScheduleEntity;
import com.luistudio.reservas.model.SystemConfigEntity;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SchemaMappingTest {

    private static final Set<Class<?>> ENTITIES = Set.of(
        AuditLogEntity.class,
        CampusScheduleEntity.class,
        EmailOutboxEntity.class,
        LoginAttemptEntity.class,
        MaintenanceEntity.class,
        NotificationPreferenceEntity.class,
        PabellonEntity.class,
        PasswordResetEntity.class,
        ReservationEntity.class,
        RoleEntity.class,
        RoomEntity.class,
        RoomScheduleEntity.class,
        SystemConfigEntity.class,
        TwoFactorCodeEntity.class,
        UserEntity.class
    );

    @Test
    void sqlBaseDefinesEveryMappedEntityColumn() throws IOException {
        Map<String, Set<String>> schema = parseBaseSchema();

        for (Class<?> entity : ENTITIES) {
            String table = entity.getAnnotation(Table.class).name();
            assertThat(schema)
                .as("table mapped by %s", entity.getSimpleName())
                .containsKey(table);

            Set<String> expectedColumns = mappedColumns(entity);
            assertThat(schema.get(table))
                .as("columns mapped by %s on table %s", entity.getSimpleName(), table)
                .containsAll(expectedColumns);
        }
    }

    private static Set<String> mappedColumns(Class<?> entity) {
        Set<String> columns = new LinkedHashSet<>();
        columns.add("id");

        for (Field field : entity.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                columns.add(resolveColumnName(column.name(), field.getName()));
            }

            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                columns.add(resolveColumnName(joinColumn.name(), field.getName()));
            }
        }

        return columns;
    }

    private static String resolveColumnName(String explicitName, String fieldName) {
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName.toLowerCase(Locale.ROOT);
        }
        return toSnakeCase(fieldName);
    }

    private static String toSnakeCase(String value) {
        return value
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .toLowerCase(Locale.ROOT);
    }

    private static Map<String, Set<String>> parseBaseSchema() throws IOException {
        Path schemaPath = Path.of("..", "..", "database", "001_init.sql");
        String sql = Files.readString(schemaPath);
        Pattern tablePattern = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS\\s+(\\w+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Map<String, Set<String>> schema = new HashMap<>();
        Matcher matcher = tablePattern.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            Set<String> columns = new HashSet<>();
            for (String line : matcher.group(2).split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || isTableConstraint(trimmed)) {
                    continue;
                }
                String firstToken = trimmed.split("\\s+")[0].replace(",", "");
                columns.add(firstToken.toLowerCase(Locale.ROOT));
            }
            schema.put(table, columns);
        }

        return schema;
    }

    private static boolean isTableConstraint(String line) {
        return Arrays.stream(new String[] { "constraint", "primary", "foreign", "unique", "check" })
            .anyMatch(prefix -> line.toLowerCase(Locale.ROOT).startsWith(prefix));
    }
}
