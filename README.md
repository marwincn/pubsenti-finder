# pubsenti-finder
 
## 项目介绍
使用爬虫周期性地爬取微博热搜榜，获取热搜列表，每条热搜内的热门微博和每条微博的评论。使用文本分类算法对每条评论进行情感分析，将所有微博数据和情感分析的结果存储在Redis里。运行Web服务直观地展示微博数据和情感分析结果。

## 项目结构介绍

### util包
`HttpUtil`类负责调用`okhttp`发送http请求。`RedisUtil`类负责使用`Jedis`连接Redis服务器并返回Jedis实例读写数据到Redis。
### entity包
`Hot`类、`Weibo`类和`Comment`类分别表示热搜、微博和评论的实体类，它们存储了基本的元数据和进行情感分析后的加工数据。
### crawler包
`CrawlTask`类继承了`TimerTask`能够周期性的运行，对微博热搜榜进爬取，进行情感分析，然后将数据存储到Redis。具体的爬取任务由`WeiboParser`类负责，由于微博页面基本都是动态的，请求微博相应的接口返回`Json`数据，然后使用`jackson`框架解析微博元数据。
### classifier包
`Train`类读取语料文件，存储到内存里，调用`HanLP`对文本进行分词处理，并计算每个词的`卡方值`，得到显著的词作为特征。最后生成`Model`类的模型。`Model`使用朴素贝叶斯算法进行文本分类。

`MyClassifier`类组合了`Train`和`Model`类，优先从指定的路径加载序列化的模型文件，如果未获取到模型就使用`Train`重新根据语料计算模型，然后将计算得到的模型序列化到指定路径。`MyClassifier`类的`getScore`方法调用模型对文本进行情感分析，所以实际使用中只会用到`MyClassifier`。

其实`HanLP`已经实现了一个分本算法，参考`HanLPClassifer`类。
### web包
Web服务由`Spring Boot`支持，后端从Redis读取数据（其实数据可以直接保存在内存的数据结构里，因为前期设计时考虑对多次爬取的数据进行横向分析才存使用Redis管理数据，为了方便后续扩展还是继续使用Redis），将数据渲染到`Thyemeleaf`模版中然后返回html。模版存储在`src/main/resources/tenplates`目录下，前端使用`Bootstrap 4`编写。
## 运行环境
* Java 8以上，本项目使用了一些Java 8的特性；
* Maven，下载项目依赖的包；
* Redis，某些数据存储在Redis中；

## 运行准备

### 1.设置Redis服务器地址
找到`src/main/java/util/RedisUtil`，修改`ADDR`为你服务器的ip地址，如果Redis服务器设置了密码就将`AUTH`改为你的密码。

### 2.设置HTTP请求参数
由于微博的限制，未登录的账户只能获取到第一页微博评论。为了爬取更多评论你需要在浏览器中登录`m.weibo.cn`，然后打开浏览器的开发者工具查看你发送的请求里的Cookie，将其复制到`src/main/java/util/HttpUtil`里的`.header("cookie", "")`的第二个参数里。

你还可以将`user-agent`设置成自己浏览器的user-agent标识。

### 3.下载分词所需的数据
本项目使用的分词工具为[HanLP](https://github.com/hankcs/HanLP/tree/1.x)，为了分词更准确它需要一些额外的数据。

下载[data.zip](http://nlp.hankcs.com/download.php?file=data)，解压后将`data`目录复制到`src/main/resources`目录下即可，HanLP会自动在classpath下面找到数据。

### 4.设置模型及语料的路径
我提供了一些基本的语料作为参考，在`src/main/resources/train`目录下。复制模型`weibo-model`的绝对路径到`MyClassifier`类的`MODEL_PATH`（**注意一定要用绝对路径，后续路径也是，因为项目打包成jar后运行的classpath路径不确定，相对路径可能会失效**），运行时如果模型文件存在就不用重新训练模型。

如果想要使用其他的语料训练模型，可以复制停用词表`cn_stopwords.txt`、否定词表`cn_nonwords.txt`、积极情绪语料`pos.txt`和消极情绪语料`neg.txt`的绝对路径到`Train`类相应的路径。

## 运行
* 下载了`pox.xml`中依赖的包后，在IDE中直接运行`Application`类中的main函数。
* 使用`mvn clean package`命令将项目打包成jar，使用`java -jar *.jar`运行。

## 自定义参数
由于微博翻爬虫系统对爬虫的限制非常严重，所以我设置的爬取速度非常慢，每次爬取的微博数量比较少，如果你解决了微博反爬问题，可以自定义这些参数提高爬取效率。
* 在`Application`类设置`Timer`的`period`参数调整定时爬虫任务的周期（默认30分钟运行一次）；
* 在`CrawlTask`类设置一次爬取热搜榜上的热搜个数`HOT_LIST_SIZE`（默认为10条），每条热搜下微博个数`WB_LIST_SIZE`（默认为一条），每条微博评论的页数`CM_LIST_SIZE`（默认为5页，一页20条评论）；
* 在`CrawlTask`类设置每次爬取的数据存在Redis中的过期时间`KEY_EXPIRE_TIME`（默认一小时后过期）。