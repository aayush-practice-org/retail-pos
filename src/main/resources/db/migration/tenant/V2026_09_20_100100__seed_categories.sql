-- ============================================================================
-- TENANT SCHEMA — A CATALOGUE THAT EXISTS BEFORE ANYONE TOUCHES IT
--
-- A freshly provisioned mart had no categories, and a product cannot be created
-- without one. So the first thing every new shop had to do was invent a
-- taxonomy — before it could enter a single product, and before it had any idea
-- what it was going to stock. That is the wrong order, and it is a question
-- with a boring right answer, so the answer ships.
--
-- These are a starting point, not a fixture: they are ordinary rows, editable
-- and deletable like any other. Nothing in the code depends on a particular one
-- existing except the default, and that is handled below.
-- ============================================================================

-- The category a product falls into when nobody says otherwise. Quick-add at
-- the till leans on this: a cashier ringing up an unknown barcode mid-queue is
-- not going to pick an aisle, and refusing to create the product is worse than
-- filing it under General for someone to sort out later.
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS default_category BOOLEAN NOT NULL DEFAULT FALSE;

-- At most one, or "the default" is not a question with an answer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_default ON categories (default_category)
    WHERE default_category AND deleted_at IS NULL;

-- Only inserts what is missing, so a mart that already named its own aisles
-- keeps them and an existing tenant picks up just the gaps.
INSERT INTO categories (name, description, default_category)
SELECT v.name, v.description, v.is_default
FROM (VALUES ('General', 'Anything not yet filed under an aisle', TRUE),
             ('Grocery & Staples', 'Rice, flour, pulses, oil, sugar, salt and spices', FALSE),
             ('Beverages', 'Water, soft drinks, juice, tea and coffee', FALSE),
             ('Dairy & Eggs', 'Milk, curd, butter, ghee, cheese and eggs', FALSE),
             ('Snacks & Confectionery', 'Noodles, biscuits, crisps, chocolate and sweets', FALSE),
             ('Bakery', 'Bread, buns, cakes and pastries', FALSE),
             ('Fruits & Vegetables', 'Fresh produce, sold loose or by the pack', FALSE),
             ('Meat & Fish', 'Fresh, frozen and processed', FALSE),
             ('Personal Care', 'Soap, shampoo, oral care and cosmetics', FALSE),
             ('Household & Cleaning', 'Detergent, cleaners, paper goods and utensils', FALSE),
             ('Baby Care', 'Formula, nappies and baby food', FALSE),
             ('Stationery', 'Pens, paper, notebooks and school supplies', FALSE))
         AS v(name, description, is_default)
WHERE NOT EXISTS (SELECT 1
                  FROM categories c
                  WHERE LOWER(c.name) = LOWER(v.name)
                    AND c.deleted_at IS NULL);

-- A tenant that already had a category called "General" skipped the insert
-- above and so has no default at all. Give the oldest category the job rather
-- than leaving quick-add with nowhere to file anything.
UPDATE categories
SET default_category = TRUE
WHERE id = (SELECT id
            FROM categories
            WHERE deleted_at IS NULL
            ORDER BY id
            LIMIT 1)
  AND NOT EXISTS (SELECT 1
                  FROM categories
                  WHERE default_category
                    AND deleted_at IS NULL);
