#!/usr/bin/env python3
"""批量创建数据库题库，覆盖所有题型"""
import requests
import json

BASE = "http://localhost:8080/api"

# 先注册教师用户，再登录获取 token
requests.post(f"{BASE}/user/register", json={
    "username": "teacher", "password": "123456", "role": "teacher"
})
login_resp = requests.post(f"{BASE}/user/login", json={
    "username": "teacher", "password": "123456", "role": "teacher"
})
token = login_resp.json()["data"]["token"]
print(f"Token obtained, length={len(token)}")

headers = {
    "Content-Type": "application/json;charset=UTF-8",
    "Authorization": f"Bearer {token}"
}

def create(data):
    r = requests.post(f"{BASE}/teacher/questions", headers=headers, json=data)
    result = r.json()
    status = "OK" if result["code"] == 200 else f"FAIL: {result['message']}"
    print(f"  [{status}] {data['type']}: {data['content'][:40]}...")
    return r.json()

# ============================================================
# 一、单选题 (single_choice) — 15 道
# ============================================================
singles = [
    {
        "type": "single_choice",
        "content": "在 SQL 中，用于删除表中所有行但保留表结构的命令是？",
        "options": '["A. DELETE FROM table_name", "B. DROP TABLE table_name", "C. TRUNCATE TABLE table_name", "D. ALTER TABLE table_name"]',
        "answer": "C",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "数据库事务 ACID 特性中，确保事务要么全部执行、要么全部不执行的是？",
        "options": '["A. 一致性 Consistency", "B. 隔离性 Isolation", "C. 持久性 Durability", "D. 原子性 Atomicity"]',
        "answer": "D",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "WHERE 子句和 HAVING 子句的主要区别是什么？",
        "options": '["A. WHERE 用于分组前过滤行，HAVING 用于分组后过滤组", "B. 两者功能完全相同", "C. HAVING 用于分组前过滤，WHERE 用于分组后过滤", "D. WHERE 只能用于 SELECT 语句"]',
        "answer": "A",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "数据库设计中第三范式（3NF）主要解决什么问题？",
        "options": '["A. 消除部分函数依赖", "B. 消除传递函数依赖", "C. 消除多值依赖", "D. 消除所有数据冗余"]',
        "answer": "B",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "SQL 中 INNER JOIN 的作用是什么？",
        "options": '["A. 返回左表中的所有行", "B. 返回两表中满足连接条件的行", "C. 返回两表的笛卡尔积", "D. 返回右表中的所有行"]',
        "answer": "B",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "以下哪个不是数据库索引的优点？",
        "options": '["A. 加快数据查询速度", "B. 加速 ORDER BY 排序", "C. 减少表的存储空间", "D. 保证数据唯一性（唯一索引）"]',
        "answer": "C",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "哪个事务隔离级别可以防止脏读、不可重复读和幻读？",
        "options": '["A. READ UNCOMMITTED", "B. READ COMMITTED", "C. REPEATABLE READ", "D. SERIALIZABLE"]',
        "answer": "D",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "SQL 中 UNION 和 UNION ALL 的区别是什么？",
        "options": '["A. UNION 会排序，UNION ALL 不会", "B. UNION 去重复行，UNION ALL 保留所有行", "C. UNION ALL 去重复行，UNION 保留所有行", "D. 两者功能完全相同"]',
        "answer": "B",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "E-R 图中菱形表示什么？",
        "options": '["A. 实体", "B. 属性", "C. 联系", "D. 主键"]',
        "answer": "C",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "下列 SQL 语句中，哪条用于向表中插入新数据？",
        "options": '["A. INSERT INTO table_name VALUES (...)", "B. UPDATE table_name SET ...", "C. ADD INTO table_name (...)", "D. CREATE ROW IN table_name (...)"]',
        "answer": "A",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "SQL 中用于创建数据库的命令是？",
        "options": '["A. CREATE DATABASE db_name", "B. NEW DATABASE db_name", "C. ADD DATABASE db_name", "D. MAKE DATABASE db_name"]',
        "answer": "A",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "数据库范式中第二范式（2NF）是在第几范式基础上消除部分函数依赖？",
        "options": '["A. 第一范式 1NF", "B. 第三范式 3NF", "C. BCNF", "D. 第四范式 4NF"]',
        "answer": "A",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "视图（View）在数据库中的作用是什么？",
        "options": '["A. 存储物理数据", "B. 提供数据的虚拟表，简化查询", "C. 替代索引提高查询速度", "D. 存储存储过程"]',
        "answer": "B",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "SQL 中 COUNT(*) 和 COUNT(column) 的区别是什么？",
        "options": '["A. 完全一样", "B. COUNT(*) 计数所有行，COUNT(column) 忽略 NULL 值", "C. COUNT(column) 计数所有行，COUNT(*) 忽略 NULL 值", "D. COUNT(*) 比 COUNT(column) 慢"]',
        "answer": "B",
        "courseId": 1
    },
    {
        "type": "single_choice",
        "content": "数据库中的主键（Primary Key）必须满足什么条件？",
        "options": '["A. 可以为 NULL", "B. 可以重复", "C. 唯一且不能为 NULL", "D. 必须是自增的"]',
        "answer": "C",
        "courseId": 1
    },
]

print("=" * 50)
print("一、单选题 (single_choice) — 15 道")
for q in singles:
    create(q)

# ============================================================
# 二、多选题 (multi_choice) — 10 道
# ============================================================
multis = [
    {
        "type": "multi_choice",
        "content": "数据库事务 ACID 特性包括哪些？（多选）",
        "options": '["A. 原子性 Atomicity", "B. 一致性 Consistency", "C. 隔离性 Isolation", "D. 持久性 Durability", "E. 安全性 Security"]',
        "answer": "A,B,C,D",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些是 SQL 中的聚合函数？（多选）",
        "options": '["A. COUNT()", "B. SUM()", "C. AVG()", "D. WHERE()", "E. MAX()"]',
        "answer": "A,B,C,E",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些属于 DML（数据操纵语言）？（多选）",
        "options": '["A. INSERT", "B. CREATE", "C. UPDATE", "D. DELETE", "E. GRANT"]',
        "answer": "A,C,D",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "数据库索引的类型包括哪些？（多选）",
        "options": '["A. B+树索引", "B. 哈希索引", "C. 全文索引", "D. 唯一索引", "E. 复合索引"]',
        "answer": "A,B,C,D,E",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些是关系数据库的常见约束？（多选）",
        "options": '["A. PRIMARY KEY", "B. FOREIGN KEY", "C. UNIQUE", "D. CHECK", "E. NOT NULL"]',
        "answer": "A,B,C,D,E",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "SQL JOIN 的类型包括以下哪些？（多选）",
        "options": '["A. INNER JOIN", "B. LEFT JOIN", "C. RIGHT JOIN", "D. FULL JOIN", "E. CROSS JOIN"]',
        "answer": "A,B,C,D,E",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些措施可以提高数据库查询性能？（多选）",
        "options": '["A. 为常用查询列创建索引", "B. 避免使用 SELECT *", "C. 合理设计表结构减少冗余", "D. 使用连接池管理数据库连接", "E. 对所有列都建索引"]',
        "answer": "A,B,C,D",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些是数据库中常见的并发控制问题？（多选）",
        "options": '["A. 脏读", "B. 不可重复读", "C. 幻读", "D. 丢失更新", "E. 死锁"]',
        "answer": "A,B,C,D,E",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "E-R 模型的基本元素包括哪些？（多选）",
        "options": '["A. 实体", "B. 属性", "C. 联系", "D. 索引", "E. 视图"]',
        "answer": "A,B,C",
        "courseId": 1
    },
    {
        "type": "multi_choice",
        "content": "以下哪些属于 DDL（数据定义语言）？（多选）",
        "options": '["A. CREATE TABLE", "B. ALTER TABLE", "C. DROP TABLE", "D. SELECT", "E. TRUNCATE TABLE"]',
        "answer": "A,B,C,E",
        "courseId": 1
    },
]

print()
print("二、多选题 (multi_choice) — 10 道")
for q in multis:
    create(q)

# ============================================================
# 三、判断题 (true_false) — 10 道
# ============================================================
tfs = [
    {
        "type": "true_false",
        "content": "在 SQL 中，NULL 值表示空字符串。",
        "options": '["正确", "错误"]',
        "answer": "错误",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "数据库的 BCNF 范式比 3NF 要求更严格。",
        "options": '["正确", "错误"]',
        "answer": "正确",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "一个表可以有多个主键。",
        "options": '["正确", "错误"]',
        "answer": "错误",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "在 MySQL InnoDB 引擎中，事务默认是自动提交的。",
        "options": '["正确", "错误"]',
        "answer": "正确",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "外键约束用于保证引用完整性。",
        "options": '["正确", "错误"]',
        "answer": "正确",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "索引越多，数据库查询性能一定越好。",
        "options": '["正确", "错误"]',
        "answer": "错误",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "子查询可以嵌套在 SELECT、INSERT、UPDATE、DELETE 语句中。",
        "options": '["正确", "错误"]',
        "answer": "正确",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "GROUP BY 子句用于对结果集进行排序。",
        "options": '["正确", "错误"]',
        "answer": "错误",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "视图是一种物理表，会占用额外的存储空间。",
        "options": '["正确", "错误"]',
        "answer": "错误",
        "courseId": 1
    },
    {
        "type": "true_false",
        "content": "LEFT JOIN 返回左表所有行，右表不匹配的行填充 NULL。",
        "options": '["正确", "错误"]',
        "answer": "正确",
        "courseId": 1
    },
]

print()
print("三、判断题 (true_false) — 10 道")
for q in tfs:
    create(q)

# ============================================================
# 四、填空题 (fill_blank) — 10 道
# ============================================================
fills = [
    {
        "type": "fill_blank",
        "content": "SQL 语句中用于分组的关键字是 ______，用于分组后筛选的关键字是 ______。",
        "options": None,
        "answer": "GROUP BY,HAVING",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "数据库事务的四大特性简称为 ______，其中 I 代表 ______。",
        "options": None,
        "answer": "ACID,隔离性",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "SQL 中用于去重的关键字是 ______，用于限制返回行数的关键字是 ______。",
        "options": None,
        "answer": "DISTINCT,LIMIT（或 TOP 或 FETCH）",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "数据库三大范式：1NF 要求 ______，2NF 要求消除 ______，3NF 要求消除 ______。",
        "options": None,
        "answer": "属性不可再分,部分函数依赖,传递函数依赖",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "在 SQL 中，默认的排序方式是 ______（升序/降序），对应关键字是 ______。",
        "options": None,
        "answer": "升序,ASC",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "数据库并发控制的两种主要机制是 ______ 和 ______。",
        "options": None,
        "answer": "锁机制,MVCC（多版本并发控制）",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "SQL 中连接查询的四种类型是 ______、______、______、______。",
        "options": None,
        "answer": "INNER JOIN,LEFT JOIN,RIGHT JOIN,FULL JOIN",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "数据库设计中，______ 是唯一标识表中每一行的字段，______ 用于建立表与表之间的关联。",
        "options": None,
        "answer": "主键,外键",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "MySQL 中查看表结构的命令是 ______，查看数据库列表的命令是 ______。",
        "options": None,
        "answer": "DESC table_name（或 DESCRIBE）,SHOW DATABASES",
        "courseId": 1
    },
    {
        "type": "fill_blank",
        "content": "SQL 注入攻击是因为程序未对 ______ 进行过滤，预防措施包括使用 ______。",
        "options": None,
        "answer": "用户输入,参数化查询（或预编译语句）",
        "courseId": 1
    },
]

print()
print("四、填空题 (fill_blank) — 10 道")
for q in fills:
    create(q)

# ============================================================
# 五、简答题 (short_answer) — 5 道
# ============================================================
shorts = [
    {
        "type": "short_answer",
        "content": "请简述数据库事务的 ACID 特性分别代表什么含义，并举例说明。",
        "options": None,
        "answer": None,
        "referenceAnswer": "ACID 含义：\n1. 原子性(Atomicity)：事务是不可分割的最小单元，要么全部成功，要么全部回滚。例如银行转账，A扣钱和B加钱必须同时成功或同时失败。\n2. 一致性(Consistency)：事务执行前后，数据库都处于一致性状态。例如转账前后总金额不变。\n3. 隔离性(Isolation)：并发事务之间互不干扰，每个事务都感觉不到其他事务的存在。\n4. 持久性(Durability)：事务一旦提交，对数据库的改变是永久的，即使系统崩溃也不会丢失。",
        "courseId": 1
    },
    {
        "type": "short_answer",
        "content": "请解释什么是数据库索引，并说明索引的优缺点。",
        "options": None,
        "answer": None,
        "referenceAnswer": "数据库索引是一种用于加速数据检索的辅助数据结构，类似于书籍的目录。\n\n优点：\n1. 大幅提高查询速度，特别是对大表\n2. 加速 ORDER BY 和 GROUP BY 操作\n3. 唯一索引可以保证数据唯一性\n\n缺点：\n1. 索引需要额外的磁盘存储空间\n2. INSERT、UPDATE、DELETE 操作时需要同时维护索引，降低写入性能\n3. 过多的索引可能导致查询优化器选择错误的执行计划",
        "courseId": 1
    },
    {
        "type": "short_answer",
        "content": "请简述数据库三大范式（1NF、2NF、3NF）的核心要求。",
        "options": None,
        "answer": None,
        "referenceAnswer": "第一范式（1NF）：每个列都是不可再分的原子值，即表中不能有重复组或多值列。\n\n第二范式（2NF）：在满足 1NF 的基础上，所有非主属性完全函数依赖于主键，消除部分函数依赖。即不能有只依赖于部分主键的非主属性。\n\n第三范式（3NF）：在满足 2NF 的基础上，消除传递函数依赖，即非主属性不能依赖于其他非主属性，必须直接依赖于主键。",
        "courseId": 1
    },
    {
        "type": "short_answer",
        "content": "请解释 LEFT JOIN、INNER JOIN 和 CROSS JOIN 的区别，并给出示例场景。",
        "options": None,
        "answer": None,
        "referenceAnswer": "INNER JOIN：只返回两表中满足连接条件的匹配行。例如查询已有订单的用户信息。\n\nLEFT JOIN：返回左表所有行，右表不匹配的行用 NULL 填充。例如查询所有用户及其订单（包含没有订单的用户）。\n\nCROSS JOIN：返回两表的笛卡尔积，即左表每行与右表每行的所有组合，不需要连接条件。例如生成所有颜色和尺码的组合。\n\n示例：\n- INNER JOIN: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id\n- LEFT JOIN: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id\n- CROSS JOIN: SELECT * FROM colors CROSS JOIN sizes",
        "courseId": 1
    },
    {
        "type": "short_answer",
        "content": "请解释数据库事务的四种隔离级别，以及各级别分别解决了哪些并发问题。",
        "options": None,
        "answer": None,
        "referenceAnswer": "1. READ UNCOMMITTED（读未提交）：最低级别，可能发生脏读、不可重复读、幻读。事务可以读取其他事务未提交的数据。\n\n2. READ COMMITTED（读已提交）：解决脏读，但不能解决不可重复读和幻读。只能读取已提交的数据。\n\n3. REPEATABLE READ（可重复读）：解决脏读和不可重复读，但不能完全解决幻读。同一事务中多次读取结果一致。MySQL InnoDB 默认此级别。\n\n4. SERIALIZABLE（串行化）：最高级别，解决所有并发问题。事务完全串行执行，并发性能最低。\n\n隔离级别越高，数据一致性越好，但并发性能越差。需要在一致性和性能之间权衡。",
        "courseId": 1
    },
]

print()
print("五、简答题 (short_answer) — 5 道")
for q in shorts:
    create(q)

print()
print("=" * 50)
print("题库导入完成！共计 50 道题目：")
print("  单选题:     15 道")
print("  多选题:     10 道")
print("  判断题:     10 道")
print("  填空题:     10 道")
print("  简答题:      5 道")
print("=" * 50)
