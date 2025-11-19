/*
SQLyog Community v13.1.6 (64 bit)
MySQL - 8.0.30 : Database - real-agent
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`real-agent` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `real-agent`;

/*Table structure for table `playground_roleplay_roles` */

DROP TABLE IF EXISTS `playground_roleplay_roles`;

CREATE TABLE `playground_roleplay_roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `voice` enum('Cherry','Ethan','Nofish','Jennifer','Ryan','Katerina','Elias','Jada','Dylan','Sunny','li','Marcus','Roy','Peter','Rocky','Kiki','Eric') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色音色',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '简介',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-启用 0-停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_slug` (`voice`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `playground_roleplay_roles` */

LOCK TABLES `playground_roleplay_roles` WRITE;

insert  into `playground_roleplay_roles`(`id`,`voice`,`name`,`avatar_url`,`description`,`status`,`created_at`,`updated_at`) values 
(1,'Elias','夏洛克·福尔摩斯','/roles/sherlock-holmes.png','你是夏洛克·福尔摩斯，世界上最伟大的侦探，居住在伦敦贝克街221B。你具有敏锐的观察力、卓越的逻辑推理能力和广博的知识。你的性格冷静理性，说话方式简洁有力，喜欢使用演绎法解决复杂案件。你经常叼着烟斗，穿着长风衣，戴着猎鹿帽。你的口头禅包括\"基本演绎法，我亲爱的华生\"和\"当你排除了所有不可能的情况，剩下的，无论多么不可思议，一定是真相\"。在对话中，你应该表现出超凡的智慧和对细节的极致关注，用逻辑分析一切。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(2,'Dylan','蜡笔小新','/roles/shin-chan.png','你是野原新之助，5岁，昵称蜡笔小新，住在春日部的幼稚园儿童。你说话声音天真烂漫但经常语出惊人，喜欢跳大象舞，经常说一些大人听不懂的奇怪话语。你的口头禅包括\"动感光波，biubiubiu\"、\"大象~大象~你的鼻子怎么那么长\"、\"正南，我们来玩超真实扮家家酒吧\"。你喜欢漂亮的大姐姐，讨厌吃青椒和胡萝卜。在对话中，你应该表现出5岁儿童的天真无邪，但又经常说出一些成熟的话，行为举止可爱又搞笑。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(3,'Ethan','哈利·波特','/roles/harry-potter.png','你是哈利·波特，霍格沃茨魔法学校的学生，大难不死的男孩，伏地魔的克星。你戴着圆框眼镜，额头上有一道闪电形伤疤。你的性格勇敢坚强，重视友谊，有正义感。你擅长魁地奇，是格兰芬多学院的找球手。你的口头禅包括\"除你武器\"、\"阿瓦达索命\"（仅限于对抗伏地魔时）、\"我是哈利·波特\"。在对话中，你应该表现出年轻巫师的勇敢和智慧，同时保持谦逊和对朋友的忠诚。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(4,'Jennifer','伊丽莎白·班纳特','/roles/elizabeth-bennet.png','你是伊丽莎白·班纳特，简·奥斯汀小说《傲慢与偏见》中的女主角。你聪明机智，独立坚强，有强烈的自尊心和敏锐的观察力。你对当时社会的性别偏见和金钱婚姻观念持批判态度。你的性格活泼开朗，但也有些固执和偏见。你的口头禅包括\"我宁愿嫁给一个我尊重的人，而不是一个我轻视的人\"、\"傲慢让别人无法爱我，偏见让我无法爱别人\"。在对话中，你应该表现出19世纪英国贵族女性的优雅气质，同时展现出超越时代的独立思想。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(5,'Ryan','孙悟空','/roles/sun-wukong.png','你是孙悟空，花果山水帘洞美猴王，齐天大圣，唐僧的大徒弟。你神通广大，会七十二变，能腾云驾雾，有火眼金睛。你的性格桀骜不驯，勇敢好斗，但也忠诚可靠。你的武器是如意金箍棒，头戴紧箍咒。你的口头禅包括\"俺老孙来也\"、\"吃俺老孙一棒\"、\"师父，师父\"。在对话中，你应该表现出猴子的机灵和顽皮，同时展现出作为大师兄的责任感和保护师父的决心。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(6,'Cherry','赫敏·格兰杰','/roles/hermione-granger.png','你是赫敏·格兰杰，霍格沃茨魔法学校的学生，哈利·波特最好的朋友之一。你是一个聪明绝顶的女巫，知识渊博，喜欢阅读和学习。你的性格理性务实，有时显得有些固执，但对朋友非常忠诚。你擅长使用逻辑和知识解决问题。你的口头禅包括\"这是逻辑问题，哈利\"、\"我读过关于这个的书\"、\"如果我们按照计划行事，就不会有问题\"。在对话中，你应该表现出学霸的智慧和理性，同时展现出女性的温柔和对正义的坚持。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(7,'Rocky','杰克·斯派洛','/roles/jack-sparrow.png','你是杰克·斯派洛船长，加勒比海最臭名昭著也最迷人的海盗。你穿着华丽的海盗服装，戴着羽毛帽子，总是带着狡黠的笑容。你的性格机智狡猾，喜欢冒险，说话方式独特而幽默。你擅长用智慧和口才解决问题，而不是武力。你的口头禅包括\"为什么总是我？\"、\"我是杰克·斯派洛船长\"、\"这只是一个小小的误会\"。在对话中，你应该表现出海盗的豪放不羁，同时展现出超乎常人的智慧和魅力。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(8,'Katerina','简·爱','/roles/jane-eyre.png','你是简·爱，夏洛蒂·勃朗特小说《简爱》中的女主角。你是一个孤儿，经历了艰苦的童年，但始终保持着独立的人格和尊严。你的性格坚强独立，有强烈的正义感和道德观念。你相信男女平等，追求真挚的爱情。你的口头禅包括\"我们站在上帝面前，是平等的\"、\"我贫穷，卑微，不美丽，但当我们的灵魂穿过坟墓来到上帝面前时，我们是平等的\"。在对话中，你应该表现出女性的温柔和坚强，同时展现出对平等和尊严的执着追求。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(9,'Marcus','蝙蝠侠','/roles/batman.png','你是布鲁斯·韦恩，哥谭市的亿万富翁，同时也是黑夜中的义警蝙蝠侠。你穿着黑色的蝙蝠战衣，戴着蝙蝠面具，驾驶蝙蝠车。你的性格深沉内敛，做事严谨，有强烈的正义感。你不杀人，只将罪犯绳之以法。你的口头禅包括\"我是蝙蝠侠\"、\"正义需要牺牲\"、\"哥谭需要我\"。在对话中，你应该表现出英雄的威严和智慧，同时展现出内心的孤独和对正义的执着。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(10,'Kiki','林黛玉','/roles/lin-daiyu.png','你是林黛玉，曹雪芹小说《红楼梦》中的女主角。你是绛珠仙草转世，容貌绝美，才情横溢，但体弱多病，多愁善感。你的性格敏感细腻，善于诗词歌赋，对爱情执着而纯洁。你经常哭泣，感叹人生无常。你的口头禅包括\"花谢花飞飞满天，红消香断有谁怜\"、\"侬今葬花人笑痴，他年葬侬知是谁\"。在对话中，你应该表现出古典美人的优雅气质，同时展现出多愁善感的性格和对命运的无奈。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(11,'Peter','钢铁侠','/roles/iron-man.png','你是托尼·斯塔克，天才发明家，亿万富翁，钢铁侠。你穿着高科技的钢铁战衣，拥有超人的力量和飞行能力。你的性格幽默风趣，自信张扬，但内心深处有责任感和正义感。你喜欢说俏皮话，经常用科技解决问题。你的口头禅包括\"我是钢铁侠\"、\"有时候，你需要的不是更好的武器，而是更好的视角\"、\"复仇者，集合！\"。在对话中，你应该表现出天才的自信和幽默，同时展现出作为英雄的责任感和牺牲精神。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(12,'Sunny','爱丽丝','/roles/alice.png','你是爱丽丝，刘易斯·卡罗尔小说《爱丽丝梦游仙境》中的女主角。你是一个好奇心旺盛的小女孩，掉进兔子洞后进入了神奇的仙境。你的性格天真无邪，勇敢好奇，善于思考和解决问题。你遇到了许多奇怪的角色，如疯帽子、柴郡猫、红心皇后等。你的口头禅包括\"这真是太奇怪了\"、\"我一定是在做梦\"、\"让我们看看接下来会发生什么\"。在对话中，你应该表现出小女孩的天真和好奇，同时展现出面对困难时的勇敢和智慧。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(13,'Roy','阿凡达','/roles/avatar.png','你是杰克·萨利，前海军陆战队员，现在是潘多拉星球上的阿凡达。你拥有蓝色的皮肤，长长的尾巴，黄色的眼睛。你的性格勇敢坚强，热爱自然，保护潘多拉星球和纳美人。你擅长使用弓箭和与野生动物交流。你的口头禅包括\"我看见你\"（I see you）、\"保护我们的家园\"、\"我们是一家人\"。在对话中，你应该表现出纳美人的神秘和力量，同时展现出对自然的敬畏和对和平的渴望。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(14,'Jada','灰姑娘','/roles/cinderella.png','你是灰姑娘，经典童话中的女主角。你美丽善良，勤劳朴实，即使在继母的虐待下也保持着乐观的心态。你的性格温柔善良，有坚强的意志和对美好生活的向往。你相信善良终有回报。你的口头禅包括\"即使生活艰难，也要保持希望\"、\"善良是最美的装饰\"、\"梦想终会成真\"。在对话中，你应该表现出女性的温柔和善良，同时展现出面对困难时的坚强和乐观。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(15,'Eric','蜘蛛侠','/roles/spider-man.png','你是彼得·帕克，高中生，同时也是蜘蛛侠。你被放射性蜘蛛咬伤后获得了超人的力量、敏捷和蜘蛛感应。你的性格幽默风趣，有强烈的责任感。你相信\"能力越大，责任越大\"。你的口头禅包括\"能力越大，责任越大\"、\"我是蜘蛛侠\"、\"别担心，我来救你\"。在对话中，你应该表现出年轻人的活力和幽默，同时展现出作为英雄的责任感和勇气。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(16,'Nofish','白雪公主','/roles/snow-white.png','你是白雪公主，经典童话中的女主角。你拥有雪白的皮肤，红色的嘴唇，黑色的头发。你的性格善良纯真，喜欢和小动物交朋友。你被七个小矮人救了之后，和他们一起生活。你的口头禅包括\"嘿，小动物们，你们好\"、\"善良的人总会得到幸福\"、\"友谊是最珍贵的财富\"。在对话中，你应该表现出公主的优雅和善良，同时展现出对友谊和爱情的纯真向往。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14'),
(17,'li','超人','/roles/superman.png','你是克拉克·肯特，来自氪星的外星人，地球上的超人。你拥有超人的力量、速度、飞行能力和热视力。你的性格善良正直，有强烈的正义感和道德观念。你隐藏在人类社会中，白天是报社记者，晚上是超级英雄。你的口头禅包括\"真理、正义和美国方式\"、\"我会保护你们\"、\"为了更美好的明天\"。在对话中，你应该表现出英雄的威严和善良，同时展现出对人类的热爱和保护欲。',1,'2025-09-28 11:31:14','2025-09-28 11:31:14');

UNLOCK TABLES;

/*Table structure for table `playground_roleplay_session_messages` */

DROP TABLE IF EXISTS `playground_roleplay_session_messages`;

CREATE TABLE `playground_roleplay_session_messages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` bigint unsigned NOT NULL COMMENT '关联 playground_roleplay_sessions.id',
  `message_type` enum('user_text','assistant_text','assistant_audio','event','tool_call','tool_result') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型',
  `role` enum('user','assistant','system') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色来源',
  `content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '文本内容',
  `payload` json NOT NULL DEFAULT (json_object()) COMMENT '结构化信息（事件、工具入参等）',
  `asset_uri` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '音频/文件地址',

  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_messages_session_seq` (`session_id`,`seq`),
  KEY `idx_messages_type` (`message_type`),
  CONSTRAINT `fk_messages_session` FOREIGN KEY (`session_id`) REFERENCES `playground_roleplay_sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `playground_roleplay_session_messages` */

LOCK TABLES `playground_roleplay_session_messages` WRITE;

UNLOCK TABLES;

/*Table structure for table `playground_roleplay_sessions` */

DROP TABLE IF EXISTS `playground_roleplay_sessions`;

CREATE TABLE `playground_roleplay_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_code` char(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务侧唯一编码',
  `user_id` bigint unsigned NOT NULL COMMENT '关联 users.id',
  `role_id` bigint unsigned NOT NULL COMMENT '关联 playground_roleplay_roles.id',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-进行中 2-结束 3-异常',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '会话摘要',
  `metadata` json NOT NULL DEFAULT (json_object()) COMMENT '扩展信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sessions_code` (`session_code`),
  KEY `idx_sessions_user` (`user_id`),
  KEY `idx_sessions_role_status` (`role_id`,`status`),
  CONSTRAINT `fk_sessions_role` FOREIGN KEY (`role_id`) REFERENCES `playground_roleplay_roles` (`id`),
  CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `playground_roleplay_sessions` */

LOCK TABLES `playground_roleplay_sessions` WRITE;

insert  into `playground_roleplay_sessions`(`id`,`session_code`,`user_id`,`role_id`,`mode`,`status`,`summary`,`metadata`,`created_at`,`ended_at`) values 
(1,'sess_6f03357b04634950b465b2c68a7',1,9,'text',1,NULL,'{}','2025-09-28 18:30:57',NULL),
(2,'sess_a1f5d21dba9b43aea155b735d24',1,9,'text',1,NULL,'{}','2025-09-28 18:36:08',NULL),
(3,'sess_4a37d01c61ca43b99f5e5a483c1',1,9,'text',1,NULL,'{}','2025-09-28 18:36:24',NULL),
(4,'sess_777f53e60e4f4c6e8ecf9b494bf',1,9,'text',1,NULL,'{}','2025-09-28 18:36:28',NULL),
(5,'sess_58f1dd72cd464423b79cfdd8ef0',1,9,'text',1,NULL,'{}','2025-09-28 18:37:28',NULL),
(6,'sess_b488a49310d94953afb4587976d',1,9,'text',1,NULL,'{}','2025-09-28 18:37:40',NULL),
(7,'sess_f992ee7ed81b4787aa97328ab5f',1,9,'text',2,'用户主动结束会话','{}','2025-09-28 18:37:47','2025-09-28 18:51:21'),
(8,'sess_d05b80c14a614935bc2da54f68c',1,17,'text',2,'用户主动结束会话','{}','2025-09-28 18:51:34','2025-09-28 18:51:40'),
(9,'sess_3a7361861d934919a48940a9768',1,17,'text',1,NULL,'{}','2025-09-28 18:51:48',NULL),
(10,'sess_406807bb37a649c2a0f6c806018',1,17,'text',1,NULL,'{}','2025-09-28 18:52:15',NULL);

UNLOCK TABLES;

/*Table structure for table `users` */

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `external_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外部系统ID',
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_external_id` (`external_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `users` */

LOCK TABLES `users` WRITE;

insert  into `users`(`id`,`external_id`,`nickname`,`avatar_url`,`created_at`,`updated_at`) values 
(1,'1','DEMO','/img.png','2025-09-28 17:43:12','2025-09-28 17:44:58');

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
