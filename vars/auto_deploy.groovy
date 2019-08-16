#!groovy

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = "${REMOTE_USER}"
    remote.host = "${REMOTE_HOST}"
    remote.password = "${REMOTE_SUDO_PASSWORD}"
    remote.port = "${REMOTE_PORT}"
    remote.identityFile = '/root/.ssh/id_rsa'
    remote.allowAnyHosts = true
    return remote
}

def send_all(file_str) {
    script {
        files_list = file_str.split(',')
    }
    files_list.each { item ->
//         environment {
        script {
            echo "print file object to be send: ${item}"
            files = item.split(':')
            source_file = ${files}.split(":")[0]
            dest_file = ${files}.split(":")[1]
        }
        echo "send file ${source_file} to ${dest_file}"
//         echo "${source_file} ${env.source_file}"
        sshPut remote: remote, from: "${source_file}", into: "${dest_file}"
    }
}

def call(Map map) {

    pipeline {
//         agent none
        agent {
            label 'master'
        }
//         agent {
//             when {
//                     BUILD_TYPE "maven"
//             }
//             docker {
//                 image "maven:3-alpine"
//                 args "${map.BUILD_ARGS}"
//             }
//             when {
//                     BUILD_TYPE "npm"
//             }
//             docker {
//                 image 'node:6-alpine'
//                 args "${map.BUILD_ARGS}"
//             }
//             when {
//                     BUILD_TYPE "python"
//             }
//             docker {
//                 image 'python:2-alpine'
//                 // sh 'python -m py_compile sources/add2vals.py sources/calc.py'
//                 args "${map.BUILD_ARGS}"
//             }
//             when {
//                     BUILD_TYPE "python3"
//             }
//             docker {
//                 image 'python:3-alpine'
//                 // sh 'python -m py_compile sources/add2vals.py sources/calc.py'
//                 args "${map.BUILD_ARGS}"
//             }
//             when {
//                     BUILD_TYPE "none"
//             }
//             any
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
            REPLICATES = "${map.REPLICATES}"
            HEALTH_CHECK = "${map.HEALTH_CHECK}"
            NETWORK = "${map.NETWORK}"
            VOLUMES = "${map.VOLUMES}"
            ENVS = "${map.ENVS}"

            // deploy config
            APP_NAME = "${map.APP_NAME}"
            IMAGE_NAME = "${REGISTRY_URL}/" + "${map.APP_NAME}" + "_${map.ENV_TYPE}"
//             STACK_FILE_NAME = "docker-stack-" + "${map.APP_NAME}" + "${map.ENV_TYPE}" + ".yml"
            STACK_FILE_NAME = 'docker-stack.yml'
            SEND_FILES = "${map.SEND_FILES}"
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
//                 agent {
//                     docker {
//                         image 'python:3-alpine'
//                         args "${map.BUILD_ARGS}"
//                     }
//                 }
//                 when {
//                     beforeAgent true
//                     environment name: 'BUILD_TYPE', value: 'python3'
//                 }

                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }

            stage('编译代码') {
//                 agent {
//
//                     docker {
//                         image 'python:3-alpine'
//                         args "${map.BUILD_ARGS}"
//                     }
//                 }
//                 when {
//                     beforeAgent true
//                     environment name: 'BUILD_TYPE', value: 'python3'
//                 }
//                 when {
//                     expression {
//                         BUILD_TYPE == "npm" || BUILD_TYPE == "maven" || BUILD_TYPE == "python2" ||  BUILD_TYPE == "python3"
//                     }
//                 }
                steps {
                    sh 'echo `pwd`'
                    sh "${BUILD_CMD}"
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
//                 agent {
//
//                     docker {
//                         image 'python:3-alpine'
//                         args "${map.BUILD_ARGS}"
//                     }
//                 }
//                 when {
//                     beforeAgent true
//                     environment name: 'BUILD_TYPE', value: 'python3'
//                 }
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
//                     script {
//                         files = SEND_FILES.split(',')
//                     }
//                     echo "print objects list: ${files}"
                    // send files
                    send_all("${SEND_FILES}")
                }
            }

            stage('发布') {
                steps {
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " http://git.tezign.com/ops/jenkins-script.git/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c ${STACK_FILE_NAME} ${APP_NAME}"

                    // deploy
                    sshScript remote: remote, script: "deploy.sh"
                }
            }
        }
//         post {
//             success {
//                 environment {
//                             docker_build_res = 1
//                         }
//                 }
//             unstable {
//                 environment {
//                             docker_build__res = 0
//                 }
//             }
//             failure {
//                 environment {
//                             docker_build__res = -1
//                 }
//             }
//         }
    }
}