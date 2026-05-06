pipeline {
    agent any

    parameters {
        string(name: 'LIMITED_ID', defaultValue: '3')
        string(name: 'OFFSET', defaultValue: '0')
        string(name: 'ITERATIONS_LIMIT', defaultValue: '1')
    }

    stages {

        stage('Config file') {
            steps {
                configFileProvider([
                    configFile(
                        fileId: 'dfd9e0d6-c951-4b08-b8d5-00fbac677041',
                        targetLocation: 'configuration.properties'
                    ),
                    configFile(
                        fileId: 'e2fa96d4-6a9f-4e3e-8866-bce458fee835',
                        targetLocation: 'src/test/resources/blocked-hosts.txt'
                    )
                ]) {
                    script {
                        def lines = readFile('configuration.properties')
                            .split('\n')
                            .collect { line ->

                                if (!line.contains('=')) return line

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

        stage('Run tests') {
            steps {
                bat 'type configuration.properties'
                bat 'mvn clean test'
                bat 'echo DEBUG_MARKER'
            }
        }
    }

    post {

        always {
            allure([
                includeProperties: false,
                jdk: '',
                results: [[path: 'target/allure-results']]
            ])
        }

        success {
            withCredentials([
                string(credentialsId: 'SLACK_WEBHOOK_URL', variable: 'SLACK_URL')
            ]) {
                powershell """
                Invoke-RestMethod -Uri "${SLACK_URL}" `
                -Method Post `
                -ContentType "application/json" `
                -Body '{\"text\":\"✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}\"}'
                """
            }
        }

        failure {

            // Slack
            withCredentials([
                string(credentialsId: 'SLACK_WEBHOOK_URL', variable: 'SLACK_URL')
            ]) {
                powershell """
                Invoke-RestMethod -Uri "${SLACK_URL}" `
                -Method Post `
                -ContentType "application/json" `
                -Body '{\"text\":\"❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}\"}'
                """
            }

            // Telegram
            withCredentials([
                string(credentialsId: 'TELEGRAM_TOKEN', variable: 'TOKEN'),
                string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'CHAT_ID')
            ]) {
                powershell """
                \$msg = "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}`n${env.BUILD_URL}`nAllure: ${env.BUILD_URL}allure"
                Invoke-RestMethod -Uri "https://api.telegram.org/bot${TOKEN}/sendMessage" `
                -Method Post `
                -Body @{
                    chat_id = "${CHAT_ID}"
                    text = \$msg
                }
                """
            }
        }

        unstable {

            // Slack
            withCredentials([
                string(credentialsId: 'SLACK_WEBHOOK_URL', variable: 'SLACK_URL')
            ]) {
                powershell """
                Invoke-RestMethod -Uri "${SLACK_URL}" `
                -Method Post `
                -ContentType "application/json" `
                -Body '{\"text\":\"⚠️ Build UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}\"}'
                """
            }

            // Telegram
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

        aborted {

            // Slack
            withCredentials([
                string(credentialsId: 'SLACK_WEBHOOK_URL', variable: 'SLACK_URL')
            ]) {
                powershell """
                Invoke-RestMethod -Uri "${SLACK_URL}" `
                -Method Post `
                -ContentType "application/json" `
                -Body '{\"text\":\"⛔ Build ABORTED: ${env.JOB_NAME} #${env.BUILD_NUMBER}\"}'
                """
            }

            // Telegram
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