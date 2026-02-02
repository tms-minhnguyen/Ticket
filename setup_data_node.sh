#!/bin/bash
# setup_data_node.sh
# Dùng cho Data Node (Chạy 1 lần duy nhất)

# 1. System Setup
sudo yum update -y
sudo yum install -y git htop
sudo amazon-linux-extras install docker -y || sudo dnf install -y docker
sudo service docker start
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# Install Docker Compose
ARCH=$(uname -m)
if [ "$ARCH" == "aarch64" ]; then
  DOCKER_ARCH="linux-aarch64"
else
  DOCKER_ARCH="linux-x86_64"
fi
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-${DOCKER_ARCH}" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 2. OS Tuning
echo "* soft nofile 100000" | sudo tee -a /etc/security/limits.conf
echo "* hard nofile 100000" | sudo tee -a /etc/security/limits.conf

# 3. Pull Code
cd /home/ec2-user
# !!! Sửa URL repo của bạn !!!
git clone https://github.com/your-username/ticket-system.git
cd ticket-system/ticket # Vào thư mục ticket (nơi chứa file sh và yml)

# Fix ownership
sudo chown -R ec2-user:ec2-user /home/ec2-user/ticket-system

# 4. Set Password Env
echo "DB_PASSWORD=stress_test_password" > .env

# 5. Start Data Services
sudo docker-compose -f docker-compose.data.yml up -d
