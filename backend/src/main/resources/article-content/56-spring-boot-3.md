# Spring Boot 3.x 新特性解析

这篇文章不打算把 Spring Boot 3.x 的发布说明重新抄一遍。对我这个博客项目来说，升级真正带来的变化主要集中在三个地方：Java 版本和 Jakarta 命名空间、Spring Security 6 的配置方式，以及 Redis 等基础设施配置的迁移。

## 第一件事：先确认升级边界

Spring Boot 3 的最低 Java 版本是 17，同时底层升级到 Spring Framework 6 和 Jakarta EE 10。也就是说，这不是只改一下 `pom.xml` 版本号的升级。项目里凡是直接依赖 Servlet、Validation、JPA 的地方，都要检查 `javax.*` 是否已经迁移成 `jakarta.*`，第三方依赖也要确认是否支持新的 API。

我的做法是先让项目停在一个可以工作的中间状态，再逐个处理编译错误。一次性修改所有依赖和业务代码，很容易把“版本不兼容”和“业务逻辑回归”混在一起。先升级 Java 和 Spring Boot，再处理 Jakarta 导入，最后验证安全配置和数据库访问，排查范围会小很多。

## Spring Security 6：配置写法变了，边界更清楚了

以前很多项目会继承 `WebSecurityConfigurerAdapter`，然后在几个重写方法里配置路径。Spring Security 6 更推荐直接声明 `SecurityFilterChain` Bean。表面上是 API 变化，实际影响是安全规则更像一份明确的配置表：哪些路径公开，哪些路径需要登录，哪些路径只允许管理员，一眼就能看出来。

我的博客把公开读接口和管理写接口拆开处理。文章列表、文章详情、归档和搜索属于匿名可读；创建、更新、删除文章属于认证接口；后台管理接口还要额外检查 ADMIN 角色。JWT 只负责携带身份信息，真正的权限判断仍然在服务端完成，不能因为前端把按钮隐藏了就认为用户没有权限。

迁移过程中最容易忽略的是 OPTIONS 请求、静态资源和错误页面。前端登录、跨域请求和浏览器预检都可能在业务接口之前发生，所以安全规则不能只盯着 `/api` 主路径。每改一次 Security 配置，我都会用匿名请求、普通用户请求和管理员请求分别验证，而不是只看项目能否启动。

## Jakarta 迁移不只是替换字符串

把 `javax.persistence` 换成 `jakarta.persistence` 只是开始。Hibernate 版本、验证器、Servlet 容器和部分第三方 starter 都可能跟着变化。尤其是自定义异常处理、参数校验和 JPA 实体映射，编译通过不代表运行时一定正确。

我会重点检查三类路径：启动时是否能创建 EntityManager，提交无效参数时是否仍返回预期的校验错误，文章保存和查询是否仍能正确加载分类、标签等关联对象。这些检查比单纯看首页更容易发现迁移遗漏。

## Redis 配置的细节很容易踩坑

Spring Boot 3 对 Redis 的配置命名空间更加明确，项目里使用的是 `spring.data.redis`。如果代码里同时存在旧的 `spring.redis` 配置，最好明确哪个是主配置，避免开发环境和生产环境连接到不同的地址。

我的部署文件会给后端显式传入 Redis 主机、端口和密码，容器内使用 Compose 服务名 `redis`，本地开发则使用 `localhost`。这种差异不应该散落在业务代码里，而应该由环境配置解决。最近一次线上 502 就很好地说明了依赖服务名的重要性：后端能启动不等于它能解析到 Redis，依赖容器的健康状态必须被单独验证。

## AOT 和原生镜像，我暂时没有盲目跟进

Spring Boot 3 对 AOT 和 GraalVM 原生编译的支持更成熟，但这不代表所有项目都应该马上切过去。原生镜像可以改善启动时间和内存占用，但也会带来反射配置、动态代理、第三方库兼容等额外工作。我的博客目前运行在一台资源有限的云服务器上，先把容器健康、缓存一致性和部署回滚做稳，比为了追求一个更小的启动数字更重要。

## 升级后的检查清单

1. 使用 Java 17 编译并启动后端。
2. 搜索 `javax` 引用，确认没有遗留的 Jakarta 迁移问题。
3. 匿名、普通用户、管理员分别验证接口权限。
4. 检查 H2 开发环境和 MySQL 生产环境的配置差异。
5. 验证 Redis、RabbitMQ 和 MySQL 的健康检查。
6. 访问文章列表、详情、搜索、归档和管理端写接口。

升级框架真正的收益，不是让简历上的版本号更新，而是借这个机会重新确认依赖边界、权限边界和运行边界。对一个个人项目来说，这种确认往往比新 API 本身更有价值。

参考：

- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Spring Boot Actuator REST API](https://docs.spring.io/spring-boot/api/rest/actuator/)
