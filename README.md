项目架构图
<img width="657" height="782" alt="image" src="https://github.com/user-attachments/assets/a9cd96a8-06d6-4999-bb1f-b5f4a3ae7d21" />

使用JMeter对关键接口（优惠券秒杀）进行压测：

首先测试一人一单，设置100个线程组,所有线程组使用同一个AccessToken模拟同一个用户同时进行100次抢购,结果一百次请求全部成功

<img width="1125" height="190" alt="image" src="https://github.com/user-attachments/assets/d88d8d9d-cbb9-4028-94a9-5daed9fff65b" />

然而查询redis和数据库，订单只增加了一个，库存也只减少了一个

<img width="1175" height="285" alt="image" src="https://github.com/user-attachments/assets/d808c40b-ccd2-4b35-b221-baa54edae04d" />

系统接收到订单请求后，将订单全部存储到消息队列，消息队列只处理了其中的一条,幂等性地丢弃了后 99 条
结果显示，100次高并发请求全部返回成功（HTTP 200），但后端持久化层（Redis/MySQL）仅产生一条有效数据。证明了系统在业务层面实现了严格的幂等性控制。


秒杀接口并发测试 使用 JMeter CLI 模式
设置优惠券库存500，1000用户并发抢购
用户模拟：生成500组真实 AccessToken 写入CSV文件

<img width="1180" height="164" alt="image" src="https://github.com/user-attachments/assets/30e2b04c-3936-47e6-b92c-9dd46bab35ad" />

异常率50%，符合用户与优惠券的比值，QPS达到730




