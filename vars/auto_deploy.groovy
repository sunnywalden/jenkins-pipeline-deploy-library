#!groovy

import groovy.json.JsonSlurper

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = "${REMOTE_USER}"
    remote.host = "${REMOTE_HOST}"
    remote.password = "${REMOTE_SUDO_PASSWORD}"
    remote.port = "${REMOTE_PORT}".toInteger()
    remote.identityFile = '/root/.ssh/id_rsa'
    remote.allowAnyHosts = true
    return remote
}

def send_all(file_str) {

    files_list = file_str.split(',')
    files_list.each { item ->
            echo "print file object to be send: ${item}"
            files = item.split(':')
            if (files.size() == 2) {
                source_file = files[0]
                dest_file = files[1]
            } else {
                return 0
            }
            echo "send file ${source_file} to ${dest_file}"
            sshPut remote: remote, from: "${source_file}", into: "${dest_file}"
    }
}

def call(String type, Map map) {
    if ( type == "maven" ) {
        pipeline {
        agent {

            docker {
                image 'maven:3-alpine'
                    args "${map.BUILD_ARGS}"
            }
        }

        environment {
            // Ansible host
            REMOTE_HOST = "${map.REMOTE_HOST}"
            REMOTE_USER = "${map.REMOTE_USER}"
            REMOTE_PORT = "${map.REMOTE_PORT}"
            REMOTE_SUDO_PASSWORD = "${map.REMOTE_SUDO_PASSWORD}"
            //  git config
            REPO_URL = "${map.REPO_URL}"
            BRANCH_NAME = "${map.BRANCH_NAME}"
            CREDENTIALS_ID = 'artifactory'
            TAG = "${map.tag}"

            // env type
            ENV_TYPE = "${map.ENV_TYPE}"

            // build config
            // values in: npm maven or none
            BUILD_TYPE = "${map.BUILD_TYPE}"
            BUILD_ARGS = "${map.BUILD_ARGS}"
            BUILD_CMD = "${map.BUILD_CMD}"


            // docker config
            REGISTRY_URL = "${map.REGISTRY_URL}"
            MEMORY_LIMIT = "${map.MEMORY_LIMIT}"
            PORTS = "${map.PORTS}"
            REPLICATES = "${map.REPLICATES}"
            HEALTH_CHECK = "${map.HEALTH_CHECK}"
            NETWORK = "${map.NETWORK}"
            VOLUMES = "${map.VOLUMES}"
            ENVS = "${map.ENVS}"

            // deploy config
            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
            STACK_FILE_NAME = 'docker-stack.yml'
            SEND_FILES = "${map.SEND_FILES}"
        }

        // cron for pipe
//         triggers {
//         cron('H */4 * * 1-5')
//         }

        stages {
            stage('获取代码') {

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }

            stage('编译代码') {
                steps {
                    sh 'echo `pwd`'
                    sh "${BUILD_CMD}"
                }
            }

            stage('构建镜像') {
                steps {
                    sh "docker build -t ${IMAGE_NAME}:${env.BUILD_ID} ."
                    sh "docker push ${IMAGE_NAME}:${env.BUILD_ID}"
                }
            }

            stage('获取主机') {
                steps {
                    script {
                        remote = getServer()
                    }
                }
            }

            stage('更新') {
                when {
                    expression {
                        SEND_FILES != []
                    }
                }
                steps {
                    echo "print all files objects: ${SEND_FILES}"
                    // send files
                    send_all("${SEND_FILES}")
                }
            }

            stage('发布') {
                steps {
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"

                    // deploy
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshScript remote: remote, script: "deploy.sh"
                }
            }
        }
        post {
            always {
                echo 'Deploy pipeline finished'
            }
            failure {
                mail to: 'zhangbo@tezign.com', subject: 'The Pipeline failed', body: '''
                    Deploy pipeline is failed
                '''
            }
        }
    }
    }
    else if ( type == "npm" ) {
            pipeline {
//         agent none
//         agent {
//             label 'master'
//         }
        agent {

            docker {
                image 'node:6-alpine'
                    args "${map.BUILD_ARGS}"
            }
        }

        environment {
            // Ansible host
            REMOTE_HOST = "${map.REMOTE_HOST}"
            REMOTE_USER = "${map.REMOTE_USER}"
            REMOTE_PORT = "${map.REMOTE_PORT}"
            REMOTE_SUDO_PASSWORD = "${map.REMOTE_SUDO_PASSWORD}"
            //  git config
            REPO_URL = "${map.REPO_URL}"
            BRANCH_NAME = "${map.BRANCH_NAME}"
            CREDENTIALS_ID = 'artifactory'
            TAG = "${map.tag}"

            // env type
            ENV_TYPE = "${map.ENV_TYPE}"

            // build config
            // values in: npm maven or none
            BUILD_TYPE = "${map.BUILD_TYPE}"
            BUILD_ARGS = "${map.BUILD_ARGS}"
            BUILD_CMD = "${map.BUILD_CMD}"


            // docker config
            REGISTRY_URL = "${map.REGISTRY_URL}"
            MEMORY_LIMIT = "${map.MEMORY_LIMIT}"
            PORTS = "${map.PORTS}"
            REPLICATES = "${map.REPLICATES}"
            HEALTH_CHECK = "${map.HEALTH_CHECK}"
            NETWORK = "${map.NETWORK}"
            VOLUMES = "${map.VOLUMES}"
            ENVS = "${map.ENVS}"

            // deploy config
            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
            STACK_FILE_NAME = 'docker-stack.yml'
            SEND_FILES = "${map.SEND_FILES}"
        }

        // cron for pipe
//         triggers {
//         cron('H */4 * * 1-5')
//         }

        stages {
            stage('获取代码') {

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }

            stage('编译代码') {
                steps {
                    sh 'echo `pwd`'
                    sh "${BUILD_CMD}"
                }
            }

            stage('构建镜像') {
                steps {
                    sh "docker build -t ${IMAGE_NAME}:${env.BUILD_ID} ."
                    sh "docker push ${IMAGE_NAME}:${env.BUILD_ID}"
                }
            }

            stage('获取主机') {
                steps {
                    script {
                        remote = getServer()
                    }
                }
            }

            stage('更新') {
                when {
                    expression {
                        SEND_FILES != []
                    }
                }
                steps {
                    echo "print all files objects: ${SEND_FILES}"
                    // send files
                    send_all("${SEND_FILES}")
                }
            }

            stage('发布') {
                steps {
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"

                    // deploy
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshScript remote: remote, script: "deploy.sh"
                }
            }
        }
        post {
            always {
                echo 'Deploy pipeline finished'
            }
            failure {
                mail to: 'zhangbo@tezign.com', subject: 'The Pipeline failed', body: '''
                    Deploy pipeline is failed
                '''
            }
        }
    }
    }
    else {
    pipeline {
        agent {

            docker {
//                 image 'python:3-alpine'
                image 'docker:stable-git'
                args "${map.BUILD_ARGS}"
            }
        }

        environment {
            // Ansible host
            REMOTE_HOST = "${map.REMOTE_HOST}"
            REMOTE_USER = "${map.REMOTE_USER}"
            REMOTE_PORT = "${map.REMOTE_PORT}"
            REMOTE_SUDO_PASSWORD = "${map.REMOTE_SUDO_PASSWORD}"
            //  git config
            REPO_URL = "${map.REPO_URL}"
            BRANCH_NAME = "${map.BRANCH_NAME}"
            CREDENTIALS_ID = 'artifactory'
            TAG = "${map.tag}"

            // env type
            ENV_TYPE = "${map.ENV_TYPE}"

            // build config
            // values in: npm maven or none
            BUILD_TYPE = "${map.BUILD_TYPE}"
            BUILD_ARGS = "${map.BUILD_ARGS}"
            BUILD_CMD = "${map.BUILD_CMD}"


            // docker config
            REGISTRY_URL = "${map.REGISTRY_URL}"
            MEMORY_LIMIT = "${map.MEMORY_LIMIT}"
            PORTS = "${map.PORTS}"
            REPLICATES = "${map.REPLICATES}"
            HEALTH_CHECK = "${map.HEALTH_CHECK}"
            NETWORK = "${map.NETWORK}"
            VOLUMES = "${map.VOLUMES}"
            ENVS = "${map.ENVS}"

            // deploy config
            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
            STACK_FILE_NAME = 'docker-stack.yml'
            SEND_FILES = "${map.SEND_FILES}"
        }

        // cron for pipe
//         triggers {
//         cron('H */4 * * 1-5')
//         }

        stages {
            stage('获取代码') {

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }

            stage('编译代码') {
                steps {
                    sh 'echo `pwd`'
                    sh "${BUILD_CMD}"
                }
            }

            stage('构建镜像') {
                steps {
                    sh "docker build -t ${IMAGE_NAME}:${env.BUILD_ID} ."
                    sh "docker push ${IMAGE_NAME}:${env.BUILD_ID}"
                }
            }

            stage('获取主机') {
                steps {
                    script {
                        remote = getServer()
                    }
                }
            }

            stage('更新') {
                when {
                    expression {
                        SEND_FILES != []
                    }
                }
                steps {
                    echo "print all files objects: ${SEND_FILES}"
                    // send files
                    send_all("${SEND_FILES}")
                }
            }

            stage('发布') {
                steps {
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"

                    // deploy
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
                    sshScript remote: remote, script: "deploy.sh"
                }
            }
        }
        post {
            always {
                echo 'Deploy pipeline finished'
            }
            failure {
                mail to: 'zhangbo@tezign.com', subject: 'The Pipeline failed', body: '''
                    Deploy pipeline is failed
                '''
            }
        }
    }
    }
}