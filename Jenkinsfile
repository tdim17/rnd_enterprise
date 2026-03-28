pipeline {
    agent any

    // Input parameters for test configuration
    parameters {
        string(name: 'LIMITED_ID', defaultValue: '3')
        string(name: 'OFFSET', defaultValue: '0')
        string(name: 'ITERATIONS_LIMIT', defaultValue: '1')
    }

    // Main pipeline execution stages
    stages {

        // Clone source code from repository
        //stage('Checkout') {
        //    steps {
        //        git branch: 'main',
        //        url: 'https://github.com/tdim17/rnd_enterprise.git'
        //   }
        // }

        // Prepare and override configuration.properties
        stage('Config file') {
            steps {
                configFileProvider([
                    configFile(
                        fileId: 'dfd9e0d6-c951-4b08-b8d5-00fbac677041',
                        targetLocation: 'configuration.properties'
                    )
                ]) {

                    script {
                        def lines = readFile('configuration.properties')
                                .split('\n')
                                .collect { line ->

                                    if (!line.contains('=')) {
                                        return line
                                    }

                                    def key = line.split('=')[0].trim()

                                    if (key == 'idNumberInResponseLimit') {
                                        return "idNumberInResponseLimit=${params.LIMITED_ID}"
                                    }

                                    if (key == 'offsetParam') {
                                        return "offsetParam=${params.OFFSET}"
                                    }

                                    if (key == 'iterationsLimit') {
                                        return "iterationsLimit=${params.ITERATIONS_LIMIT}"
                                    }

                                    return line
                                }

                        writeFile(
                            file: 'configuration.properties',
                            text: lines.join('\n')
                        )
                    }
                }
            }
        }

        // Execute automated tests
        stage('Run tests') {
             steps {
                bat 'type configuration.properties'
                bat 'mvn clean test'
                bat 'echo DEBUG_MARKER'
                // bat 'exit 1'
            }
        }
    }

    // Post-build notifications and status handling
    post {

    always {
        allure([
            includeProperties: false,
            jdk: '',
            results: [[path: 'target/allure-results']]
        ])
    }

    failure {
        withCredentials([
            string(credentialsId: 'TELEGRAM_TOKEN', variable: 'TOKEN'),
            string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'CHAT_ID')
        ]) {
            powershell """
            \$msg = "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}`n${env.BUILD_URL}"
            Invoke-RestMethod -Uri "https://api.telegram.org/bot${TOKEN}/sendMessage" `
            -Method Post `
            -Body @{
                chat_id = "${CHAT_ID}"
                text = \$msg
            }
            """
        }
    }

    // Telegram notification on UNSTABLE
    unstable {
        withCredentials([
            string(credentialsId: 'TELEGRAM_TOKEN', variable: 'TOKEN'),
            string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'CHAT_ID')
        ]) {
            powershell """
            \$msg = "⚠️ Build UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}`n${env.BUILD_URL}"
            Invoke-RestMethod -Uri "https://api.telegram.org/bot${TOKEN}/sendMessage" `
            -Method Post `
            -Body @{
                chat_id = "${CHAT_ID}"
                text = \$msg
            }
            """
        }
    }

    // Telegram notification on ABORTED
    aborted {
        withCredentials([
            string(credentialsId: 'TELEGRAM_TOKEN', variable: 'TOKEN'),
            string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'CHAT_ID')
        ]) {
            powershell """
            \$msg = "⛔ Build ABORTED: ${env.JOB_NAME} #${env.BUILD_NUMBER}`n${env.BUILD_URL}"
            Invoke-RestMethod -Uri "https://api.telegram.org/bot${TOKEN}/sendMessage" `
            -Method Post `
            -Body @{
                chat_id = "${CHAT_ID}"
                text = \$msg
                }
                """
            }
        }
    }
}