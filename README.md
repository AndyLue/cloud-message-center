### 资产云消息组件

组件使用需要以下步骤

1.增加包依赖

```
<dependency>
    <groupId>cn.com.zstars</groupId>
    <artifactId>cloud-message-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```



2.在配置文件中，指定消息中心地址

\#本地消费者信息

cloudmessage.name-server=IP:PORT

cloudmessage.consumer-group-name = 消息组唯一标识



3.本地程序监听

注意：消息的TOPIC为：topic_zstar-加上应用的密钥（从应用中心查找）

```
/**
 * @description: 卡片相关消息监听
 * @author: lin
 * @create: 2020/04/08 11:33
 */
@Component
@EnableCloudMessageListener(topic = "topic_zstar-${cloudmessage.app-secret}",
        consumerGroup = "${cloudmessage.consumer-group-name}")
public class BaseConsumerListener implements CloudMessageListener<CloudMessage> {

    @Override
    public void onMessage(CloudMessage messageExt) {
        LogUtils.infoWEB("同步资产云消息");
        //1.各业务系统自行做，幂等（消息可能重复发送需要根据消息id做幂等）
        if (selectMessage(messageExt)) {
            return;
        }
       //2.业务操作，省略
//     String tags = messageExt.getTags().getProperties().get("TAGS");

    }
}
```