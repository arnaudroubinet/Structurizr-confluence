workspace {
    name "ITMS : Instant Ticket Manager System"
    description "Instant Ticket Manager System and all the platforms with which it interacts."
    configuration {
        scope SoftwareSystem
        visibility private
        users {
            arnaud write
            guest read
        }
    }

    !docs documentation
    !adrs adrs

    model {
        terminal = person "Terminal"{
            tag terminal terminal_actor
        }
        operator = person "Operator"{
            tag operator operator_actor
        }

        s3 = softwareSystem "S3"{
            tag s3
        }

        iDecide = softwareSystem "iDecide"{
            tag iDecide
        }

        okta = softwareSystem "Okta"{
            tag okta
        }


        data = softwareSystem "Data platform"{
            tag data
        }

        audit_store = softwareSystem "Audit store"{
            tag audit_store
        }

        database =  softwareSystem "Postgresql"{
            tag database
        }
        itms_platform_retail = softwareSystem "Instant Ticket Manager System (Retail)" {
            tag retail
        }
        itms_platform_moteur = softwareSystem "Instant Ticket Manager System (Moteur)" {


            keycloak = container "Keycloak" {
                tag keycloak
            }


            backoffice = container "BackOffice" {
                tag operator
            }

            ingress_terminal = container "Ingress terminal"{
                technology "Envoy"
                description "Token validation, protocol break (HTTPS decryption/re-encryption), access logs (without payload), application of open telemetry headers"
                tag component terminal
            }

            ingress_iDecide = container "Ingress iDecide"{
                technology "Envoy"
                description "Token validation, protocol break (HTTPS decryption/re-encryption), access logs (without payload), application of open telemetry headers"
                tag component iDecide
            }


            egress_audit = container "Egress audit store"{
                technology "Egress"
                description "Authorised outbound flow to the audit store cluster"
                tag component audit_store
            }

            egress_okta = container "Egress okta"{
                technology "Egress"
                description "Authorised outbound flow to Okta"
                tag component okta
            }

            egress_data = container "Egress data"{
                technology "Egress"
                description "Authorised outbound flow to the data cluster"
                tag component data
            }

            egress_s3 = container "Egress S3"{
                technology "Egress"
                description "Authorised outbound flow to S3-compatible datagrid"
                tag component s3
            }

            egress_operator = container "Egress operator"{
                technology "Egress"
                description "Authorised outbound flow to the retail cluster"
                tag operator component
            }

            egress_postgreqsl = container "Egress postgresql"{
                technology "Egress"
                description "Authorised outbound flow to the DB cluster"
                tag component database

            }

            egress_terminal = container "Egress terminal"{
                technology "Egress"
                description "Authorised outbound flow to the retail cluster"
                tag terminal component
            }

            ingress_operator = container "Ingress operator"{
                technology "Envoy"
                description "Token validation, protocol break (HTTPS decryption/re-encryption), access logs (without payload), application of open telemetry headers"
                tag operator component
            }

            ims_operator = container "IMS operator"{
                tag operator
            }
            ims_terminal = container "IMS terminal"{
                tag terminal
            }
            ttp_operator = container "TTP operator"{
                tag operator
            }
            ttp_terminal = container "TTP terminal"{
                tag terminal
            }
     
            debezium = container "Debezium"{
                tag kafka
            }

            kafka  = container "Kafka + kafka connect"{
                tag kafka
            }
        }

        ingress_operator -> backoffice "Load backoffice" Https

        terminal -> ingress_terminal "Terminal actions" Https
        ingress_terminal -> ims_terminal "Terminal actions" Https
        ims_terminal -> ttp_terminal "Terminal actions" Https
        ttp_terminal -> ingress_terminal "Fetch winnings" Https

        operator -> ingress_operator "Operator actions" Https
        ingress_operator -> ims_operator "Operator actions" Https
        ingress_operator -> ttp_operator "Operator actions" Https

        ims_terminal -> egress_postgreqsl "Data persistence"
        ttp_terminal -> egress_postgreqsl "Data persistence"
        ims_operator -> egress_postgreqsl "Data persistence"
        ttp_operator -> egress_postgreqsl "Data persistence"

        debezium -> egress_postgreqsl "Replication" TCP/TLS
        egress_postgreqsl -> database "Read/Write access" TCP/TLS
        debezium -> kafka "Push events" Https

        ims_terminal -> egress_terminal "Update limits" Https
        ttp_terminal -> egress_terminal "Update limits" Https
        egress_terminal -> itms_platform_retail "Update limits" Https

        ims_operator -> egress_operator "Update limits" Https
        ttp_operator -> egress_operator "Update limits" Https
        egress_operator -> itms_platform_retail "Update limits" Https


        ims_operator -> egress_s3 "Read game configuration" Https
        egress_s3 -> s3 "Read game configuration" Https

        kafka -> egress_audit "Audit replication" TCP/TLS
        egress_audit -> audit_store "Audit replication" TCP/TLS

        kafka -> egress_data "Replication data" TCP/TLS
        egress_data -> data "Replication data" TCP/TLS

        iDecide -> ingress_iDecide "iDecide authentication/validate/lock/paid" Https
        ingress_iDecide -> ttp_terminal "iDecide validate/lock/paid" Https

        operator -> okta "Authentification" Https
        ims_operator -> egress_okta "Token validation" Https
        ttp_operator -> egress_okta "Token validation" Https
        egress_okta -> okta "Token validation" Https

        ingress_iDecide -> keycloak "Authentification" Https
        ttp_terminal -> keycloak "iDecide Token validation" Https
    }
    views {

        systemContext itms_platform_moteur itms_platform_moteur_context_view "Focus on ITMS platform" {
            title "System context view - The ITMS platform and links with external systems."
            description "This diagram shows the links between all the systems that interact with the ITMS platform."
            default

            include *
        }

        container itms_platform_moteur 01_itms_platform_moteur_all_view {
            title "All
            description "All"

            include *
        }

        styles {
            element "database" {
                background #6FBFE2
                shape cylinder
            }

            element "s3" {
                background #0069D9
                shape folder
            }

            element "okta" {
                background #0098d8
                shape folder
            }

            element "kafka" {
                background #C1B0E5
                shape pipe
            }

            element "terminal" {
                background #FFA69E
            }

            element "data" {
                background #7B52D4
            }

            element "terminal_actor" {
                shape webbrowser
            }

            element "operator_actor" {
                background #A9F0D1
                shape person
            }

            element "operator" {
                background #A9F0D1
            }

            element "iDecide" {
                background #F3DE8A
            }

            element "retail" {
                background #8C5E58
                shape folder
            }

            element "component"{
                shape component
            }

            element "audit_store" {
                background #7E7F9A
            }
        }

    }
}