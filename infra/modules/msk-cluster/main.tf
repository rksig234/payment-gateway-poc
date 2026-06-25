variable "name" { type = string }
variable "subnet_ids" { type = list(string) }
variable "security_group_ids" { type = list(string) }
variable "kafka_version" {
  type    = string
  default = "3.6.0"
}
variable "broker_count" {
  type    = number
  default = 3
}
variable "instance_type" {
  type    = string
  default = "kafka.t3.small"
}

resource "aws_msk_cluster" "this" {
  cluster_name           = "${var.name}-msk"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.broker_count

  broker_node_group_info {
    instance_type   = var.instance_type
    client_subnets  = var.subnet_ids
    security_groups = var.security_group_ids
    storage_info {
      ebs_storage_info {
        volume_size = 50
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
      in_cluster    = true
    }
  }
}

output "bootstrap_brokers" { value = aws_msk_cluster.this.bootstrap_brokers }
