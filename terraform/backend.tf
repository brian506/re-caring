terraform {
	required_version = ">= 1.0"

	required_providers {
		aws = {
			source = "hashicorp/aws"
			version = "~> 5.0"
			}
	}


	backend "s3" {
		bucket = "recaring-terraform-state"
		key = "recaring/dev/terraform.tfstate"
		region = "ap-northeast-2"
		encrypt = true
	}
}