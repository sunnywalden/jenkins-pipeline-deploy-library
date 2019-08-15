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

@NonCPS // has to be NonCPS or the build breaks on the call to .each
def send_all(list) {
    list.each { item ->
        environment {
            source_file = item.split(':')[0]
            dest_file = item.split(':')[1]
        }
        echo "send file ${env.source_file} to ${env.dest_file} now"
        sshPut remote: remote, from: "${env.source_file}", into: "${env.dest_file}"
    }
}

def call(Map map) {
    pipeline {
        agent {
            label 'master'
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
        stages {
            stage('获取代码') {
                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}", credentialsId: "${CREDENTIALS_ID}"])
                }
            }
            stage('编译代码') {
                steps {
                    sh 'echo `pwd`'
                    sh "${BUILD_CMD}"                     }
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
            stage('执行发版') {
                steps {
                    // send files
                    send_all("${SEND_FILES}")
                    // generate deploy script
                    writeFile file: 'deploy.sh', text: "wget -O ${STACK_FILE_NAME} " +
                        " http://git.tezign.com/ops/jenkins-script.git/raw/master/resources/docker-compose/${STACK_FILE_NAME} \n" +
                        "sudo docker stack deploy -c ${STACK_FILE_NAME} ${APP_NAME}"
                    // deploy
                    sshScript remote: remote, script: "deploy.sh"
                }
            }
        }
    }
