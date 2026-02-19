// =============================================================================
// DevSecOps Security-First Jenkins Pipeline
// =============================================================================
// Stages: Checkout → SCA → Dockerfile Lint → Unit Tests → Build WAR →
//         Docker Build → Vulnerability Scan → ECR Push →
//         Terraform Plan → Manual Approval → Terraform Apply → K8s Deploy
// =============================================================================

pipeline {
    agent any

    environment {
        AWS_ACCOUNT_ID   = credentials('aws-account-id')
        AWS_REGION       = 'us-east-1'
        ECR_REPO         = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/devsecops-pipeline-app"
        IMAGE_TAG        = "${BUILD_NUMBER}"
        CLUSTER_NAME     = 'devsecops-pipeline-prod'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // ================================================================
        // STAGE 1: Checkout Source Code
        // ================================================================
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
                echo "✅ Source code checked out successfully"
            }
        }

        // ================================================================
        // STAGE 2: Static Code Analysis (SCA) with Trivy
        // ================================================================
        stage('SCA - Trivy FS Scan') {
            steps {
                echo "🔍 Running Static Code Analysis on application source..."
                sh '''
                    trivy fs --severity HIGH,CRITICAL \
                             --format table \
                             --exit-code 0 \
                             --output trivy-sca-report.txt \
                             app/
                '''
                archiveArtifacts artifacts: 'trivy-sca-report.txt', allowEmptyArchive: true
                echo "✅ SCA scan completed — review trivy-sca-report.txt"
            }
        }

        // ================================================================
        // STAGE 3: Dockerfile Linting with Hadolint
        // ================================================================
        stage('Dockerfile Lint - Hadolint') {
            steps {
                echo "📋 Linting Dockerfile for best practices..."
                sh '''
                    hadolint Dockerfile --format tty || true
                    hadolint Dockerfile --format json > hadolint-report.json || true
                '''
                archiveArtifacts artifacts: 'hadolint-report.json', allowEmptyArchive: true
                echo "✅ Dockerfile lint completed — review hadolint-report.json"
            }
        }

        // ================================================================
        // STAGE 4: Unit Tests
        // ================================================================
        stage('Unit Tests') {
            steps {
                echo "🧪 Running unit tests..."
                dir('app') {
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/target/surefire-reports/*.xml'
                }
            }
        }

        // ================================================================
        // STAGE 5: Build WAR Artifact
        // ================================================================
        stage('Build WAR') {
            steps {
                echo "🏗️ Building application WAR..."
                dir('app') {
                    sh 'mvn package -DskipTests -B'
                }
                echo "✅ WAR built successfully"
            }
        }

        // ================================================================
        // STAGE 6: Docker Build
        // ================================================================
        stage('Docker Build') {
            steps {
                echo "🐳 Building Docker image..."
                sh """
                    docker build -t ${ECR_REPO}:${IMAGE_TAG} \
                                 -t ${ECR_REPO}:latest \
                                 .
                """
                echo "✅ Docker image built: ${ECR_REPO}:${IMAGE_TAG}"
            }
        }

        // ================================================================
        // STAGE 7: Container Vulnerability Scan with Trivy
        // ================================================================
        // ** QUALITY GATE: Fails the build on HIGH or CRITICAL vulns **
        // ================================================================
        stage('Container Vuln Scan - Trivy') {
            steps {
                echo "🛡️ Scanning Docker image for vulnerabilities..."
                sh """
                    trivy image --severity HIGH,CRITICAL \
                                --format table \
                                --output trivy-image-report.txt \
                                ${ECR_REPO}:${IMAGE_TAG}
                """
                archiveArtifacts artifacts: 'trivy-image-report.txt', allowEmptyArchive: true

                echo "🚨 Running quality gate — failing on HIGH/CRITICAL..."
                sh """
                    trivy image --severity HIGH,CRITICAL \
                                --exit-code 1 \
                                ${ECR_REPO}:${IMAGE_TAG}
                """
                echo "✅ Image passed vulnerability scan"
            }
        }

        // ================================================================
        // STAGE 8: ECR Login & Push
        // ================================================================
        stage('ECR Push') {
            steps {
                echo "🔐 Authenticating with Amazon ECR..."
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | \
                    docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                """

                echo "📤 Pushing image to ECR..."
                sh """
                    docker push ${ECR_REPO}:${IMAGE_TAG}
                    docker push ${ECR_REPO}:latest
                """
                echo "✅ Image pushed to ECR: ${ECR_REPO}:${IMAGE_TAG}"
            }
        }

        // ================================================================
        // STAGE 9: Terraform Plan
        // ================================================================
        stage('Terraform Plan') {
            steps {
                echo "📐 Running Terraform plan..."
                dir('terraform') {
                    sh '''
                        terraform init -input=false
                        terraform plan -out=tfplan -input=false
                    '''
                }
                archiveArtifacts artifacts: 'terraform/tfplan', allowEmptyArchive: true
                echo "✅ Terraform plan created — review before approving"
            }
        }

        // ================================================================
        // STAGE 10: Manual Approval Gate
        // ================================================================
        stage('Approval') {
            steps {
                echo "⏸️ Waiting for manual approval..."
                input message: 'Review the Terraform plan and Docker scan results. Proceed with deployment?',
                      ok: 'Deploy',
                      submitter: 'admin,devops-team'
            }
        }

        // ================================================================
        // STAGE 11: Terraform Apply
        // ================================================================
        stage('Terraform Apply') {
            steps {
                echo "🚀 Applying Terraform changes..."
                dir('terraform') {
                    sh 'terraform apply -auto-approve tfplan'
                }
                echo "✅ Infrastructure provisioned successfully"
            }
        }

        // ================================================================
        // STAGE 12: Deploy to EKS
        // ================================================================
        stage('Deploy to EKS') {
            steps {
                echo "☸️ Deploying to Kubernetes..."
                sh """
                    aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}

                    # Update the image tag in the deployment manifest
                    sed -i 's|IMAGE_PLACEHOLDER|${ECR_REPO}:${IMAGE_TAG}|g' kubernetes/deployment.yaml

                    kubectl apply -f kubernetes/namespace.yaml
                    kubectl apply -f kubernetes/service-account.yaml
                    kubectl apply -f kubernetes/deployment.yaml
                    kubectl apply -f kubernetes/service.yaml
                    kubectl apply -f kubernetes/hpa.yaml
                    kubectl apply -f kubernetes/ingress.yaml

                    # Wait for rollout to complete
                    kubectl rollout status deployment/vprofile-app -n vprofile --timeout=300s
                """
                echo "✅ Application deployed to EKS successfully"
            }
        }
    }

    // ====================================================================
    // POST-BUILD ACTIONS
    // ====================================================================
    post {
        always {
            echo "🧹 Cleaning up Docker images..."
            sh """
                docker rmi ${ECR_REPO}:${IMAGE_TAG} || true
                docker rmi ${ECR_REPO}:latest || true
            """
        }
        success {
            echo """
            ============================================================
            ✅ DevSecOps Pipeline SUCCEEDED
            ============================================================
            Image:   ${ECR_REPO}:${IMAGE_TAG}
            Cluster: ${CLUSTER_NAME}
            Region:  ${AWS_REGION}
            ============================================================
            """
            // Uncomment for Slack notification:
            // slackSend channel: '#deployments',
            //     color: 'good',
            //     message: "✅ Deployment SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
        failure {
            echo """
            ============================================================
            ❌ DevSecOps Pipeline FAILED
            ============================================================
            Check the stage logs for details.
            ============================================================
            """
            // Uncomment for Slack notification:
            // slackSend channel: '#deployments',
            //     color: 'danger',
            //     message: "❌ Deployment FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
    }
}
