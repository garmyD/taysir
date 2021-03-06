package sn.garmy.taysir.sql

import grails.gorm.transactions.Transactional

import groovy.json.JsonOutput
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import sn.garmy.tayisr.TaysirException
import sn.garmy.tayisr.sql.ConnexionParamDTO
import sn.garmy.tayisr.sql.DatabaseType
import sn.garmy.tayisr.sql.MysqlExecutor
import sn.garmy.tayisr.sql.PostgresqlExecutor
import sn.garmy.tayisr.sql.SqlExecutor

import javax.sql.DataSource
import java.sql.SQLException

@Transactional
class SqlCatalogueService {


    private static final Object lock = new Object()
    private static Map<String, String> sqlCache = [:]
    private static DatabaseType databaseType
    static SqlExecutor sqlExecutor
    static DataSource dataSourceIn
    private static Sql sqlIn

    static int BATCH_SIZE = 500

    // Set the database type and the sqlExecutor from the Datasource
    void setDatabaseType() {
        def sql = new Sql(dataSourceIn)
        def driverConnexion = sql.dataSource.getConnection().getMetaData().getDriverName().toString()
        log.debug(JsonOutput.toJson([driverConnexion: driverConnexion]))


        switch (driverConnexion) {
            case ["MySQL Connector Java", "MySQL-AB JDBC Driver"]:
                databaseType = DatabaseType.MYSQL
                sqlExecutor = new MysqlExecutor(dataSourceIn)
                break
            case "PostgreSQL JDBC Driver":
                databaseType = DatabaseType.POSTGRESQL
                sqlExecutor = new PostgresqlExecutor(dataSourceIn)
                break
        }
        log.debug("driverConnexion: ${driverConnexion} ---> databaseType : ${databaseType}")
    }

    void initServiceFromDataSource(DataSource dataSourceP) {
        if (dataSourceP != null && dataSourceP != dataSourceIn) {
            dataSourceIn = dataSourceP
            setDatabaseType()
        }
    }

    void initServiceFromJdbcParam(ConnexionParamDTO connexionParam) {
        if (connexionParam != null) {
            sqlIn = getSqlInstance(connexionParam)
            dataSourceIn = null   // Seul un des deux est ?? utiliser

            // Pour pouvoir executer les proc??dures stock??es bas??ees sur le type de base
            setDatabaseType(connexionParam, sqlIn)
        }
    }

    void loadSqlCacheFromDirectory(File directory) {
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDirectory", message: "Loading SQL cache from disk using base directory ${directory.name}"]))
        synchronized (lock) {
            if (sqlCache.size() == 0) {
                try {
                    directory.eachFileRecurse { sqlFile ->
                        if (sqlFile.isFile() && sqlFile.name.toUpperCase().startsWith(databaseType.code + "_") && sqlFile.name.toUpperCase().endsWith(".SQL")) {
                            def sqlKey = sqlFile.name.toUpperCase()[0..-5] //Enlever le .sql ?? la fin dans la cl??
                            sqlCache[sqlKey] = sqlFile.text
                            loadSqlCache(sqlKey, sqlFile.text)
                        }
                    }
                } catch (Exception ex) {
                    log.error(ex)
                }
            } else {
                log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDirectory", message: "request to load sql cache and cache not empty: size [${sqlCache.size()}]"]))
            }
        }
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDirectory", message: "End loading SQL cache from disk using base directory ${directory.name}"]))
    }

    void loadSqlCache() {
        try {
            setDatabaseType()
            loadSqlCacheFromDirectory(new File(this.class.getResource("/sql/").getFile()))
            loadSqlCacheFromDatabase()
        } catch (Exception ex) {
            log.error(JsonOutput.toJson([method: "loadSqlCache", status: "error", message: ex.message]), ex)
        }
    }

    boolean loadSqlCache(String sqlKey, String sqlText) {
        synchronized (lock) {
            if (sqlKey?.trim() && sqlText?.trim()) {
                sqlCache[sqlKey] = sqlText
                log.debug "added SQL [${sqlKey}] to cache"
                return true

            } else {
                log.error(JsonOutput.toJson([method: "loadSqlCache", sqlKey: sqlKey, sqlText: sqlText, message: "loadSqlCache: key or text invalid!!"]))
                return false
            }
        }
    }

    void loadSqlCacheFromDatabase() {
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDatabase", message: "Loading SQL cache from database table sql_request"]))
        try {
            SqlRequest.findAllWhere(deleted: false).each { sqlRequest ->
                def sqlKey = sqlRequest.databaseType + "_" + sqlRequest.code.toUpperCase()
                loadSqlCache(sqlKey, sqlRequest.requestText)
            }
        } catch (Exception ex) {
            log.error(JsonOutput.toJson([method: "loadSqlCacheFromDatabase", status: "error", message: ex.message]), ex)
        }
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDatabase", message: "End loading SQL cache from database table  sql_request"]))
    }

    void loadSqlCacheFromDatabase(String sqlRequestCode) throws TaysirException {
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDatabase", sqlRequestCode: sqlRequestCode, message: "Loading SQL cache from database table sql_request"]))
        synchronized (lock) {
            def sqlRequest = SqlRequest.findByCodeAndDeleted(sqlRequestCode, false)
            if (!sqlRequest) {
                throw new TaysirException("La requ??te ${sqlRequestCode} n'existe pas dans la base de donn??es")
            }
            def sqlKey = sqlRequest.databaseType + "_" + sqlRequest.code.toUpperCase()
            loadSqlCache(sqlKey, sqlRequest.requestText)
        }
        log.debug(JsonOutput.toJson([method: "loadSqlCacheFromDatabase", message: "End loading SQL cache from database table  sql_request"]))
    }

    // Set the database type and the sqlExecutor from the connexion param
    void setDatabaseType(ConnexionParamDTO connexionParam, Sql sql) {
        if (!connexionParam || !sql) {
            databaseType = DatabaseType.UNKNOWN
            sqlExecutor = null
        } else {
            databaseType = connexionParam.databaseType
            switch (databaseType) {
                case DatabaseType.POSTGRESQL:
                    sqlExecutor = new PostgresqlExecutor(sql)
                    break
            }
        }
    }


    String getSql(String sqlId) {
        def sqlKey = (databaseType.code + "_" + sqlId)?.toUpperCase()
        log.debug "SQL Id requested: ${sqlKey}"

        if (!sqlCache[sqlKey]) {
            log.error(JsonOutput.toJson([method: "getSql", status: "error", message: "SQL [${sqlKey}] not found in cache, loading cache from disk"]))
            loadSqlCache()
        }

        return sqlCache[sqlKey]
    }

    /**
     *
     * @param sqlId
     * @return le commentaire rattach?? ?? la requete
     */
    String getSqlComment(String requeteAndComment) {
        def comment = ""

        // Extraire le commentaire li??  ?? la requ??te s'il y'en a (s??parateur @@)
        def commentAndRequest = requeteAndComment.split("@@")
        log.debug("${commentAndRequest[0]}")
        log.debug("${commentAndRequest[1]}")

        if (2 == commentAndRequest.size() && commentAndRequest[1].trim()) {
            comment = commentAndRequest[0].trim()
        }

        return comment
    }


    def executeScript(String codeRequeteSql, Map sqlParams) {

        Sql sql
        String requeteToExecute
        def commentAndRequest
        int resultat
        String logResultat = ""
        String jsonResult
        def data

        log.debug(JsonOutput.toJson([method: "executeScript", codeRequeteSql: codeRequeteSql, sqlParams: sqlParams]))

        try {
            if (!codeRequeteSql?.trim()) {
                throw new IllegalArgumentException("Le code de la requ??te ?? ex??cuter est invalide")
            }
            String script = getSql(codeRequeteSql)
            String[] requetesScript = script.split(";")

            // Utiliser la connexion SQL en param??tre en priorit?? si elle a ??t?? initialis??e
            if (sqlIn) {
                sql = sqlIn
            } else if (dataSourceIn) {
                sql = new Sql(dataSourceIn)
            } else {
                throw new IllegalStateException("Service sql not correctly initialized or unknown")
            }

            requetesScript.each { requete ->
                if (requete?.trim()) {
                    // Pour isoler la requete du commentaire, besoin de log
                    requeteToExecute = requete?.trim()
                    commentAndRequest = requete.split("@@")
                    if (2 == commentAndRequest.size()) {
                        log.debug(JsonOutput.toJson([status: 'info', method: 'executeScript', message: "Request comment >> ${commentAndRequest[0]?.trim()}"]))
                        requeteToExecute = commentAndRequest[1]?.trim()
                    }

                    log.debug(JsonOutput.toJson([status: 'info', method: 'executeScript', message: "Request >>> $requeteToExecute"]))

                    if (!requeteToExecute) {
                        throw new IllegalStateException("La requ??te SQL n'est pas bien format??e")
                    }
                    if (requeteToExecute.contains(':')) {
                        resultat = sql.executeUpdate(requeteToExecute, sqlParams)
                    } else {
                        resultat = sql.executeUpdate(requeteToExecute)
                    }

                    log.debug("Rows updated : ${resultat}")
                    logResultat += resultat + "\n"
                }
            }

            data = [
                    success: true,
                    data   : logResultat
            ]

            log.debug(JsonOutput.toJson([method: "executeScript", status: "success", codeRequeteSql: codeRequeteSql]))

            jsonResult = JsonOutput.toJson(data)
            log.debug jsonResult

            return jsonResult

        }
        catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
            log.error(JsonOutput.toJson([method: "executeScript", status: "error", message: ex.message]), ex)
            data = [
                    success     : false,
                    errorMessage: ex.message
            ]
            jsonResult = JsonOutput.toJson(data)
            return jsonResult
        }
    }

    String callStoredProcedure(String procName, List<Object> params, Boolean rows) throws IllegalStateException {
        log.debug(JsonOutput.toJson([method: "callStoredProcedure", procedureName: procName, databaseType: databaseType, rows: rows]))
        def data
        def jsonResult

        try {
            if (!sqlExecutor || !sqlExecutor.getSql()) {
                throw new IllegalStateException("Service datasource not correctly initialized or unknown. Make call to 'initServiceFromDataSource' or 'initServiceFromJdbc'  method before executing.")
            }
            Collection resultat = sqlExecutor.callStoredProcedure(procName, params ?: [], rows)

            data = [
                    success: true,
                    data   : resultat
            ]

            jsonResult = JsonOutput.toJson(data)
            log.debug jsonResult

            return jsonResult
        }
        catch (IllegalStateException | SQLException ex) {
            log.error(JsonOutput.toJson([method: "callStoredProcedure", status: "error", message: ex.message]), ex)
            data = [
                    success     : false,
                    errorMessage: ex.message
            ]
            jsonResult = JsonOutput.toJson(data)
            return jsonResult
        }
    }

    /**
     *
     * Pour ??x??cuter une requ??te unitaire (SELECT, UPDATE, DELETE, INSERTE, CREATE, DROP etc.)
     * Renvoie le r??sultat sous forme d'objet JSON contenant le r??sutat ainsi que la liste des objets retourn??s
     *
     */
    def executeRequete(String typeRequete, String requeteSql, Map sqlParams) {
        log.debug("requestType=${typeRequete}, requeteSql = ${requeteSql} , Param??tres sql : ${sqlParams}")

        RequestType requestType = RequestType.getByCode(typeRequete)
        if (!requestType) {
            throw new IllegalArgumentException("Type requ??te [${requestType}] invalide")
        }

        Sql sql
        String requeteToExecute = ""
        def resultat
        Map data
        String jsonResult
        boolean requeteSqlWithParam = false

        try {

            if (!requeteSql?.trim()) {
                throw new IllegalArgumentException("La requ??te ?? ex??cuter est vide")
            }
            // Utiliser la connexion SQL en param??tre en priorit?? si elle a ??t?? initialis??e
            if (sqlIn) {
                sql = sqlIn
            } else if (dataSourceIn) {
                sql = new Sql(dataSourceIn)
            } else {
                throw new IllegalStateException("Service sql  n'est pas correctement initialiser ou-bien il est inconnue")
            }

            // La requete ?? ex??cuter doit exister dans le cache de requ??tes ?? l'exception des proc??dures stock??es
            if (!(requestType in [RequestType.EXECUTESPROW, RequestType.EXECUTESP])) {
                requeteToExecute = getSql(requeteSql)

                log.debug("Requete to Execute -->"+requeteToExecute)
                if (!requeteToExecute?.trim()) {
                    throw new TaysirException("La requ??te ${requeteSql} n'existe pas dans le cache de requ??tes SQL")
                }
                requeteSqlWithParam = requeteToExecute.contains(':')
            }

            switch (requestType) {
                case RequestType.SELECT:
                    log.debug(JsonOutput.toJson([method:"executeRequete", requeteToExecute:requeteToExecute ,sqlParams: sqlParams]))
                    resultat = requeteSqlWithParam ? sql.rows(requeteToExecute, sqlParams) : sql.rows(requeteToExecute)
                    break
                case [RequestType.INSERT, RequestType.UPDATE, RequestType.DELETE, RequestType.CREATE, RequestType.DROP, RequestType.ALTER]:
                    resultat = requeteSqlWithParam ? sql.executeUpdate(requeteToExecute, sqlParams) : sql.executeUpdate(requeteToExecute)
                    break;
                case RequestType.EXECUTESP:
                    List<Object> listParams = new ArrayList<Object>(sqlParams.values());
                    resultat = callStoredProcedure(requeteSql, listParams, new Boolean(false))
                    break
                case RequestType.EXECUTESPROW:
                    List<Object> listParams = new ArrayList<Object>(sqlParams.values());
                    resultat = callStoredProcedure(requeteSql, listParams, new Boolean(true))
                    break
                default:
                    throw new TaysirException("La requ??te sql [${requeteSql}] est invalide! ")
            }

            // Retourner les donn??es
            if (requestType in [RequestType.EXECUTESPROW, RequestType.SELECT]) {
                resultat = (List<GroovyRowResult>) resultat
                data = [
                        success: true,
                        count  : resultat.size(),
                        data   : resultat
                ]
            } else if (RequestType.EXECUTESP == requestType) {
                data = [
                        success: true,
                        data   : resultat
                ]
            } else {
                data = [
                        success: true,
                        count  : resultat
                ]
            }

            jsonResult = JsonOutput.toJson(data)
            // log.debug("Resultat ===== ${jsonResult}")

            return jsonResult
        }
        catch (IllegalStateException | IllegalArgumentException | SQLException | TaysirException ex) {
            log.error(JsonOutput.toJson([method: "executeRequete", status: "error", message: ex.message]), ex)
            data = [
                    success     : false,
                    errorMessage: ex.message
            ]
            jsonResult = JsonOutput.toJson(data)
            return jsonResult
        }
    }

    /**
     *
     * Pour ??x??cuter une requ??te d'insertion en mode batch
     * Renvoie le r??sultat sous forme d'objet JSON contenant le r??sutat
     *
     */
    String bulkInsert(String requeteSql, List<Map> sqlParams) {

        Sql sql
        String requeteToExecute
        int[] resultat = null
        def data
        def jsonResult

        log.debug("requeteSql = ${requeteSql} , Param??tres sql : ${sqlParams}")

        try {

            // Utiliser la connexion SQL en param??tre en priorit?? si elle a ??t?? initialis??e
            if (sqlIn) {
                sql = sqlIn
            } else if (dataSourceIn) {
                sql = new Sql(dataSourceIn)
            } else {
                throw new IllegalStateException("Service sql not correctly initialized or unknown")
            }

            // La requete ?? ex??cuter doit exister dans le cache de requ??tes ?? l'exception des proc??dures stock??es
            if (!requeteSql?.trim()) {
                throw new IllegalArgumentException("Le code de la requ??te ?? ex??cuter est invalide")
            }
            requeteToExecute = getSql(requeteSql)
            if (!requeteToExecute?.trim()) {
                throw new TaysirException("La requ??te ${requeteSql} n'existe pas dans le cache de requ??tes SQL")
            }
            log.debug("requeteToExecute = ${requeteToExecute}")
            resultat = sql.withBatch(BATCH_SIZE, requeteToExecute) { ps ->
                sqlParams.each { row ->
                    ps.addBatch(row)
                }
            }

            data = [
                    success: true,
                    data   : resultat,
                    message: "R??cup??ration Compl??te"
            ]

            jsonResult = JsonOutput.toJson(data)
            log.debug jsonResult
        }
        catch (IllegalStateException | IllegalArgumentException | SQLException | TaysirException ex) {
            log.error(JsonOutput.toJson([method: "bulkInsert", status: "error", message: ex.message]), ex)
            data = [
                    success     : false,
                    errorMessage: ex.message,
                    message: ex.message
            ]
            jsonResult = JsonOutput.toJson(data)
        }

        return jsonResult
    }


    def callStoredProcedure(String procName, Map params, Boolean rows) throws IllegalStateException {
        List<Object> paramsObj = new ArrayList<Object>(params.values())
        return callStoredProcedure(procName, paramsObj, rows)
    }

    static Sql getSqlIn() {
        return sqlIn
    }

    static DatabaseType getDatabaseType() {
        return databaseType
    }

    /**
     *
     * Get an sql connexion from connexion parameters
     * this also set the service databaseType and sqlExecutor accordingly
     *
     * @param connexionParam
     * @return sql
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    Sql getSqlInstance(ConnexionParamDTO connexionParam) throws IllegalArgumentException, SQLException, ClassNotFoundException {

        if (!connexionParam) {
            throw new IllegalArgumentException("driverClassName is null")
        }
        if (!connexionParam.driverClassName?.trim()) {
            throw new IllegalArgumentException("driverClassName is null")
        }
        if (!connexionParam.url?.trim()) {
            throw new IllegalArgumentException("url is null")
        }
        if (!connexionParam.user?.trim()) {
            throw new IllegalArgumentException("user is null")
        }
        if (!connexionParam.password) {
            throw new IllegalArgumentException("password is null")
        }

        Sql sql = Sql.newInstance(connexionParam.url, connexionParam.password, connexionParam.password, connexionParam.driverClassName)

        return sql

    }

}
