
// DML to add holiday scale based feature
COPY (
SELECT w.holiday, w.week, f.temperature, s.size,
CASE WHEN s.type='A' THEN 1 WHEN s.type='B' THEN 2 ELSE 3 END AS type,
CASE WHEN f.markdown1 IS NULL THEN '?' ELSE '' || f.markdown1 END AS markdown1,
CASE WHEN f.markdown2 IS NULL THEN '?' ELSE '' || f.markdown2 END AS markdown2,
CASE WHEN f.markdown3 IS NULL THEN '?' ELSE '' || f.markdown3 END AS markdown3,
CASE WHEN f.markdown4 IS NULL THEN '?' ELSE '' || f.markdown4 END AS markdown4,
CASE WHEN f.markdown5 IS NULL THEN '?' ELSE '' || f.markdown5 END AS markdown5,
t.weeklySales
FROM train t
JOIN features f ON (f.store = t.store AND f.date = t.date)
JOIN stores s ON (s.store = t.store)
JOIN weeks w ON (w.date = t.date)
WHERE t.dept = 2
) TO '/Users/Neha/Downloads/weka/data2.csv'
(FORMAT 'csv', HEADER TRUE)

SELECT * FROM features2 WHERE store=42

SELECT COUNT(*) FROM features

SELECT DISTINCT dept,store FROM test ORDER BY dept,store

SELECT w.holiday, w.week, f.temperature, s.size,
CASE WHEN s.type='A' THEN 1 WHEN s.type='B' THEN 2 ELSE 3 END AS type,
CASE WHEN f.markdown1 IS NULL THEN '?' ELSE '' || f.markdown1 END AS markdown1,
CASE WHEN f.markdown2 IS NULL THEN '?' ELSE '' || f.markdown2 END AS markdown2,
CASE WHEN f.markdown3 IS NULL THEN '?' ELSE '' || f.markdown3 END AS markdown3,
CASE WHEN f.markdown4 IS NULL THEN '?' ELSE '' || f.markdown4 END AS markdown4,
CASE WHEN f.markdown5 IS NULL THEN '?' ELSE '' || f.markdown5 END AS markdown5,
t.weeklySales
FROM train t
JOIN features f ON (f.store = t.store AND f.date = t.date)
JOIN stores s ON (s.store = t.store)
JOIN weeks w ON (w.date = t.date)
WHERE t.dept = 3