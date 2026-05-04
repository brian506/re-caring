import {
   to = aws_ecs_cluster.main
   id = "recaring-cluster"           # 콘솔에서 확인한 클러스터 이름
 }
 import {
   to = aws_ecs_task_definition.nginx
   id = "nginx:2"                    # family:revision (최신 revision)
 }
 import {
   to = aws_ecs_service.nginx
   id = "recaring-cluster/nginx"     # cluster/service
 }
 import {
   to = aws_ecs_task_definition.spring_app
   id = "recaring-task:1"
 }
 import {
   to = aws_ecs_service.spring_app
   id = "recaring-cluster/recaring-service"
 }
 import {
   to = aws_ecr_repository.nginx
   id = "nginx"
 }
 import {
   to = aws_ecr_repository.spring_app
   id = "re-caring-api"
 }