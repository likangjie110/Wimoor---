-- --------------------------------------------------------
-- 主机:                           wimoor.rwlb.rds.aliyuncs.com
-- 服务器版本:                        8.0.36 - Source distribution
-- 服务器操作系统:                      Linux
-- HeidiSQL 版本:                  12.6.0.6765
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- 导出  表 db_admin.t_sys_permission 结构
CREATE TABLE IF NOT EXISTS `t_sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '权限名称',
  `menu_id` int DEFAULT NULL COMMENT '菜单模块ID\r\n',
  `url_perm` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'URL权限标识',
  `btn_perm` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '按钮权限标识',
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id` (`id`,`name`),
  KEY `id_2` (`id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1697438074115891206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='权限表';

-- 正在导出表  db_admin.t_sys_permission 的数据：~14 rows (大约)
INSERT IGNORE INTO `t_sys_permission` (`id`, `name`, `menu_id`, `url_perm`, `btn_perm`, `gmt_create`, `gmt_modified`) VALUES
	(1590216687451979778, '库位编辑', 7, 'POST:/erp/v1/warehouse/shelf/add', 'erp:wh:shelf:add', '2022-11-09 13:36:18', '2022-11-09 13:36:18'),
	(1627577448918474754, '删除1688授权', 26, 'GET:/erp/api/v1/purchase1688/delete', 'erp:purchase:open1688:delete', '2023-02-20 15:54:37', '2023-02-20 15:54:37'),
	(1628579022289592322, '导出', 98, 'GET:/erp/api/v1/material/downloadMaterialList', 'erp:material:download', '2023-02-23 10:14:31', '2023-02-23 10:14:31'),
	(1628580738779144193, '导出', 44, 'GET:/erp/api/v1/customer/downloadCustomerList', 'erp:customer:download', '2023-02-23 10:21:20', '2023-02-23 10:21:20'),
	(1648888310276505601, '本地已完成', 27, 'GET:/amazon/api/v1/shipForm/marketShipped', 'amz:ship:localdone', '2023-04-20 11:16:23', '2023-04-20 11:16:23'),
	(1648888789660286977, '删除货件', 27, 'GET:/erp/api/v1/shipForm/disableShipment', 'amz:ship:delete', '2023-04-20 11:18:17', '2023-04-20 11:18:17'),
	(1656144687886938114, '确认出库(含耗材)', 27, 'POST:/erp/api/v1/shipForm/saveInventoryConsumable', 'amz:ship:conship', '2023-05-10 11:50:38', '2023-05-10 11:50:38'),
	(1656145342726844417, '确认出库', 27, 'GET:/amazon/api/v1/shipForm/marketAmazonShipped', 'amz:ship:shipdone', '2023-05-10 11:53:14', '2023-05-10 11:53:14'),
	(1697432618920222722, '添加加工单', 70, 'POST:/erp/api/v1/assembly/saveData', 'erp:po:ass:add', '2023-09-01 10:14:08', '2023-09-01 10:14:08'),
	(1697433107619553281, '入库操作', 70, 'GET:/erp/api/v1/assembly/saveRecord', 'erp:po:ass:in', '2023-09-01 10:16:04', '2023-09-01 10:16:04'),
	(1697437139805315073, '修改单据数量', 70, 'GET:/erp/api/v1/assembly/changeAssAmount', 'erp:po:ass:update', '2023-09-01 10:32:06', '2023-09-01 10:32:06'),
	(1697438074115891202, '终止单据', 70, 'GET:/erp/api/v1/assembly/stopAssemblyEvent', 'erp:po:ass:stop', '2023-09-01 10:35:48', '2023-09-01 10:35:48'),
	(1697438074115891203, '查看产品信息', 1012151, 'POST:/quote/api/v1/quote/supplier/addsupplier', 'erp:pi:list:item', '2023-09-01 10:35:48', '2023-09-01 10:35:48'),
	(1697438074115891205, '查看物流连接', 1012153, 'POST:/quote/api/v1/quote/supplier/addsupplier', 'erp:pi:supplier:link', '2023-09-01 10:35:48', '2023-09-01 10:35:48'),
	(1697438074115891206, '查看Ozon授权', 1012186, 'GET:/ozon/api/v1/auth/list', 'ozon:auth:list', '2026-03-23 15:30:00', '2026-03-23 15:30:00'),
	(1697438074115891207, '绑定Ozon授权', 1012186, 'POST:/ozon/api/v1/auth/bind', 'ozon:auth:bind', '2026-03-23 15:30:00', '2026-03-23 15:30:00'),
	(1697438074115891208, '测试Ozon连接', 1012186, 'GET:/ozon/api/v1/auth/ping', 'ozon:auth:ping', '2026-03-23 15:30:00', '2026-03-23 15:30:00'),
	(1697438074115891209, '停用Ozon授权', 1012186, 'POST:/ozon/api/v1/auth/disable', 'ozon:auth:disable', '2026-03-23 15:30:00', '2026-03-23 15:30:00'),
	(1697438074115891210, '轮换Ozon密钥', 1012186, 'POST:/ozon/api/v1/auth/rotateKey', 'ozon:auth:rotate', '2026-03-23 15:30:00', '2026-03-23 15:30:00'),
	(1697438074115891211, '查看Ozon商品映射', 1012187, 'GET:/ozon/api/v1/product/list', 'ozon:product:list', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
	(1697438074115891212, '导入Ozon商品草稿', 1012187, 'POST:/ozon/api/v1/product/importDraft', 'ozon:product:import', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
	(1697438074115891213, '保存Ozon商品映射', 1012187, 'POST:/ozon/api/v1/product/map/save', 'ozon:product:save', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
	(1697438074115891244, '查看Ozon商品发布任务历史', 1012187, 'GET:/ozon/api/v1/product/publish/task/list', 'ozon:product:publish:task:list', '2026-04-11 10:10:00', '2026-04-11 10:10:00'),
	(1697438074115891214, '查看Ozon库存快照', 1012188, 'GET:/ozon/api/v1/stock/snapshot/list', 'ozon:stock:list', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
		(1697438074115891215, '推送Ozon库存', 1012188, 'POST:/ozon/api/v1/stock/push', 'ozon:stock:push', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
		(1697438074115891216, '查看Ozon价格快照', 1012189, 'GET:/ozon/api/v1/price/snapshot/list', 'ozon:price:list', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
		(1697438074115891217, '推送Ozon价格', 1012189, 'POST:/ozon/api/v1/price/push', 'ozon:price:push', '2026-03-23 20:30:00', '2026-03-23 20:30:00'),
		(1697438074115891242, '查看Ozon库存任务', 1012188, 'GET:/ozon/api/v1/stock/task/list', 'ozon:stock:task', '2026-04-11 09:55:00', '2026-04-11 09:55:00'),
		(1697438074115891243, '查看Ozon价格任务', 1012189, 'GET:/ozon/api/v1/price/task/list', 'ozon:price:task', '2026-04-11 09:55:00', '2026-04-11 09:55:00'),
		(1697438074115891226, '导入Ozon财务报表', 1012193, 'POST:/ozon/api/v1/finance/import', 'ozon:finance:import', '2026-03-26 12:10:00', '2026-03-26 12:10:00'),
		(1697438074115891227, '查看Ozon财务任务', 1012193, 'GET:/ozon/api/v1/finance/task/list', 'ozon:finance:task', '2026-03-26 12:10:00', '2026-03-26 12:10:00'),
		(1697438074115891228, '查看Ozon财务明细', 1012193, 'GET:/ozon/api/v1/finance/transaction/list', 'ozon:finance:transaction', '2026-03-26 12:10:00', '2026-03-26 12:10:00'),
		(1697438074115891229, '查看Ozon财务原文', 1012193, 'GET:/ozon/api/v1/finance/task/raw', 'ozon:finance:raw', '2026-03-26 12:10:00', '2026-03-26 12:10:00'),
	(1697438074115891230, '导入Ozon聊天', 1012194, 'POST:/ozon/api/v1/chat/import', 'ozon:chat:import', '2026-03-26 20:20:00', '2026-03-26 20:20:00'),
	(1697438074115891231, '查看Ozon会话', 1012194, 'GET:/ozon/api/v1/chat/session/list', 'ozon:chat:session', '2026-03-26 20:20:00', '2026-03-26 20:20:00'),
	(1697438074115891232, '查看Ozon消息', 1012194, 'GET:/ozon/api/v1/chat/message/list', 'ozon:chat:message', '2026-03-26 20:20:00', '2026-03-26 20:20:00'),
	(1697438074115891233, '记录Ozon回复', 1012194, 'POST:/ozon/api/v1/chat/reply/record', 'ozon:chat:reply', '2026-03-26 20:20:00', '2026-03-26 20:20:00'),
	(1697438074115891253, '查看Ozon回复审计', 1012194, 'GET:/ozon/api/v1/chat/reply/audit/list', 'ozon:chat:reply:audit', '2026-04-11 17:10:00', '2026-04-11 17:10:00'),
	(1697438074115891234, '导入Ozon广告', 1012195, 'POST:/ozon/api/v1/ads/import', 'ozon:ads:import', '2026-03-26 22:30:00', '2026-03-26 22:30:00'),
	(1697438074115891254, '查看Ozon广告账号', 1012195, 'GET:/ozon/api/v1/ads/account/list', 'ozon:ads:account', '2026-04-11 17:10:00', '2026-04-11 17:10:00'),
	(1697438074115891235, '查看Ozon广告活动', 1012195, 'GET:/ozon/api/v1/ads/campaign/list', 'ozon:ads:campaign', '2026-03-26 22:30:00', '2026-03-26 22:30:00'),
	(1697438074115891236, '查看Ozon广告报表', 1012195, 'GET:/ozon/api/v1/ads/report/list', 'ozon:ads:report', '2026-03-26 22:30:00', '2026-03-26 22:30:00'),
	(1697438074115891237, '查看Ozon广告汇总', 1012195, 'GET:/ozon/api/v1/ads/summary', 'ozon:ads:summary', '2026-03-26 22:30:00', '2026-03-26 22:30:00'),
	(1697438074115891255, '记录Ozon广告同步意图', 1012195, 'POST:/ozon/api/v1/ads/sync/intent', 'ozon:ads:sync:intent', '2026-04-11 17:10:00', '2026-04-11 17:10:00'),
	(1697438074115891218, '查看Ozon订单', 1012190, 'GET:/ozon/api/v1/posting/list', 'ozon:posting:list', '2026-03-23 22:50:00', '2026-03-23 22:50:00'),
	(1697438074115891245, '查看Ozon订单详情', 1012190, 'GET:/ozon/api/v1/posting/detail', 'ozon:posting:detail', '2026-04-11 10:10:00', '2026-04-11 10:10:00'),
	(1697438074115891219, '同步Ozon订单', 1012190, 'POST:/ozon/api/v1/posting/sync', 'ozon:posting:sync', '2026-03-23 22:50:00', '2026-03-23 22:50:00'),
	(1697438074115891220, '重试Ozon桥接', 1012190, 'POST:/ozon/api/v1/posting/retryOne', 'ozon:posting:retry', '2026-03-25 15:30:00', '2026-03-25 15:30:00'),
	(1697438074115891246, '查看Ozon售后详情', 1012190, 'GET:/ozon/api/v1/posting/aftersale/detail', 'ozon:posting:aftersale:detail', '2026-04-11 11:00:00', '2026-04-11 11:00:00'),
	(1697438074115891247, '保存Ozon包裹记录', 1012190, 'POST:/ozon/api/v1/posting/aftersale/package/save', 'ozon:posting:package:save', '2026-04-11 11:00:00', '2026-04-11 11:00:00'),
	(1697438074115891248, '保存Ozon退货记录', 1012190, 'POST:/ozon/api/v1/posting/aftersale/return/save', 'ozon:posting:return:save', '2026-04-11 11:00:00', '2026-04-11 11:00:00'),
	(1697438074115891249, '保存Ozon取消记录', 1012190, 'POST:/ozon/api/v1/posting/aftersale/cancellation/save', 'ozon:posting:cancellation:save', '2026-04-11 11:00:00', '2026-04-11 11:00:00'),
	(1697438074115891221, '推送Ozon追踪号', 1012190, 'POST:/ozon/api/v1/shipment/pushTracking', 'ozon:shipment:tracking', '2026-03-25 18:05:00', '2026-03-25 18:05:00'),
	(1697438074115891222, '查看Ozon任务', 1012191, 'GET:/ozon/api/v1/task/list', 'ozon:task:list', '2026-03-26 09:30:00', '2026-03-26 09:30:00'),
	(1697438074115891223, '查看Ozon错误', 1012192, 'GET:/ozon/api/v1/error/list', 'ozon:error:list', '2026-03-26 10:45:00', '2026-03-26 10:45:00'),
	(1697438074115891224, '重试Ozon错误', 1012192, 'POST:/ozon/api/v1/error/retryOne', 'ozon:error:retry', '2026-03-26 10:45:00', '2026-03-26 10:45:00'),
	(1697438074115891225, '忽略Ozon错误', 1012192, 'POST:/ozon/api/v1/error/ignore', 'ozon:error:ignore', '2026-03-26 10:45:00', '2026-03-26 10:45:00'),
	(1697438074115891238, '查看Ozon功能开关', 1012186, 'GET:/ozon/api/v1/meta/features', 'ozon:meta:features', '2026-04-10 21:30:00', '2026-04-10 21:30:00'),
	(1697438074115891239, '查看Ozon仓库列表', 1012186, 'GET:/ozon/api/v1/seller/warehouse/list', 'ozon:seller:warehouse:list', '2026-04-11 00:30:00', '2026-04-11 00:30:00'),
	(1697438074115891240, '查看Ozon配送方式', 1012186, 'GET:/ozon/api/v1/seller/deliveryMethod/list', 'ozon:seller:delivery:list', '2026-04-11 00:30:00', '2026-04-11 00:30:00'),
	(1697438074115891241, '保存Ozon配送方式', 1012186, 'POST:/ozon/api/v1/seller/deliveryMethod/save', 'ozon:seller:delivery:save', '2026-04-11 00:30:00', '2026-04-11 00:30:00'),
	(1697438074115891250, '查看Ozon运维摘要', 1012191, 'GET:/ozon/api/v1/ops/summary', 'ozon:ops:summary', '2026-04-11 16:20:00', '2026-04-11 16:20:00'),
	(1697438074115891251, '查看Ozon接口日志', 1012192, 'GET:/ozon/api/v1/ops/api-log/list', 'ozon:ops:api-log:list', '2026-04-11 16:20:00', '2026-04-11 16:20:00'),
	(1697438074115891252, '查看Ozon操作审计', 1012192, 'GET:/ozon/api/v1/ops/operation-audit/list', 'ozon:ops:operation-audit:list', '2026-04-11 16:20:00', '2026-04-11 16:20:00');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
