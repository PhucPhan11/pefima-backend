package com.ceu.pefima.backend.source.sqlite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Location of the {@code notion_cleanup} SQLite database.
 *
 * @param path file path, absolute or relative to the working directory
 */
@ConfigurationProperties("pefima.source.sqlite")
public record SqliteSourceProperties(String path) {
}
