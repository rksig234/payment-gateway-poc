variable "name" { type = string }
variable "subnet_ids" { type = list(string) }
variable "vpc_security_group_ids" { type = list(string) }
variable "instance_class" {
  type    = string
  default = "db.t3.medium"
}
variable "db_name" {
  type    = string
  default = "payments"
}
variable "username" {
  type    = string
  default = "payments"
}
variable "password" {
  type      = string
  sensitive = true
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-pg-subnets"
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "this" {
  identifier             = "${var.name}-postgres"
  engine                 = "postgres"
  engine_version         = "16"

  instance_class         = "db.t3.micro"
  allocated_storage      = 20

  multi_az               = false

  db_name                = var.db_name
  username               = var.username
  password               = var.password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = var.vpc_security_group_ids

  skip_final_snapshot    = true
}

output "endpoint" { value = aws_db_instance.this.address }
output "jdbc_url" { value = "jdbc:postgresql://${aws_db_instance.this.address}:5432/${var.db_name}" }
