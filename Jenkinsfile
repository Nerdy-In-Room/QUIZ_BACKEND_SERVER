pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // GitHub 리포지토리에서 소스 코드를 체크아웃
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/dev']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Nerdy-In-Room/QUIZ_BACKEND_SERVER.git',
                        credentialsId: 'github-token'
                    ]]
                ])
            }
        }

        stage('Build') {
            steps {
                // 예시로 Gradle 빌드 시스템을 사용
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                // 테스트 단계
                sh './gradlew test'
            }
        }

        stage('Deploy') {
            steps {
                // 배포 단계
                sh './gradlew deploy'
            }
        }
    }

    post {
        always {
            // 빌드 결과를 항상 기록
            archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true
            junit '**/build/test-results/test/*.xml'
        }
        success {
            // 빌드 성공 시
            echo 'Build completed successfully'
        }
        failure {
            // 빌드 실패 시
            echo 'Build failed'
        }
    }
}