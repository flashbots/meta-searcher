-- Local variables
-- Lua script stays in memory for the lifetime of the Fluent Bit process,
-- so all local variables persist across filter calls.

local DELAY_SEC = 120 -- Delay (in seconds) before flushing logs
local buckets = {} -- Table to bucket logs by their timestamp (seconds)
local current_buffered_records = 0 -- Number of currently buffered logs, kept track of manually
local MAX_BUFFERED_LOG_RECORDS = 65536 -- Max amount of log records to keep in memory before truncating
local truncated_buckets = {} -- Tracks which timestamps were truncated (and emits that information only when the time comes)
local earliest_sec = nil -- Tracks the earliest second we have in our buckets table
local last_processed_second = nil -- Tracks which second we last ran the flush logic on

-- Lua Filter function
function log_delay(tag, ts_table, record)
    -- Current time in integer seconds
    local now_sec = os.time()
    local now_floor = now_sec  
    local arrival_sec = ts_table.sec or 0
    if earliest_sec == nil or arrival_sec < earliest_sec then
        earliest_sec = arrival_sec
    end

	-- Check if we are not exceeding the limit of records
	if current_buffered_records < MAX_BUFFERED_LOG_RECORDS then
		-- 1) Insert the new record into its bucket
		if not buckets[arrival_sec] then
			buckets[arrival_sec] = {}
		end
		table.insert(buckets[arrival_sec], record)
		current_buffered_records = current_buffered_records + 1
	else
		truncated_buckets[arrival_sec] = true
	end

    -- 2) Check if we've already processed this second
    if last_processed_second == now_floor then
        -- Skip the flush; Return no output
        return 2, ts_table, {}
    end

    -- 3) Otherwise, do the flush logic once for this second
    last_processed_second = now_floor
    local to_emit = {}

    -- Flush all buckets whose second <= (now_sec - DELAY_SEC)
	local buckets_were_truncated = false
    while earliest_sec and earliest_sec <= (now_sec - DELAY_SEC) do
		buckets_were_truncated = buckets_were_truncated || truncated_buckets[earliest_sec] 
		table.remove(truncated_buckets, earliest_sec) 

        local bucket_logs = buckets[earliest_sec]
        if bucket_logs then
            -- Move all logs in this bucket to 'to_emit'
            for _, old_record in ipairs(bucket_logs) do
                table.insert(to_emit, old_record)
				current_buffered_records = current_buffered_records - 1
            end
			-- TODO: should probably delete right?
            buckets[earliest_sec] = nil
            table.remove(buckets, earliest_sec)
        end

        -- Move on to the next second
        earliest_sec = earliest_sec + 1
    end

	-- 3.5) Append truncation notice if any
	if buckets_were_truncated then
		table.insert(to_emit, {"Some entries were truncated!"})
	end

    -- 4) Return any flushed logs
    if #to_emit == 0 then
        return 2, ts_table, {}
    else
        local new_ts = { sec = now_sec, nsec = 0 }
        return 1, new_ts, to_emit
    end
end
