package sn.garmy.taysir.sql

enum RequestType {
    SELECT("SELECT", "SELECT Statement"),
    UPDATE("UPDATE", "UPDATE Statement"),
    DELETE("DELETE", "DELETE Statement"),
    INSERT("INSERT", "INSERT Statement"),
    CREATE("CREATE", "CREATE SQL OBJECT "),
    ALTER("ALTER", "ALTER SQL OBJECT "),
    DROP("DROP", "DROP SQL OBJECT "),
    EXECUTESP("EXECUTESP", "EXECUTE Stored procedure that did not return a resulset "),
    EXECUTESPROW("EXECUTESPROW", "EXECUTE Stored procedure that return a resulset "),
    SCRIPT("SCRIPT", "EXECUTE an sql script ")

    String code
    String name
    RequestType(String code, String name) {
        this.code = code
        this.name = name
    }

    static RequestType getDefault() {
        return SELECT
    }

    static RequestType getByCode(String code) {
        values().find {it.code == code}
    }

    @Override
    String toString() {
        return code
    }
}