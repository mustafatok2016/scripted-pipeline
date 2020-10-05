properties([
    parameters([
        booleanParam(defaultValue: true, description: 'Do you want to run terrform apply', name: 'terraform_apply'),
        booleanParam(defaultValue: false, description: 'Do you want to run terrform destroy', name: 'terraform_destroy'),
        choice(choices: ['dev', 'qa', 'prod'], description: 'Choose Environment', name: 'environment'),
        string(defaultValue: '', description: 'Provide AMI Name', name: 'ami_name', trim: true)
    ])
])

def aws_region_var = ''

if(params.environment == "dev") {
    println("Applying Dev")
    aws_region_var = "us-east-1"
}
else if(params.environment == "qa") {
    println("Applying QA")
    aws_region_var = "us-east-2"
}
else if(params.environment == "prod") {
    println("Applying Prod")
    aws_region_var = "us-west-2"
}

def tfvar = """
    s3_bucket = \"jenkins-terraform-evolvecybertraining\"
    s3_folder_project = \"terraform_ec2\"
    s3_folder_region = \"us-east-1\"
    s3_folder_type = \"class\"
    s3_tfstate_file = \"infrastructure.tfstate\"
    environment = \"${params.environment}\"

    region   = \"${aws_region_var}\"
    az1      = \"${aws_region_var}a\"
    az2      = \"${aws_region_var}b\"
    az3      = \"${aws_region_var}c\"

    vpc_cidr_block  = \"172.32.0.0/16\"

    public_cidr1    = \"172.32.1.0/24\"
    public_cidr2    = \"172.32.2.0/24\"
    public_cidr3    = \"172.32.3.0/24\"

    private_cidr1   = \"172.32.10.0/24\"
    private_cidr2   = \"172.32.11.0/24\"
    private_cidr3   = \"172.32.12.0/24\"

    public_key    = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCzm79oYWLjj0l8FBah58lwmIF5WeRjNtOq/IeeanU0kMNINwqANnTD580fg3I4vhsVEbLwVbBIQdJGcR0VHNldIhKpK7RWcLJOTWTptKnqSEzoW8VwkSYI0+CoUa+3n4pSGBMNNTGwixstyqaxSBClI+HJQDutPXQMUs+m1N3iLfjTUdCjMF8IKcNXkd/PG8nNxRwhx64hW1Tzgh8g+wGUGMIoqV3onQdPzFooMplZ/jZj8Ow54CA2TIPic50w9TDj2PV752DOmxzeDs6ZpV7kvv1jnijYd3pZNao0UvjhusGthrS6YQ5Dot7GcPEHGyP5OWkQUQFrggxD3wnx/RUJ root@Jenkins-Master"	
    ami_name        = "${params.ami_name}"
"""

node{
    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/ikambarov/terraform-ec2-by-ami-name.git'

        writeFile file: "${params.environment}.tfvars", text: "${tfvar}"
    }

    withCredentials([usernamePassword(credentialsId: 'aws_jenkins-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["AWS_REGION=${aws_region_var}"]) {
            stage("Terrraform Init"){
                sh """
                    bash setenv.sh ${environment}.tfvars
                    terraform init
                """
            }        
            
            if (params.terraform_apply) {
                stage("Terraform Apply"){
                    sh """
                        terraform apply -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else if (params.terraform_destroy) {
                stage("Terraform Destroy"){
                    sh """
                        terraform destroy -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else {
                stage("Terraform Plan"){
                    sh """
                        terraform plan -var-file ${environment}.tfvars
                    """
                }
            }
        }
    }    
}

