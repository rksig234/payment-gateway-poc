# Remote state for the POC. Create the bucket + lock table once, then uncomment.
# terraform {
#   backend "s3" {
#     bucket         = "sigmoid-paymentgateway-tfstate"
#     key            = "poc/terraform.tfstate"
#     region         = "ap-south-1"
#     dynamodb_table = "paymentgateway-tf-lock"
#     encrypt        = true
#   }
# }
