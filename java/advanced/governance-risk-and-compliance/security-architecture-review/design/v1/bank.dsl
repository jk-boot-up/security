workspace "Online banking, first design" "The design as the team drew it." {

    model {
        customer = person "Customer" "Uses the web site and the mobile app." {
            properties {
                "id" "customer"
                "zone" "internet"
                "owner" "business"
            }
        }
        staff = person "Operations staff" "Support and operations." {
            properties {
                "id" "staff"
                "zone" "office"
                "owner" "operations"
            }
        }
        cards = softwareSystem "Card network" "Authorises card payments." "External" {
            properties {
                "id" "cards"
                "zone" "external"
                "owner" "payments-team"
                "adr" "0004"
            }
        }

        bank = softwareSystem "Online banking" {
            web = container "Web application" "Pages, sessions and forms." "Spring Boot" {
                properties {
                "id" "web"
                    "zone" "dmz"
                    "owner" "web-team"
                }
            }
            api = container "Payments API" "Transfers and statements." "Java" {
                properties {
                "id" "api"
                    "zone" "app"
                    "owner" "payments-team"
                    "adr" "0002"
                }
            }
            ledger = container "Ledger service" "Books every movement." "Java" {
                properties {
                "id" "ledger"
                    "zone" "app"
                    "owner" "payments-team"
                }
            }
            ledgerdb = container "Ledger database" "Accounts and balances." "PostgreSQL" "Datastore" {
                properties {
                "id" "ledgerdb"
                    "zone" "data"
                    "owner" "dba-team"
                    "classification" "confidential"
                    "encrypted-at-rest" "no"
                }
            }
            sessions = container "Session store" "Logged-in sessions." "Redis" "Datastore" {
                properties {
                "id" "sessions"
                    "zone" "data"
                    "owner" "web-team"
                    "classification" "internal"
                    "adr" "0003"
                    "encrypted-at-rest" "no"
                }
            }
            reports = container "Reporting job" "Nightly statements." "Java batch" {
                properties {
                "id" "reports"
                    "zone" "app"
                }
            }
        }

        customer -> web "Uses" "HTTPS" {
            properties {
                "encrypted" "yes"
                "auth" "session cookie"
            }
        }
        web -> sessions "Reads and writes sessions" "Redis protocol" {
            properties {
                "encrypted" "no"
                "auth" "password"
            }
        }
        web -> api "Calls" "REST" {
            properties {
                "encrypted" "yes"
                "auth" "service token"
            }
        }
        web -> ledgerdb "Reads balances for the home page" "JDBC" {
            properties {
                "encrypted" "yes"
                "auth" "database user"
            }
        }
        api -> ledger "Books transfers" "gRPC" {
            properties {
                "encrypted" "no"
                "auth" "none"
            }
        }
        ledger -> ledgerdb "Reads and writes" "JDBC" {
            properties {
                "encrypted" "yes"
                "auth" "database user"
            }
        }
        reports -> ledgerdb "Reads" "JDBC" {
            properties {
                "encrypted" "yes"
                "auth" "database user"
            }
        }
        api -> cards "Authorises payments" "HTTPS" {
            properties {
                "encrypted" "yes"
                "auth" "none"
            }
        }
        staff -> api "Support actions" "HTTPS" {
            properties {
                "encrypted" "yes"
                "auth" "single sign-on"
            }
        }
    }

    views {
        container bank "Containers" {
            include *
            autolayout lr
        }
    }
}
