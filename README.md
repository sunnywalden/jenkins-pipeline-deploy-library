
# describe 简介

this jenkins library can be used for your app deploy via pipeline, the main sections included:

自定义的Jenkins shared library，用于编译并部署应用到Docker环境：

![image](https://github.com/sunnywalden/jenkins-pipeline-deploy-library/raw/master/img/flow.png)


# deploy 部署

    1. addd this library as your global pipeline by config your jenkins in configure 修改Jenkins配置，将本库添加为全局的pipeline
    
    2. add jenkinfile in your project like below:  在应用代码项目中定义jenkinfile，引用本库
    
```
#!groovy

// 在多分支构建下，严格规定Jenkinsfile只存在可以发版的分支上

// 引用在jenkins已经全局定义好的library
@Library('deploy-pipeline-library')

def map = [:]

// 远程管理节点地址（用于执行发版）
map.put('REMOTE_USER','root')
map.put('REMOTE_HOST','192.168.1.1')
map.put('REMOTE_SUDO_PASSWORD','password')
map.put('REMOTE_PORT',22)
// 项目gitlab代码地址
map.put('REPO_URL','your repo url')
// 分支名称
map.put('BRANCH_NAME','renew')
// 服务名称
map.put('APP_NAME','ops-backend')
map.put('CREDENTIALS_ID', 'artifactory')

// 构建配置
// 构建参数, 取值：npm 、 maven、 python2、 python3 or none
map.put('BUILD_TYPE', 'python3')
map.put('BUILD_ARGS', '')
// python -m py_compile sources/add2vals.py sources/calc.py
// npm install
// mvn -B -DskipTests clean package
map.put('BUILD_CMD', 'python -m py_compile account/views/user_authentication.py')

// docker config
map.put('REGISTRY_URL', 'registry-url')
map.put('MEMORY_LIMIT', '2048M')
map.put('PORTS', '8000:8000')
map.put('REPLICATES', 1)
map.put('HEALTH_CHECK', 'curl http://127.0.0.1:8000')
map.put('NETWORK', 'tezign')
map.put('VOLUMES', '/data/ops_backend/log:/app/logs')
map.put('ENVS', "APP=ops-backend")
map.put('tag', "dev")

// deploy config
map.put('APP_NAME', 'ops-backend')
map.put('ENV_TYPE', 'dev')
map.put('BUILD_ID', 'dev')
map.put('SEND_FILES', 'Dockerfile:/tmp')

// 调用library中var目录下的build.groovy脚本
deploy("python3", map)
```
    
