package sn.garmy.taysir

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.json.JsonOutput
import sn.garmy.tayisr.TaysirException

import javax.naming.Context
import javax.naming.InitialContext


@Transactional
class RuntimeConfigService {



    def grailsApplication

    Boolean isBetween(BigDecimal val, BigDecimal start, BigDecimal end) {
        return start <= val && end >= val
    }

    Boolean isBetween(Integer val, Integer start, Integer end) {
        return start <= val && end >= val
    }

    Map searchParams(String params) {
        Map searchParams = [:]
        if (params != null) {
            params.replace("[", "")?.replace("]", "")?.replace(", ", ",")?.split(",")?.each { sParams ->
                def nameAndValue = sParams.split(":")
                if (nameAndValue.size() > 1) {
                    searchParams.put(nameAndValue[0].trim(), nameAndValue[1])
                }
                else {
                    searchParams.put(nameAndValue[0], null)
                }
            }
        }

        return searchParams
    }


    Properties prop

    def loadPropertiesFile() {
        try {

            String propFileLocation
            if ((Environment.isDevelopmentMode()) || (Environment.getCurrent() == Environment.TEST)) {
                propFileLocation = grailsApplication.config.getProperty('propFile.location')
            }
            else {
                propFileLocation = ((Context) (new InitialContext().lookup("java:comp/env"))).lookup("propFileLocation")
            }
            if (propFileLocation == null) {
                throw new TaysirException("propFileLocation is not set")
            }

            InputStream input = new FileInputStream(propFileLocation)
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // Log properties
            prop.each { code, value ->
                if ((code.contains("password")) || (code.contains("secret"))) {
                    log.info(JsonOutput.toJson([method: "loadPropertiesFile", "${code}": "*************"]))
                }
                else {
                    log.info(JsonOutput.toJson([method: "loadPropertiesFile", "${code}": value]))
                }
            }
            this.prop = prop

        }
        catch (IOException ex) {
            log.error(JsonOutput.toJson([method: "loadPropertiesFile", message: ex.toString()]))
        }
        catch (TaysirException ex) {
            log.error(JsonOutput.toJson([method: "loadPropertiesFile", message: ex.toString()]))
        }
    }

    String getRuntimeProperty(String propertyCode) {
        return getRuntimeProperty(propertyCode, null)
    }

    String getRuntimeProperty(String propertyCode, String defaultValue) {

        String propertyValue
        if (this.prop == null) {
            throw new TaysirException("Property file not loaded!!!")
        }
        propertyValue = this.prop.getProperty(propertyCode)
        if (propertyValue == null) {
            if (defaultValue == null) {
                throw new TaysirException("Aucune valeur de propiété n'a été définie pour ${propertyCode}")
            }
            else {
                propertyValue = defaultValue
            }
        }
        return propertyValue
    }


    // ========================================= DEBUT PARAMETRES KEYCLOAK =============================================


    String getKeycloakRealm() {
        return getRuntimeProperty("keycloak.realm")
    }


    synchronized String getKeycloakSslRequired() {
        return getRuntimeProperty("keycloak.sslRequired")
    }


    synchronized String getKeycloakAuthServer() {
        return getRuntimeProperty("keycloak.authServer")
    }







    synchronized String getKeycloakResource() {
        return getRuntimeProperty("keycloak.resource")
    }


    synchronized String getKeycloakSecret() {
        return getRuntimeProperty("keycloak.secret")
    }


    synchronized String getKeycloakApiUser() {
        return getRuntimeProperty("keycloak.apiUser")
    }


    synchronized String getKeycloakApiPassword() {
        return getRuntimeProperty("keycloak.apiPassword")
    }



}
