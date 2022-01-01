package sn.garmy.taysir.securite

import groovy.json.JsonOutput

//import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.keycloak.KeycloakPrincipal
import org.keycloak.KeycloakSecurityContext
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken

//import org.keycloak.admin.client.Keycloak
//import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.AccessToken

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.springframework.boot.actuate.endpoint.SecurityContext
import sn.garmy.tayisr.ServiceResponse
import sn.garmy.tayisr.TaysirException
import sn.garmy.taysir.Role
import sn.garmy.taysir.User
import sn.garmy.taysir.UserAttribute
import sn.garmy.taysir.UserRole

import javax.servlet.http.HttpServletRequest

@Transactional
class KeycloakHabilitationService {

    GrailsApplication grailsApplication
    def runtimeConfigService
//    Keycloak kc
    //static final String SERVER_URL = "http://localhost:8085/auth"
    UserRole userRole
    /**
     * Cette methode permet de recuperer les informations personnels d'un Utilsateur
     * qui s'est connecte via Keycloak
     * @param token variable servant à recuperer les informations du Token USER
     * @param attributes les attributs de l'utilsateurs
     * @param otherClaims variables servant à recuperer les autres attributs definis dans Keycloak
     *
     */
    def userInfo(HttpServletRequest request) {

        ServiceResponse serviceResponse = new ServiceResponse()
        def roles

        //Recuperation de KeycloakAuthenticationToken Info
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal()


        log.debug("User principal $principal")

        if (principal == null) {
            serviceResponse.serviceOk = false
            log.error(JsonOutput.toJson([method: "userInfo", message: "User Information Not found"]))
            return serviceResponse
        }

        if (!(principal.principal instanceof KeycloakPrincipal)) {
            serviceResponse.serviceOk = false
            log.error(JsonOutput.toJson([method: "userInfo", message: "Principal is not an instance of KeycloakPrincipal"]))
            return serviceResponse
        }

        KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) principal.principal
        AccessToken token = kp.getKeycloakSecurityContext().getToken()

        User user = createOrUpdateUserFromToken(token)

        //createOrUpdateUserAttributes(user, token)

        log.info(JsonOutput.toJson([method: "userInfo", message: "Successful login", username: user?.username]))

        serviceResponse.serviceOk = true

        //Recuperation du role de l'utilisateur depuis le context
        roles = keycloakSecurityContextRole()
        serviceResponse.obj1 = roles
        serviceResponse.obj2 = user

        return serviceResponse
    }

    /**
     * Création ou mise à jour de l'utilisateur à partir du Token
     * @param token
     * @return
     */
    User createOrUpdateUserFromToken(AccessToken token) {

        //Recupération des Attributs du Token
        Map<String, Object> attributes = token.properties

        log.debug(JsonOutput.toJson([method  : "createOrUpdateUserFromToken",
                                     enabled : attributes.active,
                                     email   : attributes.email,
                                     prenom  : attributes.givenName,
                                     nom     : attributes.familyName,
                                     uid     : attributes.subject,
                                     username: attributes.preferredUsername,
                                     roles   : keycloakSecurityContextRole()]))

        User user = User.findByUsername(attributes.preferredUsername)

        boolean newUser = false
        boolean userUpdated = false

        if (user == null) {
            newUser = true
            user = new User()
        }
        user.enabled = attributes.active
        user.email = attributes.email
        user.nom = attributes.familyName
        user.prenom = attributes.givenName
        user.username = attributes.preferredUsername

        // isDirty ne semble pas marcher
        if ((user.enabled != attributes.active) || (user.email != attributes.email)|| (user.nom != attributes.familyName)|| (user.prenom != attributes.givenName)|| (user.username != attributes.preferredUsername)){
            userUpdated=true
        }

        // Loguer ...
        if (userUpdated || newUser ) {
            String message = (newUser) ? "Creating new user..." : "Updating user attributes..."

            log.info(JsonOutput.toJson([method  : "createOrUpdateUserFromToken",
                                        message : message,
                                        enabled : attributes.active,
                                        email   : attributes.email,
                                        prenom  : attributes.givenName,
                                        nom     : attributes.familyName,
                                        uid     : attributes.subject,
                                        username: attributes.preferredUsername,
                                        roles   : keycloakSecurityContextRole()]))
        }

        if ((newUser || userUpdated) && (user.save() == null) ) {
            log.error(JsonOutput.toJson([method: "createOrUpdateUserFromToken", message: user?.errors?.toString()]))
            throw new TaysirException("Error create or update user from Token ${user?.errors?.toString()} ")
        }

        // update user roles
        def role
        def keyCloackRoles = keycloakSecurityContextRole()

        // Suppression des rôles qui ne sont plus affectés
        user.userRoles?.role.each {
            if (!keyCloackRoles.contains(it.authority)) {
                UserRole.remove(user, it)
                log.info(JsonOutput.toJson([method: "createOrUpdateUserFromToken", message: "Le role ${it.toString()} a été retiré de ${user.username} "]))
            }
        }

        // Ajout des nouveaux rôles
        keycloakSecurityContextRole().each {

            // Création du nouveau rôle si inexistant
            role = Role.findWhere(authority: it)
            if (role == null) {
                log.info(JsonOutput.toJson([method: "createOrUpdateUserFromToken", message: "Création du nouveau rôle ${it.toString()} "]))
                role = new Role(authority: it)
                if(!role.save()){
                    throw new TaysirException(" Erreur sur la creation de Role   =>>  ${menuMapParent.errors}")
                }
            }

            // Rattacchement du nouveau rôle
            if (UserRole.findWhere(user: user, role: role) == null) {
                UserRole userRole = new UserRole(user: user, role: role)
                if(!userRole.save()){
                    throw new TaysirException(" Erreur sur la creation userRole   =>>  ${menuMapParent.errors}")
                }
                log.info(JsonOutput.toJson([method: "createOrUpdateUserFromToken", message: "Le role ${it.toString()} a été ajouté à l'utilisateur ${user.username} "]))
            }
        }

        return user

    }

    /**
     * Cette methode permet de recuperer les autres attributs personnels d'un Utilsateur
     * qui s'est connecte via Keycloak
     * Elle prend en parametre deux valeurs
     * @param user
     * @param otherClaims variables servant à recuperer les autres attributs definis dans Keycloak
     * @flo
     */

    def createOrUpdateUserAttributes(User user, AccessToken token) {

        if (user == null) {
            throw new IllegalArgumentException("user is null")
        }
        if (token == null) {
            throw new IllegalArgumentException("token is null")
        }

        //Recuperation des autres attributs du Token
        //    Attention, il faut les configurer au niveau de keycloak  ==> https://stackoverflow.com/questions/32678883/keycloak-retrieve-custom-attributes-to-keycloakprincipal
        Map<String, Object> otherClaims = token.getOtherClaims()

        if (otherClaims == null) {
            log.warn(JsonOutput.toJson([method: "createOrUpdateUserAttributes", message: "No other attributes for user ${user?.username}"]))
            return
        }

        // Suppression des attributs non utilisés
        user.userAttributes.each {
            if (!otherClaims.containsKey(it.otherClaimsKey)) {
                UserAttribute.remove(it)
                log.info(JsonOutput.toJson([method: "createOrUpdateUserAttributes", message: "L'attribut ${it.otherClaimsKey} a été supprimé pour ${user?.username} "]))
            }
        }

        otherClaims.each { key, value ->
            UserAttribute userAttribute = UserAttribute.findByUserAndOtherClaimsKey(user, key)
            if (userAttribute == null) {
                userAttribute = new UserAttribute(user: user, otherClaimsKey: key, otherClaimsValue: value)
                if(!userAttribute.save()){
                    throw new TaysirException(" Erreur sur la creation  userAttibute  =>>  ${userAttribute.errors}")
                }
                log.info(JsonOutput.toJson([method: "createOrUpdateUserAttributes", message: "L'attribut ${key}/${value} a été ajouté pour ${user?.username} "]))
            }
            else if (userAttribute.otherClaimsValue != value) {
                log.info(JsonOutput.toJson([method: "createOrUpdateUserAttributes", message: "L'attribut ${key}/${userAttribute.otherClaimsValue} a été modifié pour ${user?.username}, new value ${value} "]))
                userAttribute.otherClaimsValue = value
                userAttribute.save()
            }
        }

    }

    /**
     * Fonction servant à enregistrer les Requestmaps d'Admin
     * dans un fichier JSON
     * @return
     */
    def saveRequestmapToFile() {

        ServiceResponse serviceResponse = new ServiceResponse()
        def requestMapFile

        requestMapFile = grailsApplication.mainContext.getResource("WEB-INF/Requestmap.json")
        if (requestMapFile == null) {
            throw new TaysirException("file WEB-INF/Requestmap.json does not exists, create an empty one if any")
        }

        def listRequest = Requestmap.all.collect {
            [url: it.url, configAttribute: it.configAttribute]
        }
        String mapJson = JsonOutput.toJson(listRequest)


        def monFichier = requestMapFile.file
        monFichier.text = mapJson

        serviceResponse.serviceOk = true

        return serviceResponse
    }

    /**
     * Fonction servant à enregistrer le fichier de config Keycloak à partir du contexte tomcat
     * dans un fichier JSON
     * @return
     */
    def generateKeycloakConfigFile() {

        ServiceResponse serviceResponse = new ServiceResponse()
        def keycloakFile

        keycloakFile = grailsApplication.mainContext.getResource("WEB-INF/keycloak.json")
        if (keycloakFile == null) {
            throw new TaysirException("initial file WEB-INF/keycloak.json does not exists")
        }

        String keycloakFilePath = keycloakFile.getURL()
        log.debug(JsonOutput.toJson([method: "generateKeycloakConfigFile", keycloakFilePath: keycloakFilePath]))

        def keycloakParams = ["realm"                     : runtimeConfigService.getKeycloakRealm(),
                              "auth-server-url"           : runtimeConfigService.getKeycloakAuthServer(),
                              "ssl-required"              : runtimeConfigService.getKeycloakSslRequired(),
                              "resource"                  : runtimeConfigService.getKeycloakResource(),
                              "credentials"               : ["secret": runtimeConfigService.getKeycloakSecret()],
                              "use-resource-role-mappings": true]

        String mapJson = JsonOutput.toJson(keycloakParams)
        log.debug(JsonOutput.toJson([method: "generateKeycloakConfigFile", mapJson: mapJson]))

        def monFichier = keycloakFile.file
        monFichier.text = mapJson

        serviceResponse.serviceOk = true

        return serviceResponse
    }

    /**
     * Cette fonction permet de retourner les rôles d'un utilisateur
     * depuis Keycloak
     * @return
     */
    def keycloakSecurityContextRole() {

        SecurityContext context = SecurityContextHolder.getContext()
        // User Granted Authorities Recuperation
        def authorities = context.getAuthentication().getAuthorities()
        log.debug(JsonOutput.toJson([method: "keycloakSecurityContextRole", authorities: authorities]))
        def roleJson = JSON.parse(JsonOutput.toJson(authorities))

        roleJson = roleJson.collect {
            it.authority
        }

        return roleJson

    }

    User currentUser() {
        return WebUtils.retrieveGrailsWebRequest().session.currentUser
    }
}
