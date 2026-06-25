data "aws_caller_identity" "current" {}

locals {
  name     = "paymentgw-poc"
  ecr_base = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/payment-gateway"
}

module "network" {
  source = "../../modules/network"
  name   = local.name
}

module "rds" {
  source                 = "../../modules/rds-postgres"
  name                   = local.name
  subnet_ids             = module.network.private_subnet_ids
  vpc_security_group_ids = [module.network.data_sg_id]
  password               = var.db_password
}

module "redis" {
  source             = "../../modules/elasticache-redis"
  name               = local.name
  subnet_ids         = module.network.private_subnet_ids
  security_group_ids = [module.network.data_sg_id]
}

module "msk" {
  source             = "../../modules/msk-cluster"
  name               = local.name
  subnet_ids         = module.network.private_subnet_ids
  security_group_ids = [module.network.data_sg_id]
}

resource "aws_ecs_cluster" "this" {
  name = "${local.name}-cluster"
}

# IAM roles for ECS tasks.
data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${local.name}-exec"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

# Public ALB fronting the gateway only.
resource "aws_lb" "gateway" {
  name               = "${local.name}-alb"
  load_balancer_type = "application"
  subnets            = module.network.public_subnet_ids
  security_groups    = [module.network.alb_sg_id]
}

resource "aws_lb_target_group" "gateway" {
  name        = "${local.name}-gw-tg"
  port        = 8090
  protocol    = "HTTP"
  vpc_id      = module.network.vpc_id
  target_type = "ip"
  health_check {
    path = "/actuator/health"
    port = "8090"
  }
}

resource "aws_lb_listener" "gateway" {
  load_balancer_arn = aws_lb.gateway.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway.arn
  }
}

# One ECS service per application, driven by a single map.
locals {
  common_kafka = module.msk.bootstrap_brokers
  services = {
    payment-stubs = {
      port = 8080
      env  = {}
    }
    payment-platform = {
      port = 8090
      env = {
        KAFKA_BOOTSTRAP = local.common_kafka
        REDIS_HOST      = module.redis.endpoint
        DB_URL          = module.rds.jdbc_url
        DB_PASSWORD     = var.db_password
        STUBS_BASE_URL  = "http://payment-stubs.${local.name}.local:8080" # via service discovery (Cloud Map)
        SNS_ENABLED     = "true"
      }
    }
  }
}

module "service" {
  source             = "../../modules/ecs-service"
  for_each           = local.services
  name               = each.key
  cluster_arn        = aws_ecs_cluster.this.arn
  image              = "${local.ecr_base}/${each.key}:${var.image_tag}"
  container_port     = each.value.port
  subnet_ids         = module.network.private_subnet_ids
  security_group_ids = [module.network.app_sg_id]
  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.task.arn
  region             = var.region
  environment        = each.value.env
  target_group_arn   = each.key == "payment-platform" ? aws_lb_target_group.gateway.arn : null
}
