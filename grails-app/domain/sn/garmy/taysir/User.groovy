package sn.garmy.taysir

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.persistence.Version

@EqualsAndHashCode(includes = 'username')
@ToString(includes = 'username', includeNames = true, includePackage = false)
class User implements Serializable {

    private static final long serialVersionUID = 1

    String id = UUID.randomUUID().toString()
    @Version
    Long version

    String username
    //String password  //Gerer par Keycloack
    boolean enabled = true

    String nom
    String prenom
    String fonction
    String telephone
    String email

    //  boolean accountExpired = false
    // boolean accountLocked = false
    //  boolean passwordExpired = false

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    Set<UserRole> getUserRoles() {
        UserRole.findAllByUser(this) as Set<UserRole>
    }








    static constraints = {
        username nullable: false, blank: false, unique: true
    }

    static mapping = {
        id generator: 'assigned'
        table 'utilisateur'
    }
}
