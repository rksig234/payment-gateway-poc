variable "region" {
  type    = string
  default = "ap-south-1"
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "db_password" {
  type      = string
  sensitive = true
  # supply via TF_VAR_db_password or a secret; do not commit.
}
