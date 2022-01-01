package sn.garmy.tayisr.sql

import groovy.sql.GroovyRowResult

import groovy.sql.GroovyRowResult
import groovy.sql.OutParameter
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.Connection


abstract class SqlExecutor {
    protected Sql sql


    Sql getSql() {
        return this.sql
    }

    SqlExecutor(Sql sql) {
        this.sql = sql
    }

    SqlExecutor(DataSource dataSource) {
        this(new Sql(dataSource))
    }

    SqlExecutor(Connection connection) {
        this(new Sql(connection))
    }

    abstract Collection executeQuery(String query)
    abstract List<GroovyRowResult> callStoredProcedure(String name, List<Object> params, Boolean rows = true)
    abstract void callStoredProcedure(String name, List<Object> params, Closure closure)

    static String generateQuestionMarks(int nTimes) {
        String res = ""
        if (nTimes > 0) {
            res = "?" + (", ?" * (nTimes - 1))
        }
        return res
    }

    static String generateCallStatement(List<Object> params) {
        if (params == null || params.size() == 0) {
            return  ""
        }
        String res = "" + quoteMePlease(params[0])
        Integer nTimes = params.size()
        for (int i = 1; i < nTimes; i++) {
            res+= "," + quoteMePlease(params[i])
        }
        return res
    }
    static String quoteMePlease(Object ob) {

        if (ob instanceof String) {
            return "'${ob}'"
        }
        else if (ob instanceof byte[] ) {
            return "'[BYTE]'"
        }
        else {
            if (ob instanceof OutParameter) {
                return "Out"
            }
            else {
                return "" + ob
            }
        }
    }

}


