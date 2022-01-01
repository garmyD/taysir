package sn.garmy.taysir.sql

import sn.garmy.tayisr.sql.DatabaseType

class SqlRequest {

    String id = UUID.randomUUID().toString()
    Long version



    String code
    String requestType
    String requestText
    String databaseType
    String comment
    String requestCategory

    static mapping = {
        requestText type: "text"
        comment type: "text"
        id generator: 'assigned'
    }


    static final List databaseTypes = [ DatabaseType.POSTGRESQL.toString()]
    static final List requestTypes = [RequestType.SELECT.toString(), RequestType.UPDATE.toString(), RequestType.INSERT.toString(), RequestType.DELETE.toString(), RequestType.CREATE.toString(), RequestType.DROP.toString(), RequestType.SCRIPT.toString(), RequestType.EXECUTESP.toString(), RequestType.EXECUTESPROW.toString(), RequestType.ALTER.toString()]

    static constraints = {
        code nullable: false, unique: true
        requestType nullable: false, inList: requestTypes
        databaseType nullable: false, inList: databaseTypes
        // Config pour PostgreSQL
        //id generator:'sequence', params:[sequence:'sql_request_id_seq']
        id generator: 'assigned'
    }

    static auditable = true
    Boolean deleted = false
    String userCreate
    String userUpdate
    Date dateCreated
    Date lastUpdated
    def springSecurityService
    // GORM Events
    def beforeInsert = {
        def userPrincipal = springSecurityService.currentUser
        if ((userPrincipal != null) && (userPrincipal != 'anonymousUser')) {
            userCreate = userPrincipal.username
        } else {
            userCreate = ""
        }
    }

    def beforeUpdate = {
        def userPrincipal = springSecurityService.currentUser
        if ((userPrincipal != null) && (userPrincipal != 'anonymousUser')) {
            userUpdate = userPrincipal.username
        } else {
            userUpdate = ""
        }
    }


}


