# Wimoor 文档中心

Wimoor 现在以文档驱动的方式维护：

- `docs/zh-cn`：中文说明文档，同时也是 Obsidian 本地知识库根目录
- `docs/project-map`：技术索引、流程图、时序图和模块地图
- `tools/docs`：本地预览、校验和发布脚本

## 中文访问方式

GitHub Pages 访问地址：

- [https://likangjie110.github.io/Wimoor---/](https://likangjie110.github.io/Wimoor---/)
- [https://likangjie110.github.io/Wimoor---/zh-cn/#/](https://likangjie110.github.io/Wimoor---/zh-cn/#/)

本地预览：

```powershell
tools/docs/serve-docs.ps1
```

然后打开：

- `http://localhost:3000/`
- `http://localhost:3000/zh-cn/#/`

如果线上地址返回 404，请先进入 GitHub 仓库：

`Settings -> Pages -> Build and deployment -> Source -> GitHub Actions`

本仓库已提供 `.github/workflows/pages.yml`，保存后由 `pages` workflow 自动把 `docs` 目录发布为 Docsify 站点。

## English access

The repository now ships as a Docsify documentation site.

- `docs/zh-cn` is the Chinese docs vault and the main Docsify content root
- `docs/project-map` contains technical indexes and diagrams
- `tools/docs` contains preview, validation, and publish scripts

Docsify on GitHub Pages:

- [https://likangjie110.github.io/Wimoor---/](https://likangjie110.github.io/Wimoor---/)
- [https://likangjie110.github.io/Wimoor---/zh-cn/#/](https://likangjie110.github.io/Wimoor---/zh-cn/#/)

Local preview:

```powershell
tools/docs/serve-docs.ps1
```

Open:

- `http://localhost:3000/`
- `http://localhost:3000/zh-cn/#/`

## Reading order

1. [中文说明文档首页](docs/zh-cn/README.md)
2. [项目地图](docs/project-map/README.md)
3. [Obsidian 维护规范](docs/zh-cn/_content/skill/obsidian-workflow.md)
4. [GitHub Pages 发布流程](docs/zh-cn/_content/skill/github-pages.md)

## Publish

```powershell
tools/docs/check-docs.ps1
tools/docs/publish-docs.ps1 -Message "docs: 更新文档首页和访问说明"
```

## Notes

- 使用 Markdown 相对链接，避免 `[[WikiLinks]]` 造成 Docsify 兼容问题
- 需要通过 GitHub Pages 访问时，先在仓库 `Settings -> Pages` 中选择 `GitHub Actions`
- 敏感配置只保留键名或 `<redacted>`
