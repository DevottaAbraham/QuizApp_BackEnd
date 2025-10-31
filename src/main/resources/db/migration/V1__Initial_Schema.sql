-- This is the initial schema setup for the Bible Quiz application.
-- Flyway will execute this script to create all necessary tables.

-- Table for storing user information
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    refresh_token TEXT,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE
);

-- Table for storing quiz questions
-- The table name is 'questions' (plural) to match the @Table(name="questions") annotation in the Question entity.
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL, -- Matches the 'text' field in the entity
    text_en TEXT NOT NULL,
    options_en TEXT, -- Storing options as a JSON string
    correct_answer VARCHAR(255) NOT NULL, -- Matches the 'correctAnswer' field
    correct_answer_en VARCHAR(255) NOT NULL,
    text_ta TEXT,
    options_ta TEXT, -- Storing options as a JSON string
    correct_answer_ta VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'draft', -- Matches the 'status' field
    release_date TIMESTAMP WITHOUT TIME ZONE,
    disappear_date TIMESTAMP WITHOUT TIME ZONE,
    author_id BIGINT REFERENCES users(id), -- Matches the 'author' field
    last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

-- Table for storing user scores and quiz results
CREATE TABLE scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    score INTEGER NOT NULL, -- This should match the 'scoreValue' field in the Score entity, which is mapped to the 'score' column.
    total INTEGER NOT NULL,
    quiz_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    answered_questions_json TEXT -- Storing detailed answers as a JSON string
);

-- Table for storing the home page content
CREATE TABLE home_page_content (
    id BIGSERIAL PRIMARY KEY,
    content TEXT
);

-- Table for blacklisting JWT tokens upon logout
CREATE TABLE token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL
);