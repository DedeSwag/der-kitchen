package com.der.kitchen.database;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseBaselineSourceTest {

    private static final List<String> FINAL_TABLES = List.of(
            "sys_user",
            "sys_file",
            "dish_category",
            "dish_info",
            "biz_order",
            "biz_order_item",
            "biz_order_event",
            "sys_notification",
            "biz_favorite"
    );

    @Test
    void containsOnlyOneMigrationWithFinalTableNames() throws Exception {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");
        try (var files = Files.list(migrationDirectory)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith(".sql")).toList())
                    .extracting(path -> path.getFileName().toString())
                    .containsExactly("V1__init.sql");
        }

        String sql = new ClassPathResource("db/migration/V1__init.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        FINAL_TABLES.forEach(table -> assertThat(sql).contains("CREATE TABLE " + table));
        assertThat(sql)
                .doesNotContain("CREATE TABLE users")
                .doesNotContain("CREATE TABLE categories")
                .doesNotContain("CREATE TABLE dishes")
                .doesNotContain("CREATE TABLE orders")
                .doesNotContain("CREATE TABLE order_items")
                .doesNotContain("CREATE TABLE favorites")
                .doesNotContain("CREATE TRIGGER")
                .doesNotContain("set_update_time")
                .doesNotContain("ALTER TABLE");
    }
}
