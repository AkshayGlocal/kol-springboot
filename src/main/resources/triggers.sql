CREATE or REPLACE FUNCTION public.request_profile_notify_trigger() RETURNS trigger AS $$
DECLARE 
	BEGIN
		PERFORM pg_notify( CAST('update_notification' AS text), row_to_json(NEW)::text);
		RETURN new;
	END;
	$$ LANGUAGE plpgsql;
    //
CREATE TRIGGER request_profile_update_trigger AFTER UPDATE ON public.request_profile
FOR EACH ROW EXECUTE PROCEDURE public.request_profile_notify_trigger();
//
