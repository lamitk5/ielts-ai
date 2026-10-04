INSERT INTO mock_test_definition_sections (id, mock_test_id, section_order, skill, practice_set_id, time_limit_seconds)
VALUES ('00000000-0000-0000-0000-000000000075', '00000000-0000-0000-0000-000000000071', 3, 'SPEAKING', 'default-speaking-set', 1800)
ON CONFLICT (mock_test_id, section_order) DO NOTHING;
