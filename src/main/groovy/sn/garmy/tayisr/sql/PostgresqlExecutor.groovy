package sn.garmy.tayisr.sql


import groovy.sql.GroovyResultSet
import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Types

class PostgresqlExecutor extends SqlExecutor{


    PostgresqlExecutor(Sql sql) {
        super(sql)
    }

    PostgresqlExecutor(DataSource dataSource) {
        super(dataSource)
    }

    PostgresqlExecutor(Connection connection) {
        super(connection)
    }

    @Override
    Collection executeQuery(String query) {
        return null
    }

    @Override
    List<GroovyRowResult> callStoredProcedure(String name, List<Object> params, Boolean rows) {
        List<GroovyRowResult> res = []
        def parameters = []
        parameters.addAll(params)
        if (rows) {
            parameters.add(Sql.resultSet(Types.OTHER))
            sql.call("BEGIN ${name}(${generateQuestionMarks(params?.size() + 1)}); END;",parameters) { cursorResults ->
                GroovyResultSet
                cursorResults.eachRow { callableResultSet->
                    res.add((GroovyRowResult)callableResultSet)
                }

            }
        }
        else {
            callStoredProcedure(name, params) {

            }
        }
        return res
    }

    @Override
    void callStoredProcedure(String name, List<Object> params, Closure closure) {
        def parameters = []
        parameters.addAll(params)
        parameters.add(Sql.resultSet(Types))
        sql.call("BEGIN ${name}(${generateQuestionMarks(params?.size() + 1)}); END;", parameters, closure)
    }
}