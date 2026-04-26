---
name: java-backend-check
description: 检查当前 Java/Spring Boot 后端改动是否存在常见问题。适用于 controller、service、mapper、配置、异常处理、SQL、鉴权相关代码审查。
allowed-tools: Read Grep Glob Bash(mvn *) Bash(git diff *) Bash(git status)
---

对当前项目的 Java 后端改动做一次面向学习和实战的检查，重点关注：

1. 分层是否合理：controller / service / mapper 职责是否混乱
2. Spring Boot 常见问题：依赖注入、事务边界、配置项、异常处理
3. MyBatis / SQL 风险：空值、条件拼接、批量操作、分页、N+1、字段映射
4. 安全问题：鉴权遗漏、越权、敏感信息暴露、硬编码密钥、文件路径风险
5. 可维护性：重复逻辑、命名不清、过度复杂、无意义抽象
6. 与用户目标匹配：如果发现问题，优先解释“为什么这是后端开发里常见坑”

输出要求：
- 按“严重 / 建议”分组
- 每条都带 file_path:line_number
- 先说问题，再给简短修正建议
- 如果整体没大问题，明确说明“当前实现可继续推进”
