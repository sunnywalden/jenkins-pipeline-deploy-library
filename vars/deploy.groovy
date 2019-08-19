#!groovy

import groovy.json.JsonSlurper
// import src.org.deploy.Utils.ComFunc
// import src.org.deploy.Compose.DockerCompose

def generate_compose() {
        filePath = "/tmp/docker-stack.yml"
        File file = new File(filePath)
        file.delete()
        file <<
"""
version: '3.4'
services:
    "${APP_NAME}":
         image: "${IMAGE_NAME}:${env.BUILD_ID}"
         ports: "${PORTS_LIST}"
         environment: "${ENVS_LIST}"
         networks:
           - "${NETWORK}"
         volumes: "${VOLUMES_LIST}"
         stop_grace_period: 30s # Specify how long to wait when attempting to stop a container if it doesn’t handle SIGTERM
         deploy:
           replicas: "${REPLICATES}".toInteger()
           resources:
             limits:
               memory: "${MEMORY_LIMIT}"
           update_config:
             parallelism: 1            # The number of containers to update at a time.
             delay: 0s                 # The time to wait between updating a group of containers.
             failure_action: rollback  # What to do if an update fails  One of continue, rollback, or pause (default: pause).
             max_failure_ratio: 0      # Failure rate to tolerate during an update.
             order: start-first
           restart_policy:
             condition: any
             max_attempts: 3
         healthcheck:
           test: "${HEALTH_CHECK}"
           interval: 3s
           timeout: 5s
           retries: 3
           start_period: 2m
networks:
  tezign:
    external: true
"""
    println file.text
}


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

def str_to_list(str) {
    return list = str.split(',')
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
//     if ( type == "maven" ) {
//         pipeline {
//         agent {
//
//             docker {
//                 image 'maven:3-alpine'
//                     args "${map.BUILD_ARGS}"
//             }
//         }
//
//         environment {
//             // Ansible host
//             REMOTE_HOST = "${map.REMOTE_HOST}"
//             REMOTE_USER = "${map.REMOTE_USER}"
//             REMOTE_PORT = "${map.REMOTE_PORT}"
//             REMOTE_SUDO_PASSWORD = "${map.REMOTE_SUDO_PASSWORD}"
//             //  git config
//             REPO_URL = "${map.REPO_URL}"
//             BRANCH_NAME = "${map.BRANCH_NAME}"
//             CREDENTIALS_ID = 'artifactory'
//             TAG = "${map.tag}"
//
//             // env type
//             ENV_TYPE = "${map.ENV_TYPE}"
//
//             // build config
//             // values in: npm maven or none
//             BUILD_TYPE = "${map.BUILD_TYPE}"
//             BUILD_ARGS = "${map.BUILD_ARGS}"
//             BUILD_CMD = "${map.BUILD_CMD}"
//
//
//             // docker config
//             REGISTRY_URL = "${map.REGISTRY_URL}"
//             MEMORY_LIMIT = "${map.MEMORY_LIMIT}"
//             PORTS = "${map.PORTS}"
//             REPLICATES = "${map.REPLICATES}"
//             HEALTH_CHECK = "${map.HEALTH_CHECK}"
//             NETWORK = "${map.NETWORK}"
//             VOLUMES = "${map.VOLUMES}"
//             ENVS = "${map.ENVS}"
//
//             // deploy config
//             APP_NAME = "${map.APP_NAME}"
//             IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
//             STACK_FILE_NAME = 'docker-stack.yml'
//             SEND_FILES = "${map.SEND_FILES}"
//         }
//
//         // cron for pipe
// //         triggers {
// //         cron('H */4 * * 1-5')
// //         }
//
//         stages {
//             stage('获取代码') {
//
//                 steps {
//                     git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
//                 }
//             }
//
//             stage('编译代码') {
//                 steps {
//                     sh 'echo `pwd`'
//                     sh "${BUILD_CMD}"
//                 }
//             }
//
//             stage('构建镜像') {
//                 steps {
//                     sh "docker build -t ${IMAGE_NAME}:${env.BUILD_ID} ."
//                     sh "docker push ${IMAGE_NAME}:${env.BUILD_ID}"
//                 }
//             }
//
//             stage('获取主机') {
//                 steps {
//                     script {
//                         remote = getServer()
//                     }
//                 }
//             }
//
//             stage('更新') {
//                 when {
//                     expression {
//                         SEND_FILES != []
//                     }
//                 }
//                 steps {
//                     echo "print all files objects: ${SEND_FILES}"
//                     // send files
//                     send_all("${SEND_FILES}")
//                 }
//             }
//
//             stage('发布') {
//                 steps {
//                     // generate deploy script
//                     writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
//                         " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
//                         "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"
//
//                     // deploy
//                     sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
//                     sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
//                     sshScript remote: remote, script: "deploy.sh"
//                 }
//             }
//         }
//         post {
//             always {
//                 echo 'Deploy pipeline finished'
//             }
//             failure {
//                 mail to: 'zhangbo@tezign.com', subject: 'The Pipeline failed', body: '''
//                     Deploy pipeline is failed
//                 '''
//             }
//         }
//     }
//     }
//     else if ( type == "npm" ) {
//             pipeline {
// //         agent none
//         agent {
//             label 'master'
//         }
// //         agent {
// //
// //             docker {
// //                 image 'node:6-alpine'
// //                     args "${map.BUILD_ARGS}"
// //             }
// //         }
//
//         environment {
//             // Ansible host
//             REMOTE_HOST = "${map.REMOTE_HOST}"
//             REMOTE_USER = "${map.REMOTE_USER}"
//             REMOTE_PORT = "${map.REMOTE_PORT}"
//             REMOTE_SUDO_PASSWORD = "${map.REMOTE_SUDO_PASSWORD}"
//             //  git config
//             REPO_URL = "${map.REPO_URL}"
//             BRANCH_NAME = "${map.BRANCH_NAME}"
//             CREDENTIALS_ID = 'artifactory'
//             TAG = "${map.tag}"
//
//             // env type
//             ENV_TYPE = "${map.ENV_TYPE}"
//
//             // build config
//             // values in: npm maven or none
//             BUILD_TYPE = "${map.BUILD_TYPE}"
//             BUILD_ARGS = "${map.BUILD_ARGS}"
//             BUILD_CMD = "${map.BUILD_CMD}"
//
//
//             // docker config
//             REGISTRY_URL = "${map.REGISTRY_URL}"
//             MEMORY_LIMIT = "${map.MEMORY_LIMIT}"
//             PORTS = "${map.PORTS}"
//             REPLICATES = "${map.REPLICATES}"
//             HEALTH_CHECK = "${map.HEALTH_CHECK}"
//             NETWORK = "${map.NETWORK}"
//             VOLUMES = "${map.VOLUMES}"
//             ENVS = "${map.ENVS}"
//
//             // deploy config
//             APP_NAME = "${map.APP_NAME}"
//             IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
//             STACK_FILE_NAME = 'docker-stack.yml'
//             SEND_FILES = "${map.SEND_FILES}"
//         }
//
//         // cron for pipe
// //         triggers {
// //         cron('H */4 * * 1-5')
// //         }
//
//         stages {
//             stage('获取代码') {
//
//                 steps {
//                     git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
//                 }
//             }
//
//             stage('编译代码') {
//                 steps {
//                     sh 'echo `pwd`'
//                     sh "${BUILD_CMD}"
//                 }
//             }
//
//             stage('构建镜像') {
//                 steps {
//                     sh "docker build -t ${IMAGE_NAME}:${env.BUILD_ID} ."
//                     sh "docker push ${IMAGE_NAME}:${env.BUILD_ID}"
//                 }
//             }
//
//             stage('获取主机') {
//                 steps {
//                     script {
//                         remote = getServer()
//                     }
//                 }
//             }
//
//             stage('更新') {
//                 when {
//                     expression {
//                         SEND_FILES != []
//                     }
//                 }
//                 steps {
//                     echo "print all files objects: ${SEND_FILES}"
//                     // send files
//                     send_all("${SEND_FILES}")
//                 }
//             }
//
//             stage('发布') {
//                 steps {
//                     // generate deploy script
//                     writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
//                         " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
//                         "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"
//
//                     // deploy
//                     sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
//                     sshPut remote: remote, from: "${STACK_FILE_NAME}", into: "/tmp"
//                     sshScript remote: remote, script: "deploy.sh"
//                 }
//             }
//         }
//         post {
//             always {
//                 echo 'Deploy pipeline finished'
//             }
//             failure {
//                 mail to: 'zhangbo@tezign.com', subject: 'The Pipeline failed', body: '''
//                     Deploy pipeline is failed
//                 '''
//             }
//         }
//     }
//     }
//     else {
    pipeline {
        agent {
            label "master"
        }
//         agent {
//
//             docker {
//                 image 'docker'
//                 args "${map.BUILD_ARGS}"
//             }
//         }

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
            REPLICATES = "${map.REPLICATES}".toInteger()
            HEALTH_CHECK = "${map.HEALTH_CHECK}"
            NETWORK = "${map.NETWORK}"
            VOLUMES = "${map.VOLUMES}"
            ENVS = "${map.ENVS}"

            // deploy config
            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
            STACK_FILE_NAME = 'docker-stack.yml'
            SEND_FILES = "${map.SEND_FILES}"

            ENVS_LIST = str_to_list("${ENVS}")
            PORTS_LIST = str_to_list("${PORTS}")
            VOLUMES_LIST = str_to_list("${VOLUMES}")
        }

        stages {
            stage('获取代码') {

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }

            stage('编译代码') {
                steps {
                    echo "Ignore"
//                     sh "${BUILD_CMD}"
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
//                     script {
//                         envs_list = str_to_list("${ENVS}")
//                         ports_list = str_to_list("${PORTS}")
//                         volumes_list = str_to_list("${VOLUMES}")
//                     }
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " https://git.tezign.com/ops/jenkins-script/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c /tmp/${STACK_FILE_NAME} ${APP_NAME}"

                    // deploy
                    generate_compose()
                    sshPut remote: remote, from: "/tmp/docker-stack.yml", into: "/tmp/docker-stack.yml"
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
//     }
}