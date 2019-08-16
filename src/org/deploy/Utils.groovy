#!groovy

class ComFunc {
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
}