variable "aws_region" {
  description = "AWS 리전"
  type        = string
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "environment" {
  description = "배포 환경"
  type        = string
}

# EC2
variable "key_name" {
  description = "EC2 접속용 키페어 이름"
  type        = string
}

variable "app_instance_type" {
  description = "앱 서버 EC2 인스턴스 타입"
  type        = string
}



# RDS
variable "db_instance_class" {
    description = "RDS 인스턴스"
    type = string
    default = "db.t3.micro"
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
}

variable "db_username" {
  description = "데이터베이스 유저명"
  type        = string
}

variable "db_password" {
  description = "데이터베이스 비밀번호"
  type        = string
  sensitive   = true  # plan/apply 출력에서 값이 가려짐
}