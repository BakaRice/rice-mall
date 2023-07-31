我想请伙伴们思索下，目前如果让你做用户的权限认证，你会怎么做? 你有什么选择？

1. 原始的方式，用户名+密码登录，后台通过 uuid 生成一个token，返回给前端，这个token我们可以存储到mysql或者redis服务器中，并且设置它的一个过期时间。
2. spring security + jwt进行权限验证。目前这种形式，中小型的企业使用的比较多（一些内用的或者非 电商，非公开性的网站使用这种方式比较多）。
3. oauth2.0（需要spring security包的支持） 进行权限的认证。
   1. 通过 用户名 + 密码的形式进行验证  
      他跟原始方式有区别，oauth2还需要一个 `client id`
      和`client secret`. 如果我们的 用户名 + 密码 有了，个人用户无需额外申请 `client id`
      和`client secret`，我们后台根据一定的规则，为这个用户名和密码生成一套`client_id` 和`client secret`
   
   2. 客户端验证  
      我们通过 手机+验证码的形式，可以登录一个网站，后台会为当前用户随机生一个用户名，且当
      前用户没有密码。因此，我们采用客户端验证形式，为当前的手机用户创建 `client id` 和 `client secret`。以此
      来保证用户能够顺利的登录。

   3. 授权码验证模式  
      我们开发的网站叫 x宝，我们如果想用x宝去登录另外一个网站，那么就需要使用到授权码模式。
      我们可以用微信登录 淘宝；微信有自己的oauth2.0的服务器； 淘宝需要在微信的 oauth服务中注册一个`client id`
      和 `client secret`，当我们用微信登录淘宝的那一刻，淘宝会向微信的oauth服务发送登录请求进行验证，验证通过后，微信会
      回调淘宝的接口，发送当前登陆者的微信非敏感信息给淘宝，然后淘宝将该微信用户的信息进行存储。
   
   4. 隐藏模式、 简单模式（弃用），这个东西太简陋了。

---
对于我们的 oauth2.0的验证，会获取一个token，这个token可以通过check_token接口进行一个可用性的校验。
我们在 gateway里边，如果使用 check_token接口进行校验，通过的就可以放行，不同过的就拦截，可以做到全局的
token校验和拦截。
问：spring cloud gateway里，你要用这种check_token接口校验方式吗？会用，我会介绍两种gateway的token校验
方式，check_token接口这个是命令式编程（调用接口，等待返回结果，rest形式发送请求，等待结果，阻塞式的）；还会
使用另外一种方式，响应式编程（webflux， Mono<Object>, netty 的refactor模型就是响应式编程的基础）
我们完成了oauth2.0的校验以后，我们就会通过gateway集成 oauth2.0（2种方式。）
问：那种方式校长觉得更好呢？ 响应式编程，异步非阻塞，这个东西会提高我们的访问效率，但是只针对io密集型的，并且
IO等待时间长的这种场景下会有奇效。那么对于目前和check_token相比较，速度上不好评估，尤其是，我们将来将我们的
token的存放形式存放到redis中，会更加没办法评论这个速度问题
问：你这个用户名和密码还有clientid和secret都是假数据啊，是你给我们的sql里边insert进去的，不行啊，坑人。
答：我们搞完spring cloud gateway集成oauth2后，会直接进入user-service搞：用户名+密码注册登录；手机号+验证
码的注册登录；第三方账号（GITEE）的注册+登录。你说，到时候是不是就有真实的数据了啊？
问：校长啊，你这个 www.baidu.com 太坑啦，我就不明白什么叫注册回调接口。咋办吧
答：用户名+密码注册登录； password
手机号+验证码的注册登录； client_credentials
第三方账号（GITEE）的注册+登录。client_credentials

我会额外为大家演示 code授权码的获取方式，并且注册回调接口(另外一个xxxapp想用咱们的商城账号登录自己的
xxxapp，才需要注册回调接口。)

==============================
1. 我们去重写oauth的UserDetailsService，为了从我们的user表中提取到当前用户的信息。
   覆写 方法 ： public UserDetails loadUserByUsername
2. 去实现我们的验证服务配置类（抽象）
   public class Oauth2Config extends AuthorizationServerConfigurerAdapter
3. 去实现我们的 web security类（抽象）
   public class WebSecurityConfig extends WebSecurityConfigurerAdapter

==================================
POST MAN 验证
