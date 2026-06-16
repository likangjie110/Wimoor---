# 常用命令

## 后端

```powershell
mvn clean package -DskipTests
mvn test
mvn -pl wimoor-erp/wimoor-erp-proxy test
```

## 前端

```powershell
Set-Location wimoorui
npm install
npm run dev
npm run build
```

## 搜索

```powershell
rg --files
rg -n "@RestController" -g "*.java"
rg -n "path:" wimoorui/src/router
```

