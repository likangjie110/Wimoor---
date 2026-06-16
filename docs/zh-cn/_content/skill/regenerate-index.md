# 索引再生成

项目索引源在 `docs/project-map/indexes`。需要刷新时先运行只读命令，再更新对应 Markdown。

## 服务和接口

```powershell
rg -n "@SpringBootApplication|SpringApplication\.run" -g "*.java"
rg -n "@RestController|@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" -g "*.java"
rg -n "@FeignClient" -g "*.java"
```

## 前端

```powershell
rg -n "path:" wimoorui/src/router
rg --files wimoorui/src/api -g "*.js"
```

## 数据库和配置

```powershell
rg --files "init-config/mysql/数据库结构" -g "*.sql"
rg --files "init-config/mysql/数据" -g "*.sql"
rg --files "init-config/nacos/DEFAULT_GROUP"
```

