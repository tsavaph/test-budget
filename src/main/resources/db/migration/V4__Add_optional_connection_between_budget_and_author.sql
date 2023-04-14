ALTER TABLE budget ADD author_id INT;

ALTER TABLE budget
ADD CONSTRAINT fk_budget_author
FOREIGN KEY (author_id) REFERENCES author (id) ON DELETE SET NULL;