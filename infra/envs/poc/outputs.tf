output "alb_dns_name" {
  description = "Public DNS of the gateway ALB"
  value       = aws_lb.gateway.dns_name
}

output "rds_endpoint" { value = module.rds.endpoint }
output "redis_endpoint" { value = module.redis.endpoint }
output "msk_bootstrap_brokers" { value = module.msk.bootstrap_brokers }
output "ecs_cluster" { value = aws_ecs_cluster.this.name }
