# 최신 Ubuntu 22.04 LTS AMI
data "aws_ami" "ubuntu" {
	most_recent = true
	owners = ["099720109477"]

	filter {
		name = "name"
		values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
	}

	filter {
		name = "virtualization-type"
		values = ["hvm"]
	}
}

resource "aws_instance" "recaring-app-server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.app_instance_type
  subnet_id     = aws_subnet.public.id
  key_name      = var.key_name

  vpc_security_group_ids = [aws_security_group.app_server.id]

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    set -e
    exec > /var/log/user-data.log 2>&1

    # 1. Docker 설치
    curl -fsSL https://get.docker.com | sh
    usermod -aG docker ubuntu
    apt-get install -y docker-compose-plugin

    # 2. 디렉토리 생성
    mkdir -p /home/ubuntu/recaring/nginx/conf.d
    mkdir -p /home/ubuntu/recaring/scripts
    mkdir -p /home/ubuntu/recaring/data/certbot/conf
    mkdir -p /home/ubuntu/recaring/data/certbot/www
    chown -R ubuntu:ubuntu /home/ubuntu/recaring

    # 3. 레포에서 파일 복사
    cd /tmp
    git clone https://github.com/brian506/re-caring.git
    cp re-caring/docker-compose-dev.yml /home/ubuntu/recaring/
    cp re-caring/nginx/conf.d/service-url.inc /home/ubuntu/recaring/nginx/conf.d/
    cp re-caring/scripts/deploy.sh /home/ubuntu/recaring/scripts/
    chmod +x /home/ubuntu/recaring/scripts/deploy.sh
    rm -rf /tmp/re-caring

    # 4. 초기 nginx 설정 (certbot HTTP 인증용)
    cat > /home/ubuntu/recaring/nginx/conf.d/myapp.conf << 'NGINX'
    server {
        listen 80;
        server_name re-caring.com;
        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }
        location / {
            return 200 'ok';
        }
    }
    NGINX

    # 5. nginx + certbot 컨테이너 시작
    cd /home/ubuntu/recaring
    docker compose -f docker-compose-dev.yml up -d nginx certbot
  EOF

  tags = {
    Name        = "${var.project_name}-app-server"
    Environment = var.environment
  }
}

# Elastic IP (기존 IP 재사용)
resource "aws_eip" "app_server" {
  instance = aws_instance.recaring-app-server.id
  domain   = "vpc"

  tags = {
    Name        = "${var.project_name}-eip"
    Environment = var.environment
  }
}

output "app_server_ip" {
  value       = aws_eip.app_server.public_ip
  description = "앱 서버 퍼블릭 IP (43.200.235.247)"
}