--
-- PostgreSQL database dump
--

\restrict CyceidJMKklu8Qr8RIyq85HtmN5iVgWhTa65VnaKgB2H9U9ULh5uUDOtVdf2MqV

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: user_role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_role AS ENUM (
    'USER',
    'WORKER',
    'ADMIN'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: billings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.billings (
    billing_id uuid DEFAULT gen_random_uuid() NOT NULL,
    booking_id uuid NOT NULL,
    base_price_per_hour numeric(10,2) NOT NULL,
    minimum_service_fee numeric(10,2) NOT NULL,
    labour_charge numeric(10,2) NOT NULL,
    travel_charge numeric(10,2) DEFAULT 0.00 NOT NULL,
    parts_cost numeric(10,2) DEFAULT 0.00 NOT NULL,
    additional_fees numeric(10,2) DEFAULT 0.00 NOT NULL,
    final_billed_price numeric(10,2) NOT NULL,
    worker_rating numeric(2,1) DEFAULT 5.0 NOT NULL,
    billable_hours numeric(6,2) DEFAULT 0.00 NOT NULL
);


--
-- Name: booking_offers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.booking_offers (
    offer_id uuid NOT NULL,
    booking_id uuid NOT NULL,
    worker_id uuid NOT NULL,
    offered_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: booking_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.booking_parts (
    booking_part_id uuid DEFAULT gen_random_uuid() NOT NULL,
    booking_id uuid NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    total_price numeric(10,2) NOT NULL,
    part_name character varying(150) NOT NULL,
    status character varying(20) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    approved_at timestamp without time zone,
    photo_url character varying(1000) NOT NULL
);


--
-- Name: booking_review_decisions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.booking_review_decisions (
    booking_id uuid NOT NULL,
    decision character varying(10) NOT NULL,
    decided_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT booking_review_decisions_decision_check CHECK (((decision)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'SKIPPED'::character varying])::text[])))
);


--
-- Name: bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bookings (
    booking_id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    worker_id uuid,
    service_id uuid NOT NULL,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    customer_note text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    accepted_at timestamp without time zone,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    failed_at timestamp without time zone,
    worker_cancelled_at timestamp without time zone,
    worker_cancellation_reason character varying(100),
    worker_cancellation_message character varying(1000),
    worker_completed_at timestamp without time zone,
    service_latitude numeric(10,7) NOT NULL,
    service_longitude numeric(10,7) NOT NULL,
    worker_acceptance_latitude numeric(10,7),
    worker_acceptance_longitude numeric(10,7),
    start_otp character varying(4),
    completion_otp character varying(4),
    completion_otp_requested_at timestamp without time zone,
    completion_requested boolean DEFAULT false NOT NULL,
    admin_cancellation_reason text
);


--
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    category_id uuid NOT NULL,
    category_name character varying(100) NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: credentials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credentials (
    user_id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    role public.user_role DEFAULT 'USER'::public.user_role NOT NULL
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    user_id uuid NOT NULL
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    payment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    booking_id uuid NOT NULL,
    razorpay_order_id character varying(100),
    razorpay_payment_id character varying(100),
    amount numeric(10,2) NOT NULL,
    status character varying(20) DEFAULT 'CREATED'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    paid_at timestamp without time zone,
    payment_method character varying(20),
    CONSTRAINT payments_amount_check CHECK ((amount >= (0)::numeric)),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying])::text[])))
);


--
-- Name: services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.services (
    service_id uuid NOT NULL,
    category_id uuid NOT NULL,
    service_name character varying(100) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    base_price_per_hour numeric(10,2) NOT NULL,
    minimum_service_fee numeric(10,2) NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    user_id uuid NOT NULL,
    full_name character varying(150) NOT NULL,
    age integer NOT NULL,
    gender character varying(20) NOT NULL,
    phone character varying(20) NOT NULL,
    address_line1 character varying(255) NOT NULL,
    address_line2 character varying(255),
    landmark character varying(150),
    city character varying(100) NOT NULL,
    state character varying(100) NOT NULL,
    pin_code character varying(10) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    terms_accepted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: worker_booking_rejections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_booking_rejections (
    rejection_id uuid DEFAULT gen_random_uuid() NOT NULL,
    worker_id uuid NOT NULL,
    booking_id uuid NOT NULL,
    rejected_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: worker_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_reviews (
    review_id uuid DEFAULT gen_random_uuid() NOT NULL,
    booking_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    worker_id uuid NOT NULL,
    rating integer NOT NULL,
    feedback_tags text[] DEFAULT '{}'::text[] NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT worker_reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


--
-- Name: worker_services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_services (
    worker_id uuid NOT NULL,
    service_id uuid NOT NULL
);


--
-- Name: worker_settlement_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_settlement_items (
    settlement_item_id uuid DEFAULT gen_random_uuid() NOT NULL,
    settlement_id uuid NOT NULL,
    payment_id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    CONSTRAINT worker_settlement_items_amount_check CHECK ((amount > (0)::numeric))
);


--
-- Name: worker_settlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_settlements (
    settlement_id uuid DEFAULT gen_random_uuid() NOT NULL,
    worker_id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    payment_reference character varying(100),
    bank_account_holder_name character varying(150) NOT NULL,
    bank_account_number character varying(30) NOT NULL,
    bank_ifsc_code character varying(11) NOT NULL,
    bank_name character varying(150) NOT NULL,
    settled_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    settled_at timestamp without time zone,
    failure_reason text,
    business_date date,
    CONSTRAINT worker_settlements_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT worker_settlements_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SETTLED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: worker_settlement_business_date_backfill_issues; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.worker_settlement_business_date_backfill_issues AS
 WITH item_business_dates AS (
         SELECT settlement_1.settlement_id,
            count(item.settlement_item_id) AS item_count,
            count(payment.paid_at) AS dated_item_count,
            count(DISTINCT
                CASE
                    WHEN (payment.paid_at IS NULL) THEN NULL::date
                    WHEN ((payment.paid_at)::time without time zone < '20:00:00'::time without time zone) THEN (payment.paid_at)::date
                    ELSE ((payment.paid_at)::date + 1)
                END) AS distinct_business_date_count
           FROM ((public.worker_settlements settlement_1
             LEFT JOIN public.worker_settlement_items item ON ((item.settlement_id = settlement_1.settlement_id)))
             LEFT JOIN public.payments payment ON ((payment.payment_id = item.payment_id)))
          GROUP BY settlement_1.settlement_id
        )
 SELECT settlement.settlement_id,
    item_dates.item_count,
    item_dates.dated_item_count,
    item_dates.distinct_business_date_count,
        CASE
            WHEN (item_dates.item_count = 0) THEN 'NO_PAYMENT_ITEMS'::text
            WHEN (item_dates.dated_item_count < item_dates.item_count) THEN 'MISSING_PAYMENT_PAID_AT'::text
            WHEN (item_dates.distinct_business_date_count > 1) THEN 'MULTIPLE_PAYMENT_BUSINESS_DATES'::text
            ELSE 'UNCLASSIFIED'::text
        END AS reason
   FROM (public.worker_settlements settlement
     JOIN item_business_dates item_dates ON ((item_dates.settlement_id = settlement.settlement_id)))
  WHERE (settlement.business_date IS NULL);


--
-- Name: worker_settlement_reversals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_settlement_reversals (
    reversal_id uuid NOT NULL,
    settlement_id uuid NOT NULL,
    bank_reversal_reference character varying(100) NOT NULL,
    reversal_reason text NOT NULL,
    verified_at timestamp without time zone NOT NULL,
    verified_by uuid NOT NULL,
    CONSTRAINT chk_worker_settlement_reversals_reason CHECK ((length(btrim(reversal_reason)) > 0)),
    CONSTRAINT chk_worker_settlement_reversals_reference CHECK ((length(btrim((bank_reversal_reference)::text)) > 0))
);


--
-- Name: workers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workers (
    user_id uuid NOT NULL,
    experience_years integer NOT NULL,
    qualification text NOT NULL,
    bio text,
    verification_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    availability_status character varying(20) DEFAULT 'UNAVAILABLE'::character varying NOT NULL,
    rating numeric(2,1) DEFAULT 5.0 NOT NULL,
    bank_account_holder_name character varying(150),
    bank_account_number character varying(30),
    bank_ifsc_code character varying(11),
    bank_name character varying(150),
    current_latitude numeric(10,7),
    current_longitude numeric(10,7),
    location_last_updated_at timestamp without time zone
);


--
-- Name: billings billings_booking_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billings
    ADD CONSTRAINT billings_booking_id_key UNIQUE (booking_id);


--
-- Name: billings billings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billings
    ADD CONSTRAINT billings_pkey PRIMARY KEY (billing_id);


--
-- Name: booking_offers booking_offers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_offers
    ADD CONSTRAINT booking_offers_pkey PRIMARY KEY (offer_id);


--
-- Name: booking_parts booking_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_parts
    ADD CONSTRAINT booking_parts_pkey PRIMARY KEY (booking_part_id);


--
-- Name: booking_review_decisions booking_review_decisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_review_decisions
    ADD CONSTRAINT booking_review_decisions_pkey PRIMARY KEY (booking_id);


--
-- Name: bookings bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT bookings_pkey PRIMARY KEY (booking_id);


--
-- Name: categories categories_category_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_category_name_key UNIQUE (category_name);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);


--
-- Name: credentials credentials_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credentials
    ADD CONSTRAINT credentials_email_key UNIQUE (email);


--
-- Name: credentials credentials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credentials
    ADD CONSTRAINT credentials_pkey PRIMARY KEY (user_id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (user_id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (payment_id);


--
-- Name: payments payments_razorpay_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_razorpay_order_id_key UNIQUE (razorpay_order_id);


--
-- Name: payments payments_razorpay_payment_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_razorpay_payment_id_key UNIQUE (razorpay_payment_id);


--
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (service_id);


--
-- Name: booking_offers uk_booking_offer_booking_worker; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_offers
    ADD CONSTRAINT uk_booking_offer_booking_worker UNIQUE (booking_id, worker_id);


--
-- Name: worker_booking_rejections uk_worker_booking_rejection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_booking_rejections
    ADD CONSTRAINT uk_worker_booking_rejection UNIQUE (worker_id, booking_id);


--
-- Name: worker_settlement_items uq_settlement_payment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_items
    ADD CONSTRAINT uq_settlement_payment UNIQUE (settlement_id, payment_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: worker_booking_rejections worker_booking_rejections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_booking_rejections
    ADD CONSTRAINT worker_booking_rejections_pkey PRIMARY KEY (rejection_id);


--
-- Name: worker_reviews worker_reviews_booking_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_reviews
    ADD CONSTRAINT worker_reviews_booking_id_key UNIQUE (booking_id);


--
-- Name: worker_reviews worker_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_reviews
    ADD CONSTRAINT worker_reviews_pkey PRIMARY KEY (review_id);


--
-- Name: worker_services worker_services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_services
    ADD CONSTRAINT worker_services_pkey PRIMARY KEY (worker_id, service_id);


--
-- Name: worker_settlement_items worker_settlement_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_items
    ADD CONSTRAINT worker_settlement_items_pkey PRIMARY KEY (settlement_item_id);


--
-- Name: worker_settlement_reversals worker_settlement_reversals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_reversals
    ADD CONSTRAINT worker_settlement_reversals_pkey PRIMARY KEY (reversal_id);


--
-- Name: worker_settlement_reversals worker_settlement_reversals_settlement_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_reversals
    ADD CONSTRAINT worker_settlement_reversals_settlement_id_key UNIQUE (settlement_id);


--
-- Name: worker_settlements worker_settlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlements
    ADD CONSTRAINT worker_settlements_pkey PRIMARY KEY (settlement_id);


--
-- Name: workers workers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workers
    ADD CONSTRAINT workers_pkey PRIMARY KEY (user_id);


--
-- Name: idx_booking_offers_worker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_booking_offers_worker ON public.booking_offers USING btree (worker_id, booking_id);


--
-- Name: idx_bookings_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_customer_id ON public.bookings USING btree (customer_id);


--
-- Name: idx_bookings_customer_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_customer_status ON public.bookings USING btree (customer_id, status);


--
-- Name: idx_bookings_pending_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_pending_created ON public.bookings USING btree (status, created_at) WHERE (worker_id IS NULL);


--
-- Name: idx_bookings_service_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_service_id ON public.bookings USING btree (service_id);


--
-- Name: idx_bookings_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_status ON public.bookings USING btree (status);


--
-- Name: idx_bookings_worker_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_worker_id ON public.bookings USING btree (worker_id);


--
-- Name: idx_bookings_worker_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bookings_worker_status ON public.bookings USING btree (worker_id, status);


--
-- Name: idx_settlement_items_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlement_items_payment ON public.worker_settlement_items USING btree (payment_id);


--
-- Name: idx_worker_booking_rejections_booking_worker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_booking_rejections_booking_worker ON public.worker_booking_rejections USING btree (booking_id, worker_id);


--
-- Name: idx_worker_reviews_worker_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_reviews_worker_id ON public.worker_reviews USING btree (worker_id);


--
-- Name: idx_worker_services_service_worker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_services_service_worker ON public.worker_services USING btree (service_id, worker_id);


--
-- Name: idx_worker_settlements_business_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_settlements_business_date ON public.worker_settlements USING btree (business_date);


--
-- Name: idx_worker_settlements_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_settlements_created_at ON public.worker_settlements USING btree (created_at);


--
-- Name: idx_worker_settlements_worker_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_settlements_worker_status ON public.worker_settlements USING btree (worker_id, status);


--
-- Name: idx_workers_offer_eligibility; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workers_offer_eligibility ON public.workers USING btree (verification_status, availability_status, location_last_updated_at) WHERE ((current_latitude IS NOT NULL) AND (current_longitude IS NOT NULL));


--
-- Name: booking_offers booking_offers_booking_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_offers
    ADD CONSTRAINT booking_offers_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: booking_offers booking_offers_worker_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_offers
    ADD CONSTRAINT booking_offers_worker_id_fkey FOREIGN KEY (worker_id) REFERENCES public.workers(user_id);


--
-- Name: billings fk_billings_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billings
    ADD CONSTRAINT fk_billings_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: booking_parts fk_booking_parts_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_parts
    ADD CONSTRAINT fk_booking_parts_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: booking_review_decisions fk_booking_review_decisions_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_review_decisions
    ADD CONSTRAINT fk_booking_review_decisions_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: bookings fk_bookings_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES public.customers(user_id);


--
-- Name: bookings fk_bookings_service; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_bookings_service FOREIGN KEY (service_id) REFERENCES public.services(service_id);


--
-- Name: bookings fk_bookings_worker; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_bookings_worker FOREIGN KEY (worker_id) REFERENCES public.workers(user_id);


--
-- Name: customers fk_customers_users; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fk_customers_users FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: services fk_services_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES public.categories(category_id);


--
-- Name: worker_settlements fk_settlement_admin; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlements
    ADD CONSTRAINT fk_settlement_admin FOREIGN KEY (settled_by) REFERENCES public.users(user_id);


--
-- Name: worker_settlement_items fk_settlement_item_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_items
    ADD CONSTRAINT fk_settlement_item_payment FOREIGN KEY (payment_id) REFERENCES public.payments(payment_id);


--
-- Name: worker_settlement_items fk_settlement_item_settlement; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_items
    ADD CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id) REFERENCES public.worker_settlements(settlement_id);


--
-- Name: worker_settlements fk_settlement_worker; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlements
    ADD CONSTRAINT fk_settlement_worker FOREIGN KEY (worker_id) REFERENCES public.workers(user_id);


--
-- Name: users fk_users_credentials; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_credentials FOREIGN KEY (user_id) REFERENCES public.credentials(user_id) ON DELETE CASCADE;


--
-- Name: worker_booking_rejections fk_worker_booking_rejections_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_booking_rejections
    ADD CONSTRAINT fk_worker_booking_rejections_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: worker_booking_rejections fk_worker_booking_rejections_worker; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_booking_rejections
    ADD CONSTRAINT fk_worker_booking_rejections_worker FOREIGN KEY (worker_id) REFERENCES public.workers(user_id);


--
-- Name: worker_reviews fk_worker_reviews_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_reviews
    ADD CONSTRAINT fk_worker_reviews_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- Name: worker_reviews fk_worker_reviews_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_reviews
    ADD CONSTRAINT fk_worker_reviews_customer FOREIGN KEY (customer_id) REFERENCES public.customers(user_id);


--
-- Name: worker_reviews fk_worker_reviews_worker; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_reviews
    ADD CONSTRAINT fk_worker_reviews_worker FOREIGN KEY (worker_id) REFERENCES public.workers(user_id);


--
-- Name: worker_services fk_worker_services_service; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_services
    ADD CONSTRAINT fk_worker_services_service FOREIGN KEY (service_id) REFERENCES public.services(service_id) ON DELETE CASCADE;


--
-- Name: worker_services fk_worker_services_worker; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_services
    ADD CONSTRAINT fk_worker_services_worker FOREIGN KEY (worker_id) REFERENCES public.workers(user_id) ON DELETE CASCADE;


--
-- Name: worker_settlement_reversals fk_worker_settlement_reversals_admin; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_reversals
    ADD CONSTRAINT fk_worker_settlement_reversals_admin FOREIGN KEY (verified_by) REFERENCES public.users(user_id) ON DELETE RESTRICT;


--
-- Name: worker_settlement_reversals fk_worker_settlement_reversals_settlement; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_settlement_reversals
    ADD CONSTRAINT fk_worker_settlement_reversals_settlement FOREIGN KEY (settlement_id) REFERENCES public.worker_settlements(settlement_id) ON DELETE RESTRICT;


--
-- Name: workers fk_workers_users; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workers
    ADD CONSTRAINT fk_workers_users FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: payments payments_booking_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- PostgreSQL database dump complete
--


