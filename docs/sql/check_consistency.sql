-- 数据一致性校验脚本（对应开发方案 10.1 / T-04）
-- 用法：mysql -uroot -p finance_db < check_consistency.sql
USE finance_db;

-- 1. 账户余额与「期初余额 + 流水重算值」是否一致
SELECT a.id,
       a.acc_name,
       a.init_balance,
       a.balance,
       a.init_balance + COALESCE(SUM(CASE r.type WHEN 'INCOME' THEN r.amount ELSE -r.amount END), 0) AS calc,
       a.balance - (a.init_balance + COALESCE(SUM(CASE r.type WHEN 'INCOME' THEN r.amount ELSE -r.amount END), 0)) AS diff
FROM account a
LEFT JOIN `record` r ON r.account_id = a.id AND r.user_id = a.user_id
GROUP BY a.id, a.acc_name, a.init_balance, a.balance
HAVING diff <> 0;

-- 2. 孤立流水（账户或分类不属于同一用户）
SELECT r.id, r.user_id, r.account_id, r.category_id, 'account_mismatch' AS issue
FROM `record` r
LEFT JOIN account a ON a.id = r.account_id AND a.user_id = r.user_id
WHERE a.id IS NULL
UNION ALL
SELECT r.id, r.user_id, r.account_id, r.category_id, 'category_mismatch'
FROM `record` r
LEFT JOIN category c ON c.id = r.category_id AND c.user_id = r.user_id
WHERE c.id IS NULL;

-- 3. 月度汇总与明细是否一致（示例月份按需替换）
SELECT DATE_FORMAT(record_date, '%Y-%m') AS month,
       SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income,
       SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense,
       SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) AS net
FROM `record`
GROUP BY DATE_FORMAT(record_date, '%Y-%m')
ORDER BY month DESC;
