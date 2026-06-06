-- Sửa UTF-8 care_logs demo (pipe PowerShell thường làm hỏng dấu tiếng Việt)
-- PowerShell: Get-Content database/seeds/boarding_care_logs_fix.sql -Encoding UTF8 -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

DO $$
DECLARE
    v_session_id UUID;
    v_feed_good  TEXT := convert_from(decode('C4826E2074E1BB9174', 'hex'), 'UTF8');
    v_hyg_normal TEXT := convert_from(decode('56E1BB872073696E682062C3AC6E68207468C6B0E1BB9D6E67', 'hex'), 'UTF8');
    v_note_morn  TEXT := convert_from(decode('42C3A9207468C3A26E20746869E1BB876E2C20C4836E2068E1BABF74206B68E1BAA975207068E1BAA76E206275E1BB95692073C3A16E672E', 'hex'), 'UTF8');
    v_feed_mid   TEXT := convert_from(decode('C4826E2076E1BBAB6120C491E1BBA7', 'hex'), 'UTF8');
    v_hyg_walk   TEXT := convert_from(decode('C490692064E1BAA16F203135207068C3BA74', 'hex'), 'UTF8');
    v_note_aft   TEXT := convert_from(decode('C490C3A3206368C6A1692076E1BB9B6920C491E1BB93206368C6A169207175656E20746875E1BB99632E', 'hex'), 'UTF8');
    v_feed_low   TEXT := convert_from(decode('C4826E2068C6A16920C3AD74', 'hex'), 'UTF8');
    v_hyg_watch  TEXT := convert_from(decode('5468656F2064C3B569207468C3AA6D', 'hex'), 'UTF8');
    v_health_ok  TEXT := convert_from(decode('4B68C3B46E672073E1BB9174', 'hex'), 'UTF8');
    v_note_watch TEXT := convert_from(decode('4E68C3A26E207669C3AA6E207468656F2064C3B569207468C3AA6D206B68E1BAA975207068E1BAA76E206275E1BB95692073C3A16E672E', 'hex'), 'UTF8');
BEGIN
    SELECT bs.id INTO v_session_id
    FROM boarding_sessions bs
    JOIN boarding_bookings bb ON bb.id = bs.booking_id
    WHERE bb.booking_code = 'BRD-DEMO-001'
    LIMIT 1;

    IF v_session_id IS NULL THEN
        RAISE NOTICE 'boarding_care_logs_fix: chua co BRD-DEMO-001';
        RETURN;
    END IF;

    UPDATE care_logs SET
        feeding_status = v_feed_good,
        hygiene_status = v_hyg_normal,
        health_note = NULL,
        staff_note = v_note_morn,
        updated_at = NOW()
    WHERE session_id = v_session_id
      AND log_date = CURRENT_DATE
      AND period_code = 'MORNING';

    UPDATE care_logs SET
        feeding_status = v_feed_mid,
        hygiene_status = v_hyg_walk,
        health_note = NULL,
        staff_note = v_note_aft,
        updated_at = NOW()
    WHERE session_id = v_session_id
      AND log_date = CURRENT_DATE
      AND period_code = 'AFTERNOON';

    UPDATE care_logs SET
        feeding_status = v_feed_low,
        hygiene_status = v_hyg_watch,
        health_note = v_health_ok,
        staff_note = v_note_watch,
        updated_at = NOW()
    WHERE session_id = v_session_id
      AND log_date = CURRENT_DATE - 1
      AND period_code = 'MORNING';

    DELETE FROM care_logs cl
    WHERE cl.session_id = v_session_id
      AND NOT (
          (cl.log_date = CURRENT_DATE AND cl.period_code IN ('MORNING', 'AFTERNOON'))
          OR (cl.log_date = CURRENT_DATE - 1 AND cl.period_code = 'MORNING')
      );
END $$;
