package sn.garmy.taysir

import javax.persistence.Version

class JwtInfo {


    String id = UUID.randomUUID().toString()
    @Version
    Long version
    String uid = UUID.randomUUID().toString()
    String code
    String accessToken
    String refreshToken
    String tokenType
    String roles
    String apiKey
    Integer expiresIn
    Date dateCreated
    Date lastUpdated


    static mapping = {
        id generator: 'assigned'
        accessToken type: 'text'
        refreshToken type: 'text'
    }

    static constraints = {
        uid nullable: false, unique: true
    }

}

