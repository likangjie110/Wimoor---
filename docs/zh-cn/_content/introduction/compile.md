# 编译

## 后端编译

从仓库根目录执行：

```powershell
mvn clean package -DskipTests
```

运行全部测试：

```powershell
mvn test
```

运行单模块测试示例：

```powershell
mvn -pl wimoor-erp/wimoor-erp-proxy test
```

## 前端编译

进入 `wimoorui`：

```powershell
npm install
npm run build
```

开发模式：

```powershell
npm run dev
```

## 编译失败优先检查

- JDK 是否为 1.8。
- Maven 是否能访问依赖仓库。
- 前端依赖是否完整安装。
- 是否误把 `target`、`node_modules` 中的产物纳入源码检查。

