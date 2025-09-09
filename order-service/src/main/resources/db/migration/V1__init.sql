-- V1__init.sql
-- Purpose: Initial relational schema for Mini-Commerce Order Service.
-- Conventions:
--   - Explicit, named constraints (pk_/fk_/ck_/uq_)
--   - VARCHAR over CHAR; length enforced with CHECKs where relevant
--   - Enum values stored as UPPERCASE strings to match JPA @Enumerated(EnumType.STRING)
--   - Timestamps default to now(); updated_at maintained by trigger

-- ────────────────────────────────────────────────────────────────
-- Schema
-- ────────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS public;

-- ────────────────────────────────────────────────────────────────
-- Customers
-- ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.customers (
  id         UUID NOT NULL
    CONSTRAINT pk_customers_id PRIMARY KEY,
  email      TEXT NOT NULL
    CONSTRAINT uq_customers_email UNIQUE,
  name       TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE  public.customers        IS 'Registered customers (minimal fields for M1)';
COMMENT ON COLUMN public.customers.email  IS 'Unique identifier (demo-grade)';
COMMENT ON COLUMN public.customers.name   IS 'Display name';

-- ────────────────────────────────────────────────────────────────
-- Orders (header)
-- ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.orders (
  id           UUID NOT NULL
    CONSTRAINT pk_orders_id PRIMARY KEY,
  customer_id  UUID NOT NULL
    CONSTRAINT fk_orders_customer_id
    REFERENCES public.customers(id),
  status       VARCHAR(20) NOT NULL
    CONSTRAINT ck_orders_status
    CHECK (status IN ('CREATED','PAID','FULFILLED','CANCELLED','REFUNDED')),
  currency     VARCHAR(3) NOT NULL
    CONSTRAINT ck_orders_currency_len
    CHECK (char_length(currency) = 3),
  total        NUMERIC(12,2) NOT NULL
    CONSTRAINT ck_orders_total_nonneg CHECK (total >= 0),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE  public.orders          IS 'Order header; authoritative for order lifecycle';
COMMENT ON COLUMN public.orders.status   IS 'Enum stored as UPPERCASE to match JPA EnumType.STRING';
COMMENT ON COLUMN public.orders.currency IS 'ISO 4217 alpha-3 currency code';

-- Helpful indexes
CREATE INDEX IF NOT EXISTS ix_orders_customer_id ON public.orders(customer_id);
CREATE INDEX IF NOT EXISTS ix_orders_status      ON public.orders(status);
CREATE INDEX IF NOT EXISTS ix_orders_created_at  ON public.orders(created_at);

-- ────────────────────────────────────────────────────────────────
-- Order Items (lines)
-- ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.order_items (
  id         UUID NOT NULL
    CONSTRAINT pk_order_items_id PRIMARY KEY,
  order_id   UUID NOT NULL
    CONSTRAINT fk_order_items_order_id
    REFERENCES public.orders(id) ON DELETE CASCADE,
  sku        TEXT NOT NULL,
  name       TEXT NOT NULL,
  quantity   INTEGER NOT NULL
    CONSTRAINT ck_order_items_quantity_pos CHECK (quantity > 0),
  unit_price NUMERIC(12,2) NOT NULL
    CONSTRAINT ck_order_items_price_non_negative CHECK (unit_price >= 0)
);

COMMENT ON TABLE public.order_items IS 'Items belonging to orders (line-level)';
CREATE INDEX IF NOT EXISTS ix_order_items_order_id ON public.order_items(order_id);

-- ────────────────────────────────────────────────────────────────
-- updated_at trigger (explicit names)
-- ────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.fn_orders_set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_orders_set_updated_at ON public.orders;
CREATE TRIGGER trg_orders_set_updated_at
    BEFORE UPDATE ON public.orders
    FOR EACH ROW
EXECUTE FUNCTION public.fn_orders_set_updated_at();
