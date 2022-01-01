package taysir

import grails.util.Environment
import sn.garmy.taysir.JwtInfo

class BootStrap {

    def initService
    def dataSource
    def sqlCatalogueService
    def runtimeConfigService
    def grailsApplication
    def keycloakHabilitationService

    def init = { servletContext ->


        sqlCatalogueService.initServiceFromDataSource(dataSource)
        sqlCatalogueService.loadSqlCache()
        log.info "======== Start load properties file ========"
        runtimeConfigService.loadPropertiesFile()
        log.info "======== End load properties file ========"


        switch (Environment.current) {

            case Environment.DEVELOPMENT:
                break
            case Environment.TEST:

                break
            case Environment.PRODUCTION:
                break

        }




        log.info "======== Start load properties file ========"
        keycloakHabilitationService.saveRequestmapToFile()

        runtimeConfigService.loadPropertiesFile()
        log.info "======== End load properties file ========"

        keycloakHabilitationService.generateKeycloakConfigFile()


        // Par mesure de sécurié, on supprime tous les token pour forcer une nouvelle connexion au démarrage de l'application
        log.info("___________________le nombre jwtInfo-> ${JwtInfo.count()}")
        JwtInfo.list().each {
            it.delete()
        }
        logApplicationInfo()


    }
    def destroy = {
    }

    def logApplicationInfo() {

        def appVersion = grailsApplication.metadata.getApplicationVersion()
        def grailsVersion = grailsApplication.metadata.getGrailsVersion()

        log.info("============================================================")
        log.info("      DEMARRAGE TAYSIR ")
        log.info("      --------------- ")
        log.info("    ")
        log.info(" Version de l'application : ${appVersion}")
        log.info(" Version de grails        : ${grailsVersion}")
        log.info(" System TimeZone          : ${TimeZone.getDefault().getDisplayName()} , ${TimeZone.getDefault().getID()}  ")
        log.info(" Keycloak url             : ${runtimeConfigService.getKeycloakAuthServer()}  ")
        log.info(" Keycloak realm           : ${runtimeConfigService.getKeycloakRealm()}  ")
        log.info(" Environnement            : ${Environment.current.name}")
        log.info("    ")
        log.info("============================================================")

    }
}
