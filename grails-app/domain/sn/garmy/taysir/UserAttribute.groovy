package sn.garmy.taysir

import javax.persistence.Version

class UserAttribute {

    String id = UUID.randomUUID().toString()
    @Version
    Long version

    User user
    String otherClaimsKey
    String otherClaimsValue

    static constraints = {
        user nullable: false
    }
    static mapping = {
        id generator: 'assigned'
    }
}