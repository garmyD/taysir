package sn.garmy.tayisr.sql

enum DatabaseType {
    POSTGRESQL("POSTGRESQL", "PostgreSQL Server Database"),
    UNKNOWN("UNKNOWN", "UNKNOWN Database")

    String code
    String name

    DatabaseType(String code, String name) {
        this.code = code
        this.name = name
    }

    static DatabaseType getDefault() {
        return POSTGRESQL
    }


    @Override
    String toString() {
        return code
    }

}