#!groovy

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = "${REMOTE_USER}"
    remote.host = "${REMOTE_HOST}"
    remote.port = "${REMOTE_PORT}"
    remote.identityFile = '/root/.ssh/id_rsa'
    remote.allowAnyHosts = true
    return remote
}

def call(Map map) {

    pipeline {
        // agent any
        agent {
            when {
                    BUILD_TYPE "maven"
            }
            docker {
                image "maven:3-alpine"
                args "${map.BUILD_ARGS}"
            }
            when {
                    BUILD_TYPE "npm"
            }
            docker {
                image 'node:6-alpine'
                args "${map.BUILD_ARGS}"
            }
            when {
                    BUILD_TYPE "none"
            }
            any
        }

        environment {
            // Ansible host
            REMOTE_HOST = "${map.REMOTE_HOST}"
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

            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}" + ":${TAG}}|${env.BUILD_ID}"
//             STACK_FILE_NAME = "docker-stack-" + "${map.APP_NAME}" + "${map.ENV_TYPE}" + ".yml"
            STACK_FILE_NAME = "docker-stack.yml"
        }

        // cron for pipe
//         triggers {
//         cron('H */4 * * 1-5')
//         }

        stages {
            stage('获取代码') {

            // 多分支pipe构建
//                 parallel {
//                     stage('Branch A') {
//                         agent {
//                             label "for-branch-a"
//                         }
//                         steps {
//                             echo "On Branch A"
//                         }
//                     }
//                     stage('Branch B') {
//                         agent {
//                             label "for-branch-b"
//                         }
//                         steps {
//                             echo "On Branch B"
//                         }
//                     }
//                 }

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
                post {
                    changed | success {
                        environment {
                            code_res = 1
                        }
                    }
                    unstable | fixed {
                        environment {
                            code_res = 0
                        }
                    }
                    failure {
                        environment {
                            code_res = -1
                        }
                    }
                }
            }

            stage('编译代码') {
                when {
                    BUILD_TYPE "npm" | "maven"
                }
                steps {
                    sh "${BUILD_CMD}"
                    post {
                        changed | success {
                            environment {
                                build_res = 1
                            }
                        }
                        unstable | fixed {
                            environment {
                                build_res = 0
                            }
                        }
                        failure {
                            environment {
                                build_res = -1
                            }
                        }
                    }
                }
//                 when {
//                     BUILD_TYPE "maven"
//                 }
//                 steps {
//                     withMaven(maven: 'maven 3.6') {
//                                 sh "mvn -U -am clean package -DskipTests"
//                     }
//                 }
//                 when {
//                     BUILD_TYPE "npm"
//                 }
//                 steps {
//
//                 }
            }

            stage('构建镜像') {
                steps {
                    sh docker build -t "${IMAGE_NAME}" .
                }
                post {
                    changed | success {
                        environment {
                            docker_build_res = 1
                        }
                    }
                    unstable | fixed {
                        environment {
                            docker_build__res = 0
                        }
                    }
                    failure {
                        environment {
                            docker_build__res = -1
                        }
                    }
                }
//                 steps {
//                     sh "wget -O build.sh https://git.x-vipay.com/docker/jenkins-pipeline-library/raw/master/resources/shell/build.sh"
//                     sh "sh build.sh ${BRANCH_NAME} "
//                 }
            }

            stage('init-server') {
                steps {
                    script {
                        server = getServer()
                    }
                }
            }

            stage('执行发版') {
                steps {
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " https://git.x-vipay.com/docker/jenkins-pipeline-library/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c ${STACK_FILE_NAME} ${APP_NAME}"
                    sshScript remote: server, script: "deploy.sh"
                }
            }
        }
    }
}