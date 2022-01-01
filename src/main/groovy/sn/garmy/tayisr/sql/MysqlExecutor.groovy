package sn.garmy.tayisr.sql


import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.Connection

class MysqlExecutor extends SqlExecutor{


    MysqlExecutor(Sql sql) {
        super(sql)
    }

    MysqlExecutor(DataSource dataSource) {
        super(dataSource)
    }

    MysqlExecutor(Connection connection) {
        super(connection)
    }

    @Override
    Collection executeQuery(String query) {
        return sql.query(query, {})
    }

    @Override
    List<GroovyRowResult> callStoredProcedure(String name, List<Object> params, Boolean rows) {
        List<GroovyRowResult> res = []
        if (rows) {
            res = sql.rows("{call ${name} (${generateQuestionMarks(params?.size())})}", params)
        }
        else {
            callStoredProcedure(name, params) {}
        }
        return res
    }

    @Override
    void callStoredProcedure(String name, List<Object> params, Closure closure) {
        sql.call("{call ${name} (${generateQuestionMarks(params?.size())})}", params, closure)
    }
}