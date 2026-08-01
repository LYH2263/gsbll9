# 校园 CTF 在线竞赛平台

面向学校计算机社团的 CTF（Capture The Flag）在线竞赛系统：支持学号登录、分类抽题、Flag 提交校验、提示解锁、实时排行与管理后台，用于组织网络安全入门竞赛与技能训练。

## 🎯 功能特性

### 用户认证系统
- ✅ 学号+密码登录认证
- ✅ JWT令牌加密传输（HS512签名）
- ✅ 特殊管理员账号支持
- ✅ 密码采用BCrypt哈希存储（不可逆加密）

### 管理员功能
- ✅ 完整的用户管理（增删改查）
- ✅ 题目分类管理
- ✅ 题目内容管理（文字 + 文件链接）
- ✅ 比赛时间配置（实时生效，无需重启）
- ✅ 实时数据统计

### 参赛者功能
- ✅ 自动从各分类随机抽取题目
- ✅ 标准答题界面（Flag输入）
- ✅ 题目导航与快速跳转
- ✅ 实时排名榜显示
- ✅ 比赛倒计时

### 评分与排名系统
- ✅ 每题1分的计分制
- ✅ 同分按用时排序
- ✅ 实时排名展示
- ✅ 自动提交与成绩保存

### 比赛时间控制
- ✅ 准备阶段 (15:20-15:30) - 登录但不能答题
- ✅ 比赛阶段 (15:30-16:30) - 正式答题
- ✅ 截止阶段 (16:30-17:00) - 自动提交
- ✅ 公布阶段 (17:00-) - 显示成绩
- ✅ 后台实时调整时间配置

## 🛠 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 + Vite | Latest |
| **UI框架** | Tailwind CSS | 3.3+ |
| **状态管理** | Pinia | Latest |
| **路由** | Vue Router (Hash模式) | Latest |
| **后端** | Spring Boot | 3.1.5 |
| **ORM框架** | MyBatis | 3.0.2 |
| **数据库** | MySQL | 8.0 |
| **认证** | JWT + Spring Security | 0.12.3 |
| **容器化** | Docker & Docker Compose | Latest |

## 🚀 快速启动

### 前置要求
- Docker Desktop (已安装并运行)
- Git

### 一键启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd <project-directory>

# 2. 启动所有服务（Docker Compose会自动构建镜像）
docker compose up -d --build

# 3. 等待服务启动（约30秒）
docker compose logs -f

# 4. 访问系统
# 前端：http://localhost:3615
# 后端API：http://localhost:8615/api
```

### 服务说明

| 服务 | 端口 | 描述 |
|------|------|------|
| **frontend** | 3615 | Vue3前端应用 (Nginx) |
| **backend** | 8615 | Spring Boot后端API |
| **db** | 3306 | MySQL数据库 |

### 默认账号

| 角色 | 学号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | root | admin123 | 管理后台访问 |
| 学生 | 2024001 | 123456 | 测试参赛账号 |

## 📚 使用说明

### 管理员操作指南

1. **登录管理后台**
   - 访问 http://localhost:3615
   - 使用管理员账号登录 (root / admin123)
   - 自动跳转到管理后台 (#/admin)

2. **用户管理**
   - 点击"👥 用户管理"标签
   - 添加用户：点击"➕ 添加用户"按钮
   - 编辑用户：点击用户卡片的"编辑"按钮
   - 删除用户：点击"删除"按钮并确认

3. **分类管理**
   - 点击"📂 分类管理"标签
   - 默认分类：Crypto, Misc, Reverse, Web, Pwn
   - 可自定义添加/修改分类

4. **题目管理**
   - 点击"📝 题目管理"标签
   - 添加题目时需指定分类、难度、Flag答案
   - 支持添加文件链接（附件下载）

5. **比赛设置**
   - 点击"⚙️ 比赛设置"标签
   - 配置四个关键时间点：
     - **准备时间**：参赛者可以登录但不能答题
     - **开始时间**：正式开始答题
     - **结束时间**：自动提交所有答案
     - **发布时间**：公布成绩和排名
   - 点击"💾 保存设置"后立即生效，无需重启

### 参赛者操作指南

1. **登录系统**
   - 访问 http://localhost:3615
   - 使用学号和密码登录
   - 自动跳转到答题页面 (#/contest)

2. **开始比赛**
   - 准备时间内可以登录查看状态
   - 到达开始时间后，点击"开始比赛"按钮
   - 系统自动从各分类随机抽题

3. **答题流程**
   - 阅读题目描述
   - 如有附件，点击"📥 下载文件"
   - 在输入框输入Flag答案
   - 点击"提交答案"或按Enter键
   - 答对自动跳转下一题

4. **查看排名**
   - 答题页面右侧实时显示排名
   - 可查看自己的当前排名和得分

### 批量添加用户

#### 方法一：使用SQL脚本（推荐）

创建批量添加脚本 `batch_add_users.sql`：

```sql
-- 批量添加用户示例
-- 密码统一为 "123456"，密码hash为BCrypt加密后的值

INSERT INTO users (student_id, username, password_hash, full_name, role, is_active) VALUES
('2024001', 'student1', '$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe', '张三', 'user', 1),
('2024002', 'student2', '$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe', '李四', 'user', 1),
('2024003', 'student3', '$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe', '王五', 'user', 1),
('2024004', 'student4', '$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe', '赵六', 'user', 1),
('2024005', 'student5', '$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe', '钱七', 'user', 1);

-- 执行方式：
-- docker exec -i ctf-mysql mysql -uroot -proot ctf_db < batch_add_users.sql
```

执行命令：
```bash
# Windows PowerShell
Get-Content batch_add_users.sql | docker exec -i ctf-mysql mysql -uroot -proot ctf_db

# Linux/Mac
docker exec -i ctf-mysql mysql -uroot -proot ctf_db < batch_add_users.sql
```

#### 方法二：生成带随机密码的用户

创建生成脚本 `generate_users.py`：

```python
import bcrypt

# 用户列表
users = [
    ('2024001', 'student1', '张三', '123456'),
    ('2024002', 'student2', '李四', 'pass123'),
    ('2024003', 'student3', '王五', 'abc123'),
    # ... 添加更多用户
]

print("INSERT INTO users (student_id, username, password_hash, full_name, role, is_active) VALUES")

for i, (student_id, username, fullname, password) in enumerate(users):
    # 生成BCrypt密码哈希
    password_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    
    comma = ',' if i < len(users) - 1 else ';'
    print(f"('{student_id}', '{username}', '{password_hash}', '{fullname}', 'user', 1){comma}")
```

运行并导入：
```bash
# 生成SQL
python generate_users.py > users.sql

# 导入数据库
Get-Content users.sql | docker exec -i ctf-mysql mysql -uroot -proot ctf_db
```

#### 方法三：通过管理后台Excel导入（自定义功能）

如需批量导入，可以扩展管理后台添加CSV/Excel导入功能。

### 常用密码哈希值

以下是常用密码的BCrypt哈希（用于SQL批量添加）：

| 密码 | BCrypt Hash |
|------|-------------|
| 123456 | `$2a$10$N9qo8uLOickgx2ZfVB2ZMeXn02JqNj1OdL1zjM3D.m9UWrYQ.xMPe` |
| password | `$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG` |
| admin123 | `$2a$10$E2UPv7arXmp3q0LzVzCBNeb4B4AtbTAGjkefVDnSztEcEqKEXY6CK` |

## 🔧 配置说明

### 数据库配置

默认配置在 `docker-compose.yml`:
```yaml
MYSQL_ROOT_PASSWORD: root
MYSQL_DATABASE: ctf_db
```

### 时区配置

系统默认时区：`Asia/Shanghai` (UTC+8)

### 端口配置

如需修改端口，编辑 `docker-compose.yml`:
```yaml
ports:
  - "3615:80"      # 前端端口
  - "8615:8615"    # 后端端口
  - "3306:3306"    # 数据库端口
```

## 🐛 故障排查

### 容器启动失败

```bash
# 查看所有容器状态
docker compose ps

# 查看特定服务日志
docker compose logs backend
docker compose logs frontend
docker compose logs db

# 重启所有服务
docker compose restart

# 完全重建
docker compose down
docker compose up -d --build
```

### 数据库连接失败

```bash
# 检查数据库健康状态
docker compose exec db mysqladmin ping -h localhost -uroot -proot

# 进入数据库查看
docker exec -it ctf-mysql mysql -uroot -proot ctf_db

# 查看表结构
SHOW TABLES;
DESCRIBE users;
```

### 前端无法访问后端

检查Nginx配置是否正确代理API请求到后端。

### 清空所有数据重新开始

```bash
# 停止并删除容器和数据卷
docker compose down -v

# 重新启动（数据库会自动初始化）
docker compose up -d --build
```

## 📊 数据库结构

### 主要数据表

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| **users** | 用户表 | student_id, username, password_hash, role |
| **categories** | 分类表 | name, description, order_num |
| **questions** | 题目表 | title, description, flag, category_id, points |
| **contest_users** | 参赛记录表 | user_id, score, use_time, selected_questions |
| **submissions** | 提交记录表 | contest_user_id, question_id, is_correct |
| **contest_config** | 比赛配置表 | config_key, config_value |

## 🔐 认证与密码安全设计说明

本项目采用业界标准的**双层安全机制**，两者职责不同、互为补充：

### 密码存储：BCrypt 单向哈希

用户密码在写入数据库前经过 BCrypt 算法处理，具有以下特性：

- **不可逆**：无法从哈希值还原原始密码
- **加盐**：每次生成的哈希值不同，防止彩虹表攻击
- **高耗时**：计算代价高，抵抗暴力破解

```
用户输入密码 → BCrypt哈希 → 存入数据库
登录验证时  → BCrypt比对 → 不存储明文
```

### 身份传输：JWT 令牌（HS512签名）

登录成功后颁发 JWT Token，用于后续接口的身份认证：

- **Header**：算法声明（HS512）
- **Payload**：用户ID、学号、角色（明文 Base64，不含密码）
- **Signature**：服务端密钥签名，防止伪造

```
登录成功 → 签发 JWT Token → 存入浏览器 localStorage
后续请求 → 携带 Authorization: Bearer <token> → 服务端验签
```

> **为何不用 JWT 存储密码？**
> JWT payload 仅做 Base64 编码，并非加密，任何人均可解码读取内容。
> 若将密码存入 JWT 或数据库，将导致密码直接暴露。
> 密码存储必须使用**单向不可逆哈希**（如 BCrypt），这是安全规范的强制要求。
> 密码采用 BCrypt 不可逆哈希存储，身份鉴权采用 JWT 令牌机制，符合业界标准安全规范。原需求文档术语使用不够准确。
---

## 🔐 安全提示

1. **生产环境部署前务必修改**：
   - 数据库root密码
   - JWT密钥 (`application.yml` 中的 `jwt.secret`)
   - 管理员默认密码

2. **定期备份数据库**：
```bash
# 备份数据库
docker exec ctf-mysql mysqldump -uroot -proot ctf_db > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i ctf-mysql mysql -uroot -proot ctf_db < backup_20260130.sql
```

## 📝 开发说明

### 目录结构

```
project-root/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/ctf/
│   │       ├── controller/  # REST控制器
│   │       ├── service/     # 业务逻辑
│   │       ├── mapper/      # MyBatis Mapper
│   │       ├── entity/      # 数据实体
│   │       └── util/        # 工具类
│   └── src/main/resources/
│       ├── application.yml  # 应用配置
│       └── schema.sql       # 数据库初始化脚本
├── frontend/                # Vue3前端
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 通用组件
│   │   ├── api/            # API客户端
│   │   ├── store/          # Pinia状态管理
│   │   └── router/         # 路由配置
│   └── nginx.conf          # Nginx配置
└── docker-compose.yml       # Docker编排配置
```

### 本地开发

```bash
# 前端开发
cd frontend
npm install
npm run dev

# 后端开发
cd backend
mvn spring-boot:run
```

## 📄 许可证

本项目仅供教育和学习使用。

## 🤝 贡献

欢迎提交Issue和Pull Request！

---

**祝比赛顺利！Have fun with CTF! 🎉**

### 前置要求
- Docker Desktop (已启动)
- 磁盘空间 >= 2GB
- 网络连接

### 一键启动

```bash
# 1. 进入项目根目录
cd /path/to/project

# 2. 启动所有服务
docker compose up --build

# 3. 等待输出以下信息表示启动成功
# db      | ready for connections
# backend | Started CtfContestApplication
# frontend | ready in XXX ms
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端** | http://localhost:3615 | Web用户界面 |
| **后端API** | http://localhost:8615/api | 后端服务 |
| **MySQL** | localhost:3306 | 数据库连接 |
| **Swagger** | http://localhost:8615/api/swagger-ui | API文档 |

## 📝 测试账号

### 管理员
- **学号**: root
- **密码**: admin123
- **权限**: 完全的系统管理权限

### 普通参赛者
- **学号**: 2024001
- **密码**: 123456
- **权限**: 答题和查看排名

### 批量导入账号
如需导入更多用户，请在MySQL中执行 `database/import_users.sql` (需自行创建)

```sql
INSERT INTO users (student_id, username, password_hash, full_name, role, is_active) VALUES
('2024002', '参赛者2', '$2a...(BCrypt密码哈希)', '王二', 'user', true),
('2024003', '参赛者3', '$2a...(BCrypt密码哈希)', '赵三', 'user', true);
```

## 🔗 服务地址与端口配置

### Docker Compose 配置

```yaml
# 前端：3615
frontend:
  ports:
    - "3615:80"

# 后端：8615
backend:
  ports:
    - "8615:8615"

# MySQL：3306 (标准MySQL端口)
db:
  ports:
    - "3306:3306"
```

### 跨容器通信
- 前端访问后端：`http://backend:8615/api`
- 后端访问数据库：`jdbc:mysql://db:3306/ctf_db`

## 📊 数据库架构

### 核心表结构

| 表名 | 说明 |
|------|------|
| `users` | 用户账号与权限 |
| `categories` | 题目分类 |
| `questions` | 题目内容与答案 |
| `contest_users` | 用户比赛进度 |
| `submissions` | 答题提交记录 |

### 初始化数据
- 1个管理员账号 (root)
- 5个题目分类 (Crypto, Misc, Reverse, Web, Pwn)
- 10个示例题目

## 🔐 安全特性

- ✅ **密码加密**: BCrypt加密存储，SHA512签名的JWT
- ✅ **跨域保护**: CORS配置严格限制
- ✅ **SQL注入防护**: 使用MyBatis参数化查询
- ✅ **XSS防护**: Vue.js自动转义，Tailwind安全构建

## 📋 API接口文档

### 认证接口
```
POST /api/auth/login
  请求: { "studentId": "2024001", "password": "123456" }
  响应: { "token": "jwt...", "userId": 1, "role": "user" }

GET /api/auth/verify
  验证JWT token有效性
```

### 比赛接口
```
POST /api/contest/start          - 开始比赛
GET /api/contest/status          - 获取比赛状态
GET /api/contest/current-question - 获取当前题目
POST /api/contest/submit-answer   - 提交答案
GET /api/contest/rankings         - 获取排名
```

### 管理接口
```
GET /api/admin/users              - 获取所有用户
POST /api/admin/users             - 添加用户
GET /api/admin/categories         - 获取分类
POST /api/admin/questions         - 添加题目
```

## 🐛 常见问题

### Q: 启动时MySQL连接失败？
A: 确保MySQL容器已完全启动（等待30秒）。查看日志：
```bash
docker compose logs db
```

### Q: 前端无法连接后端？
A: 检查后端服务状态和CORS配置：
```bash
docker compose logs backend
curl http://localhost:8615/api/contest/status
```

### Q: 数据库初始化脚本未执行？
A: 第一次启动时自动执行。若需手动执行：
```bash
docker compose exec db mysql -uroot -proot ctf_db < database/init.sql
```

### Q: 容器占用端口冲突？
A: 修改 `docker-compose.yml` 中的端口映射：
```yaml
frontend:
  ports:
    - "3700:80"  # 改为 3700
backend:
  ports:
    - "8700:8615"  # 改为 8700
```

## 📦 项目结构

```
project-root/
├── backend/                      # Spring Boot后端
│   ├── src/main/java/com/ctf/
│   │   ├── controller/          # 控制器
│   │   ├── service/             # 业务逻辑
│   │   ├── mapper/              # MyBatis Mapper
│   │   ├── entity/              # 数据实体
│   │   ├── dto/                 # 数据传输对象
│   │   ├── util/                # 工具类
│   │   └── config/              # 配置类
│   ├── src/main/resources/
│   │   ├── application.yml      # 应用配置
│   │   └── mapper/              # MyBatis XML
│   ├── pom.xml                  # Maven依赖
│   ├── Dockerfile              # 后端容器化
│   └── settings.xml            # Maven镜像配置
│
├── frontend/                     # Vue 3前端
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── components/         # 通用组件
│   │   ├── store/              # Pinia状态
│   │   ├── api/                # API调用
│   │   ├── router/             # 路由配置
│   │   ├── App.vue             # 根组件
│   │   └── main.js             # 入口文件
│   ├── index.html              # HTML模板
│   ├── package.json            # 前端依赖
│   ├── vite.config.js          # Vite配置
│   ├── tailwind.config.js      # Tailwind配置
│   ├── Dockerfile              # 前端容器化
│   └── .dockerignore           # Docker忽略文件
│
├── nginx/                        # Nginx配置
│   └── nginx.conf              # 反向代理配置
│
├── database/                     # 数据库脚本
│   └── init.sql                # 初始化SQL
│
├── docker-compose.yml          # 容器编排
├── .gitignore                  # Git忽略
├── .dockerignore               # Docker忽略
└── README.md                   # 项目文档
```

## 🔧 性能优化

### 前端
- ✅ Vite 5代码分割与按需加载
- ✅ Tailwind CSS 生产优化
- ✅ Vue 3 Composition API 最小化
- ✅ Gzip压缩 (Nginx配置)

### 后端
- ✅ Spring Boot 3多线程处理
- ✅ MyBatis 缓存配置
- ✅ 连接池优化 (HikariCP)
- ✅ 日志异步输出

### 数据库
- ✅ 索引优化查询
- ✅ 外键级联删除
- ✅ 字符集UTF-8MB4

## 📄 许可证

MIT License - 自由使用与修改

## 👥 作者

CTF竞赛系统开发团队
- 设计时间: 2025年1月
- 维护: 学校计算机社团

## 📞 技术支持

遇到问题？

1. 查看 `docker compose logs` 输出
2. 检查所有容器运行状态：`docker compose ps`
3. 验证网络连接：`docker network ls`
4. 联系技术管理员

---

**祝大家在CTF竞赛中取得好成绩！** 🎉
