https://github.com/TyCoding/springboot-seckill
这个项目中学到的是mybatis使用的另一种方式，不一定要写resource/mapper，以及Redis(存密码)和rabbitmq(有点类似Handler的message队列或者EventBus的机制)的初步使用



要运行该项目，要先安装MySQL，Redis，rabbitmq
MySQL：安装包安装，看我的百家号文章有写，账号root密码66666666
redis：brew install redis 不需要设置账号密码，默认为guest
rabbitmq: brew install rabbitmq 不需要账号密码

启动Redis服务：
/usr/local/bin/redis-server /usr/local/etc/redis.conf
判断是否开启了Redis服务很简单，看终端有提示

启动rabbitmq服务：
cd /usr/local/Cellar/rabbitmq/3.9.13/sbin 
sudo ./rabbitmq-server
判断是否启动rabbitmq服务也是看终端，或者浏览器输入http://localhost:15672/是否能打开，默认账号密码都是guest
或者电脑密码


项目访问port一般是8080除非自己在application.properties上配置成其它
也可以在编译log看出来：
2022-03-01 13:57:58.604  INFO 78711 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)


看项目的访问入口，可以看controller文件夹，经测试入口是http://localhost:8080/login/to_login

GitHub下载的项目，自带有点内容的.mysql，测试账号18181818181密码123456


运行出错：
1、java.lang.IllegalStateException: Cannot load configuration class: com.jesper.seckill.MainApplication
解决方法：https://blog.csdn.net/qq_40147985/article/details/120729354 将项目的jdk1.6改成jdk1.8

2、Error starting ApplicationContext. To display the auto-configuration report re-run your application
解决方法：一般是application.preperties或pom.xml的配置上有问题，比如这个项目，就是MySQL配置上的问题，修改两个地方即可：
#spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
	<!-- 	<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
		</dependency> -->
		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<version>8.0.27</version>
			<scope>runtime</scope>
		</dependency>




