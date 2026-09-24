# k8s-demo

一个用来练习 Kubernetes 的留言板。Vue 页面通过 Nginx 把 `/api` 转到 Spring Boot，留言存在 MySQL，列表缓存在 Redis，缓存 30 秒。

## 结构

- `frontend`：Vue 3 + Vite，容器里用 Nginx 托管，并把 `/api` 反代到 `backend:8080`
- `backend`：Spring Boot，JPA 写 MySQL，列表 JSON 写入 Redis
- `k8s`：Namespace、ConfigMap、Secret、MySQL、Redis、后端 2 副本、前端 NodePort `30080`

页面上的 Pod 名称来自 Downward API。多刷新几次，可以看到请求落到不同的后端副本。来源徽章在第一次读取后显示「Redis 缓存」，新增或删除会清掉缓存。

数据库账号只用于本地练习：用户 `demo`，密码 `demo123`，root 密码 `root123`。

## 本地用 Docker Compose

```bash
docker compose up --build
```

打开 http://localhost:8088 。接口在 http://localhost:8080 。

## 分开开发

先启动 MySQL 和 Redis：

```bash
docker compose up mysql redis
```

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 。Vite 会把 `/api` 代理到 `localhost:8080`。

## 部署到 Kubernetes

镜像名要和清单一致：

```bash
docker build -t k8s-demo/backend:1.0.0 ./backend
docker build -t k8s-demo/frontend:1.0.0 ./frontend
kubectl apply -k k8s
```

集群需要能拉到 `mysql:8.4`、`redis:7-alpine`、`busybox:1.36`，并且有默认 StorageClass（MySQL 使用 1Gi PVC）。

Minikube 先把构建指到集群里的 Docker：

```powershell
minikube docker-env | Invoke-Expression
docker build -t k8s-demo/backend:1.0.0 ./backend
docker build -t k8s-demo/frontend:1.0.0 ./frontend
kubectl apply -k k8s
minikube service frontend -n k8s-demo
```

kind 在构建后执行：

```bash
kind load docker-image k8s-demo/backend:1.0.0
kind load docker-image k8s-demo/frontend:1.0.0
```

NodePort 是 `30080`。`k8s/ingress.yaml` 没有放进 Kustomize，需要 Ingress Controller 时再单独 apply，并把 `k8s-demo.local` 指到入口。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/notes` | 留言列表，`cached` 表示是否命中 Redis |
| POST | `/api/notes` | `{ "author": "可选", "content": "必填" }` |
| DELETE | `/api/notes/{id}` | 删除并清缓存 |
| GET | `/api/status` | MySQL、Redis、Pod 名 |
| GET | `/actuator/health/liveness` | 存活探针 |
| GET | `/actuator/health/readiness` | 就绪探针，包含数据库和 Redis |
