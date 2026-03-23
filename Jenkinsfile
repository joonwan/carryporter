pipeline {
    agent any
    
    options {
        skipDefaultCheckout(true)
        timestamps()
    }
    
    environment {
        TARGET_BRANCH = "release"
        DOCKER_NETWORK = "app-network"

        // Backend
        BACKEND_CONTAINER = "backend"
        BACKEND_IMAGE = "spring-boot-app:latest"

        // Frontend (Nginx에 React 정적 파일 내장)
        NGINX_CONTAINER = "nginx"
        NGINX_IMAGE = "nginx-frontend:latest"

        // Admin Frontend
        ADMIN_NGINX_CONTAINER = "nginx-admin"
        ADMIN_NGINX_IMAGE = "nginx-admin:latest"

        // Infrastructure
        MYSQL_CONTAINER = "mysql"
        REDIS_CONTAINER = "redis"
        MOSQUITTO_CONTAINER = "mosquitto"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Check Branch & Changes') {
            steps {
                script {
                    // 현재 브랜치 확인
                    def branch = env.GIT_BRANCH?.replaceAll('origin/', '') ?: ''

                    if (!branch || branch == 'HEAD') {
                        branch = sh(
                            script: "git branch -r --contains HEAD | grep -o 'origin/[^[:space:]]*' | head -n 1 | sed 's|origin/||'",
                            returnStdout: true
                        ).trim()
                    }

                    echo "Current branch: ${branch}"

                    // release 브랜치가 아니면 스킵
                    if (branch != env.TARGET_BRANCH) {
                        echo "Not on '${env.TARGET_BRANCH}' branch. Skipping deployment."
                        currentBuild.result = 'NOT_BUILT'
                        error("Branch mismatch: expected '${env.TARGET_BRANCH}', got '${branch}'")
                    }

                    // 변경된 파일 목록 확인
                    def changes = []
                    try {
                        // 병합 커밋도 감지할 수 있도록 -m 옵션 추가
                        def result = sh(
                            script: "git diff-tree -m --no-commit-id --name-only -r HEAD 2>/dev/null || git diff --name-only HEAD~1 HEAD 2>/dev/null || echo 'backend/'",
                            returnStdout: true
                        ).trim()
                        changes = result ? result.split('\n') : ['backend/']
                    } catch (Exception e) {
                        echo "First commit or unable to get diff, proceeding with build"
                        changes = ['backend/']
                    }

                    echo "Changed files: ${changes}"

                    def jenkinsfileChanged = changes.any { it.contains('Jenkinsfile') }
                    def backendChanged = changes.any { it.startsWith('backend/') }
                    // frontend/ 하위의 모든 변경 감지
                    def frontendCodeChanged = changes.any { it.startsWith('frontend/') }
                    // admin-frontend/ 변경 감지
                    def adminFrontendChanged = changes.any { it.startsWith('admin-frontend/') }
                    // nginx/ 설정 변경 감지 (루트의 nginx 폴더)
                    def nginxConfChanged = changes.any { it.startsWith('nginx/') && !it.startsWith('nginx/admin/') }
                    // nginx/admin/ 설정 변경 감지
                    def nginxAdminConfChanged = changes.any { it.startsWith('nginx/admin/') }

                    // Jenkinsfile이 바뀌면 전체 빌드
                    env.BUILD_BACKEND = (jenkinsfileChanged || backendChanged) ? 'true' : 'false'
                    // React 코드 변경 → React 빌드 + nginx 이미지 재생성
                    env.BUILD_FRONTEND = (jenkinsfileChanged || frontendCodeChanged) ? 'true' : 'false'
                    // Admin React 코드 변경 → Admin 빌드
                    env.BUILD_ADMIN_FRONTEND = (jenkinsfileChanged || adminFrontendChanged || nginxAdminConfChanged) ? 'true' : 'false'
                    // nginx 설정만 변경 → nginx 이미지만 재생성 (React 빌드는 Docker 캐시 사용)
                    env.BUILD_NGINX_CONF = (nginxConfChanged) ? 'true' : 'false'

                    if (!jenkinsfileChanged && !backendChanged && !frontendCodeChanged && !adminFrontendChanged && !nginxConfChanged && !nginxAdminConfChanged) {
                        echo "No relevant changes detected. Skipping deployment."
                        currentBuild.result = 'NOT_BUILT'
                        error("No backend, frontend, admin-frontend, or Jenkinsfile changes detected")
                    }

                    echo "Build Triggered - Backend: ${env.BUILD_BACKEND}, Frontend: ${env.BUILD_FRONTEND}, Admin: ${env.BUILD_ADMIN_FRONTEND}, Nginx Conf: ${env.BUILD_NGINX_CONF}"
                }
            }
        }

        stage('Ensure Infrastructure') {
            steps {
                script {
                    sh '''
                        set -e
                        echo "Checking Docker network..."
                        docker network inspect ${DOCKER_NETWORK} >/dev/null 2>&1 || docker network create ${DOCKER_NETWORK}

                        echo "Checking infrastructure containers..."
                        if ! docker ps | grep -q ${MYSQL_CONTAINER}; then
                            echo "MySQL not running. Infrastructure must be started manually on the server."
                            echo "Run: cd ~/infra && docker-compose up -d"
                            exit 1
                        else
                            echo "Infrastructure already running"
                        fi
                    '''
                }
            }
        }

        stage('Build Backend (Gradle)') {
            when {
                expression { env.BUILD_BACKEND == 'true' }
            }
            steps {
                dir('backend') {
                    sh '''
                        set -e
                        echo "Building Spring Boot application..."
                        chmod +x gradlew
                        ./gradlew clean build -x test
                    '''
                }
            }
        }

        stage('Docker Build & Deploy Backend') {
            when {
                expression { env.BUILD_BACKEND == 'true' }
            }
            steps {
                dir('backend') {
                    script {
                        withCredentials([file(credentialsId: 'backend-env-file', variable: 'SECRET_ENV_PATH')]) {
                            sh '''
                                set -e
                                echo "Building Backend Docker image..."
                                docker build -t ${BACKEND_IMAGE} .

                                echo "Deploying Backend..."
                                docker stop ${BACKEND_CONTAINER} 2>/dev/null || true
                                docker rm ${BACKEND_CONTAINER} 2>/dev/null || true

                                docker run -d \
                                    --name ${BACKEND_CONTAINER} \
                                    --network ${DOCKER_NETWORK} \
                                    --restart unless-stopped \
                                    --env-file ${SECRET_ENV_PATH} \
                                    ${BACKEND_IMAGE}

                                sleep 5
                                docker ps | grep ${BACKEND_CONTAINER}
                                docker exec ${NGINX_CONTAINER} nginx -s reload
                            '''
                        }
                    }
                }
            }
        }

        stage('Build & Deploy Frontend (Blue-Green)') {
            when {
                expression { env.BUILD_FRONTEND == 'true' }
            }
            steps {
                sh '''
                    set -euo pipefail
                    echo "=== Frontend Blue-Green Deployment ==="

                    # 디렉토리 초기화 (최초 실행 시)
                    mkdir -p /home/ubuntu/frontend/dist-blue
                    mkdir -p /home/ubuntu/frontend/dist-green

                    # 현재 활성 색상 확인 (없으면 blue가 기본)
                    CURRENT_COLOR=$(cat /home/ubuntu/frontend/active_color 2>/dev/null || echo "blue")

                    # 배포 대상 색상 결정 (토글)
                    if [ "$CURRENT_COLOR" = "blue" ]; then
                        TARGET_COLOR="green"
                    else
                        TARGET_COLOR="blue"
                    fi

                    echo "Current: $CURRENT_COLOR -> Target: $TARGET_COLOR"

                    # 1. React 빌드
                    echo "Building React application..."
                    docker build --no-cache -t frontend-builder -f nginx/Dockerfile .

                    # 2. 빌드 결과물을 대상 디렉토리에 복사
                    echo "Copying build output to dist-$TARGET_COLOR..."
                    rm -rf /home/ubuntu/frontend/dist-$TARGET_COLOR/*
                    docker run --rm -v /home/ubuntu/frontend/dist-$TARGET_COLOR:/output frontend-builder sh -c "cp -r /tmp/dist/* /output/"

                    # 3. nginx.conf 생성 (placeholder 치환)
                    echo "Generating nginx config for $TARGET_COLOR..."
                    ADMIN_COLOR=$(cat /home/ubuntu/admin-frontend/active_color 2>/dev/null || echo "blue")
                    cp nginx/default.conf /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__FRONT_ROOT__|/home/ubuntu/frontend/dist-$TARGET_COLOR|g" /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__ADMIN_ROOT__|/home/ubuntu/admin-frontend/dist-$ADMIN_COLOR|g" /home/ubuntu/frontend/nginx.conf

                    # 4. 설정 검증 후 restart
                    echo "Validating and restarting nginx..."
                    cat /home/ubuntu/frontend/nginx.conf | docker exec -i ${NGINX_CONTAINER} sh -c "cat > /etc/nginx/conf.d/default.conf"
                    docker exec ${NGINX_CONTAINER} nginx -t
                    docker exec ${NGINX_CONTAINER} nginx -s reload

                    # 5. 활성 색상 업데이트
                    echo "$TARGET_COLOR" > /home/ubuntu/frontend/active_color

                    echo "=== Frontend deployed to $TARGET_COLOR (zero downtime) ==="
                '''
            }
        }

        stage('Deploy Nginx Config Only') {
            when {
                expression { env.BUILD_NGINX_CONF == 'true' && env.BUILD_FRONTEND != 'true' && env.BUILD_ADMIN_FRONTEND != 'true' }
            }
            steps {
                sh '''
                    set -euo pipefail
                    echo "Updating Nginx config..."

                    # 현재 활성 색상 확인
                    CURRENT_COLOR=$(cat /home/ubuntu/frontend/active_color 2>/dev/null || echo "blue")
                    ADMIN_COLOR=$(cat /home/ubuntu/admin-frontend/active_color 2>/dev/null || echo "blue")

                    # nginx.conf 생성 (placeholder 치환)
                    cp nginx/default.conf /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__FRONT_ROOT__|/home/ubuntu/frontend/dist-$CURRENT_COLOR|g" /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__ADMIN_ROOT__|/home/ubuntu/admin-frontend/dist-$ADMIN_COLOR|g" /home/ubuntu/frontend/nginx.conf

                    # 설정 검증 후 restart
                    cat /home/ubuntu/frontend/nginx.conf | docker exec -i ${NGINX_CONTAINER} sh -c "cat > /etc/nginx/conf.d/default.conf"
                    docker exec ${NGINX_CONTAINER} nginx -t
                    docker exec ${NGINX_CONTAINER} nginx -s reload

                    echo "Nginx config updated (active: $CURRENT_COLOR, admin: $ADMIN_COLOR)"
                '''
            }
        }

        stage('Build & Deploy Admin Frontend (Blue-Green)') {
            when {
                expression { env.BUILD_ADMIN_FRONTEND == 'true' }
            }
            steps {
                sh '''
                    set -euo pipefail
                    echo "=== Admin Frontend Blue-Green Deployment ==="

                    # 디렉토리 초기화 (최초 실행 시)
                    mkdir -p /home/ubuntu/admin-frontend/dist-blue
                    mkdir -p /home/ubuntu/admin-frontend/dist-green

                    # 현재 활성 색상 확인 (없으면 blue가 기본)
                    CURRENT_COLOR=$(cat /home/ubuntu/admin-frontend/active_color 2>/dev/null || echo "blue")

                    # 배포 대상 색상 결정 (토글)
                    if [ "$CURRENT_COLOR" = "blue" ]; then
                        TARGET_COLOR="green"
                    else
                        TARGET_COLOR="blue"
                    fi

                    echo "Current: $CURRENT_COLOR -> Target: $TARGET_COLOR"

                    # 1. Admin React 빌드
                    echo "Building Admin React application..."
                    # 주의: Admin Dockerfile 경로가 nginx/admin/Dockerfile 인지 확인 필요
                    docker build --no-cache -t admin-frontend-builder -f nginx/admin/Dockerfile .

                    # 2. 빌드 결과물을 대상 디렉토리에 복사
                    echo "Copying build output to dist-$TARGET_COLOR..."
                    rm -rf /home/ubuntu/admin-frontend/dist-$TARGET_COLOR/*
                    # 주의: Dockerfile 내부에서 빌드 결과물이 /tmp/dist 에 생성되는지 확인 필요
                    docker run --rm -v /home/ubuntu/admin-frontend/dist-$TARGET_COLOR:/output admin-frontend-builder sh -c "cp -r /tmp/dist/* /output/"

                    # 3. 활성 색상 업데이트
                    echo "$TARGET_COLOR" > /home/ubuntu/admin-frontend/active_color

                    # 4. nginx.conf 업데이트 (admin root 반영)
                    echo "Updating nginx config with admin path..."
                    FRONT_COLOR=$(cat /home/ubuntu/frontend/active_color 2>/dev/null || echo "blue")

                    cp nginx/default.conf /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__FRONT_ROOT__|/home/ubuntu/frontend/dist-$FRONT_COLOR|g" /home/ubuntu/frontend/nginx.conf
                    sed -i "s|__ADMIN_ROOT__|/home/ubuntu/admin-frontend/dist-$TARGET_COLOR|g" /home/ubuntu/frontend/nginx.conf

                    # 5. nginx restart
                    cat /home/ubuntu/frontend/nginx.conf | docker exec -i ${NGINX_CONTAINER} sh -c "cat > /etc/nginx/conf.d/default.conf"
                    docker exec ${NGINX_CONTAINER} nginx -t
                    docker exec ${NGINX_CONTAINER} nginx -s reload

                    echo "=== Admin Frontend deployed to $TARGET_COLOR ==="
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Deployment successful!"
        }
        failure {
            echo "❌ Deployment failed!"
        }
        always {
            echo "Pipeline finished."
        }
    }
}