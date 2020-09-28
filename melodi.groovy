properties([
    parameters([
        string(defaultValue: '', description: 'Provide node IP', name: 'node', trim: true)
        ])
    ])

node {
    stage("Pull Repo"){
        sh "rm -rf ansible-melodi && git clone https://github.com/ikambarov/ansible-melodi.git"
    }
    stage("Install Melodi"){
        ansiblePlaybook become: true, colorized: true, credentialsId: 'jenkins-master', disableHostKeyChecking: true, inventory: "${params.node},", playbook: 'ansible-melodi/main.yml'
    }

    // withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-master', keyFileVariable: 'SSHKEY', passphraseVariable: '', usernameVariable: 'SSHUSERNAME')]) {
    //     stage("Install Melodi"){
    //         sh """
    //             export ANSIBLE_HOST_KEY_CHECKING=False
    //             ansible-playbook -i "${params.node}," --private-key $SSHKEY ansible-melodi/main.yml -b -u $SSHUSERNAME
    //         """
    //     }
    // }    
}