# Parameter Store에서 RDS 환경변수 조회
data "aws_ssm_parameter" "db_name" {
	name = "/${var.project_name}/DB_NAME"
	with_decryption = true
}

data "aws_ssm_parameter" "db_username" {
	name = "/${var.project_name}/DB_USERNAME"
	with_decryption = true
}

data "aws_ssm_parameter" "db_password" {
	name = "/${var.project_name}/DB_PASSWORD"
	with_decryption = true
}

# RDS 서브넷 그룹
resource "aws_db_subnet_group" "main" {
	name = "${var.project_name}-rds-subnet-group"
	subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_c.id]

	tags = {
		Name = "${var.project_name}-rds-subnet-group"
		Environment = var.environment
	}
}

resource "aws_db_instance" "postgres" {
	identifier = "${var.project_name}-db"

	engine = "postgres"
	engine_version = "17"
	instance_class = "db.t4g.micro"

	allocated_storage = 20
	max_allocated_storage = 100
	storage_type = "gp3"
	storage_encrypted = true

	db_name = data.aws_ssm_parameter.db_name.value
	username = data.aws_ssm_parameter.db_username.value
	password = data.aws_ssm_parameter.db_password.value

	vpc_security_group_ids = [aws_security_group.rds.id]
	db_subnet_group_name = aws_db_subnet_group.main.name

	publicly_accessible = false
	skip_final_snapshot = false
	deletion_protection = true

	backup_retention_period = 0
	backup_window = "03:00-04:00"
	maintenance_window = "Mon:04:00-Mon:05:00"

	tags = {
		Name = "${var.project_name}-db"
		Environment = var.environment
	}
}