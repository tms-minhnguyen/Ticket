#!/bin/bash
# setup_asg_template.sh
# Dùng cho Launch Template (App Tier)

# === CONFIGURATION ===
DATA_NODE_IP="172.31.xx.xx"
DB_PASSWORD="stress_test_password"
# =====================

# 1. System Setup
sudo yum update -y
sudo yum install -y git htop
sudo amazon-linux-extras install docker -y || sudo dnf install -y docker
sudo service docker start
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 2. OS Tuning
echo "* soft nofile 100000" | sudo tee -a /etc/security/limits.conf
echo "* hard nofile 100000" | sudo tee -a /etc/security/limits.conf
sudo sysctl -w net.core.somaxconn=65535

# 3. Pull Application
cd /home/ec2-user
git clone https://github.com/your-username/ticket-system.git
cd ticket-system/ticket # Vào thư mục ticket

# Fix ownership
sudo chown -R ec2-user:ec2-user /home/ec2-user/ticket-system

# 4. Create Env File pointing to Data Node
echo "DB_HOST=${DATA_NODE_IP}" > .env
echo "REDIS_HOST=${DATA_NODE_IP}" >> .env
echo "DB_PASSWORD=${DB_PASSWORD}" >> .env
echo "VNPAY_TMN_CODE=dummy" >> .env
echo "VNPAY_HASH_SECRET=dummy" >> .env

# 5. Start Backend Only
sudo docker-compose -f docker-compose.app.yml up -d --build
